package pack_and_encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PackFile extends BaseClass {

	// 文件路径列表
	static ArrayList<String> filePathList = new ArrayList<String>();

	// 文件列表
	static ArrayList<File> fileList = new ArrayList<File>();

	// 文件过滤
	static String[] filter;

	// 找不到文件列�?
	ArrayList<String> notFoundList = new ArrayList<String>();

	// 除notFoundList外，剩下的正确的文件列表
	ArrayList<String> correctList = new ArrayList<String>();

	// 打包文件路径
	protected static String packedFile;

	// 解压文件时重复文件map
	Map<String, Integer> repeatFileMap = new HashMap<String, Integer>();

	// 打包文件文件头部存储list
	Header[] header;

	// header的有效数�?
	int headerNum;

	// header元素个数与文件个数的比例
	final double multiple = 1.7;

	// 打包文件头结束标志字�?
	final String headerEnd = "$$$$";

	// �?大的offset文件头位�?
	int maxOffsetHeaderIndex;

	// 临时header文件路径
	String headerTemp = System.getProperty("java.io.tmpdir") + "headerTemp";

	// 临时打包各文件路�?(除去header部分)
	String filePartTemp = System.getProperty("java.io.tmpdir") + "filePartTemp";

	// header部分的�?�大�?
	long headerSize;

	// 文件部分的�?�大�?
	long filePartSize;

	// 错误信息代码
	protected static int errorCode;

	// 静�?�内部类单例模式
	private static class InstanceHolder {
		private static final PackFile INSTANCE = new PackFile();
	}

	protected PackFile() {
	}

	// 获取实例，判断输入是否正确，输入为（aFilePathList：文�?/文件夹列表，aPackedFile：打包文件路�?,aFileList:文件句柄列表,aFilter:文件过滤�?
	static public final PackFile getInstance(ArrayList<String> aFilePathList, String aPackedFile, String[] aFilter) {
		errorCode = 0;

		if (aFilePathList == null || aFilePathList.size() == 0) {
			logger.info("file path list to be pack:no element");
			errorCode = ErrorCode.NULL_INPUT;
		}

		if (aPackedFile == null) {
			logger.info("pack file path:input null");
			errorCode = ErrorCode.NULL_INPUT;
		}

		filePathList = aFilePathList;
		packedFile = aPackedFile;
		filter = aFilter;
		return InstanceHolder.INSTANCE;
	}

	static public final PackFile getInstance(ArrayList<File> aFileList, String aPackedFile) {
		errorCode = 0;

		if (aFileList == null || aFileList.size() == 0) {
			logger.info("file list to be pack:no element");
			errorCode = ErrorCode.NULL_INPUT;
		}

		if (aPackedFile == null) {
			logger.info("pack file path:input null");
			errorCode = ErrorCode.NULL_INPUT;
		}

		fileList = aFileList;
		packedFile = aPackedFile;
		return InstanceHolder.INSTANCE;
	}

	// checkError
	protected int checkError() {
		if (errorCode < 0)
			error(errorCode);
		return errorCode;
	}

	// 将一个文件打包到打包文件�?
	public int packOneFile(String filePath) throws SecurityException, IOException {
		if (checkError() < 0)
			return errorCode;
		logger.info("pack one file - start");
		int flg = getFileContent();
		if (flg < 0)
			return flg;
		if (!Common.isPath(filePath)) {
			return ErrorCode.PATTERN_NOT_MATCH;
		}
		File f = new File(filePath);
		if (!f.exists())
			return ErrorCode.FILE_NOT_FOUND;

		long len = f.length();
		long offset;
		if (header == null) {
			offset = 0;
		} else
			offset = header[maxOffsetHeaderIndex].offset + header[maxOffsetHeaderIndex].length;
		String fileName = f.getName();

		Header aHeader = new Header(fileName, offset, len);

		int lenTemp = 1;
		Header[] headerTemp;
		if (header != null) {
			lenTemp = header.length + 1;
			headerTemp = new Header[lenTemp];
			for (int i = 1; i < lenTemp - 1; i++) {
				headerTemp[i] = header[i];
			}
			headerTemp[lenTemp - 1] = aHeader;
		} else {
			headerTemp = new Header[lenTemp];
			headerTemp[0] = aHeader;
		}

		header = new Header[(int) ((headerNum + 1) * multiple)];
		int index = 0, next = 0;
		for (int i = 0; i < lenTemp; i++) {
			if (headerTemp[i] == null)
				continue;
			index = Common.BKDRHash(String.valueOf(headerTemp[i].fileName).trim()) % header.length;
			next = 0;
			if (header[index] == null)
				header[index] = headerTemp[i];
			else {
				// 发生碰撞时线性探测插�?
				int placedFlg = 0;
				for (next = 1; next + index < header.length && placedFlg == 0; next++) {
					if (header[index + next] == null) {
						header[index + next] = headerTemp[i];
						placedFlg = 1;
					}
				}
				if (placedFlg == 0) {
					for (next = -1; next + index >= 0 && placedFlg == 0; next--) {
						if (header[index + next] == null) {
							header[index + next] = headerTemp[i];
							placedFlg = 1;
						}
					}
				}
				if (placedFlg == 0)
					errorCode = ErrorCode.INSERT_FAIL;
			}
		}
		headerNum++;

		try {
			// 在临时文件里写入内容
			String packFilePath = packedFile + "_temp";
			FileOutputStream fos = new FileOutputStream(packFilePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(header);
			oos.flush();
			byte[] end = { '$', '$', '$', '$' };
			byte[] lastHeader = Common.intToFourByte(index + next);
			fos.write(end);
			fos.write(lastHeader);
			oos.close();
			fos.close();
			if (flg == 0) {
				Common.writeToAnotherFile(packFilePath, filePartTemp);
			}
			if ((errorCode = Common.writeToAnotherFile(packFilePath, filePath)) < 0)
				return errorCode;

			if (Common.deleteFile(packedFile) == 0 || !Common.isFileExist(packedFile))
				Common.writeToAnotherFile(packedFile, packFilePath);
			else
				errorCode = ErrorCode.DELETE_FAIL;
			Common.deleteFile(packFilePath);

		} catch (FileNotFoundException e) {
			errorCode = ErrorCode.FILE_NOT_FOUND;
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			errorCode = ErrorCode.IO_ERROR;
		}
		deleteTemp();
		logger.info("pack one file - end");
		error(errorCode);
		return errorCode;
	}

	// 将fileList里的文件打包到打包文件里
	public int packFileList() {
		if (checkError() < 0)
			return errorCode;
		logger.info("pack file list - start");
		int flg = getFileContent();
		if (flg < 0) {
			errorCode = flg;
			error(errorCode);
			return errorCode;
		}

		if (!Common.isPath(packedFile)) {
			errorCode = ErrorCode.PATH_INPUT_ERROR;
			logger.info("pack file path:input error");
			return errorCode;
		}

		for (int i = 0; i < fileList.size(); i++) {
			File f = fileList.get(i);
			if (!f.exists()) {
				notFoundList.add(fileList.get(i).getPath());
			} else {
				correctList.add(fileList.get(i).getPath());
			}
		}
		if (correctList == null || correctList.size() == 0) {
			errorCode = ErrorCode.FILE_LIST_ZERO;
			error(errorCode);
			return errorCode;
		}
		fileList.clear();

		// 传入的fileList计算其Header
		Header[] correctHeader = new Header[correctList.size()];

		for (int i = 0; i < correctList.size(); i++) {
			File f = new File(correctList.get(i));
			if (i == 0) {
				long len = f.length();
				long offset;
				if (header == null) {
					offset = 0;
				} else
					offset = header[maxOffsetHeaderIndex].offset + header[maxOffsetHeaderIndex].length;
				String fileName = f.getName();
				correctHeader[i] = new Header(fileName, offset, len);
			} else {
				correctHeader[i] = new Header(f.getName(), correctHeader[i - 1].offset + correctHeader[i - 1].length,
						f.length());
			}
		}

		// aHeader与原有header[]合并
		Header[] headerTemp;
		int lenTemp = correctHeader.length;
		if (header != null) {
			lenTemp += header.length;
			headerTemp = new Header[lenTemp];
			for (int i = 0; i < header.length; i++) {
				headerTemp[i] = header[i];
			}
			for (int i = 0, j = header.length; i < correctHeader.length; i++, j++) {
				headerTemp[j] = correctHeader[i];
			}
		} else {
			headerTemp = correctHeader;
		}

		logger.info("make the hash of headers - start");
		header = new Header[(int) ((headerNum + correctHeader.length) * multiple)];
		int index = 0, next = 0;
		int maxOffsetPos = 0;
		for (int i = 0; i < lenTemp; i++) {
			if (headerTemp[i] == null)
				continue;
			index = Common.BKDRHash(String.valueOf(headerTemp[i].fileName).trim()) % header.length;
			next = 0;
			if (header[index] == null) {
				header[index] = headerTemp[i];
				maxOffsetPos = index;
			} else {
				// 发生碰撞时线性探测插�?
				int placedFlg = 0;
				for (next = 1; next + index < header.length && placedFlg == 0; next++) {
					if (header[index + next] == null) {
						header[index + next] = headerTemp[i];
						placedFlg = 1;
						maxOffsetPos = index + next;
					}
				}
				if (placedFlg == 0) {
					for (next = -1; next + index >= 0 && placedFlg == 0; next--) {
						if (header[index + next] == null) {
							header[index + next] = headerTemp[i];
							placedFlg = 1;
							maxOffsetPos = index + next;
						}
					}
				}
				if (placedFlg == 0)
					errorCode = ErrorCode.INSERT_FAIL;
			}
		}
		headerNum += correctHeader.length;
		logger.info("make the hash of headers - end");

		try {
			// 在临时文件里写入内容
			logger.info("write to packed file - start");
			String packFilePath = packedFile + "_temp";
			FileOutputStream fos = new FileOutputStream(packFilePath);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(header);
			oos.flush();
			byte[] end = { '$', '$', '$', '$' };
			byte[] lastHeader = Common.intToFourByte(maxOffsetPos);
			fos.write(end);
			fos.write(lastHeader);
			fos.close();
			oos.close();
			if (flg == 0) {
				Common.writeToAnotherFile(packFilePath, filePartTemp);
			}

			FileInputStream fis = null;
			FileOutputStream fos1 = new FileOutputStream(packFilePath, true);
			for (int i = 0; i < correctList.size(); i++) {
				File f = new File(correctList.get(i));
				fis = new FileInputStream(correctList.get(i));
				byte[] b = new byte[(int) f.length()];
				fis.read(b);
				fos1.write(b);
			}
			fis.close();
			fos1.flush();
			fos1.close();
			logger.info("write to packed file - end");

			if (Common.deleteFile(packedFile) == 0 || !Common.isFileExist(packedFile))
				Common.writeToAnotherFile(packedFile, packFilePath);
			else
				errorCode = ErrorCode.DELETE_FAIL;
			Common.deleteFile(packFilePath);

		} catch (FileNotFoundException e) {
			errorCode = ErrorCode.FILE_NOT_FOUND;
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			errorCode = ErrorCode.IO_ERROR;
		}
		notFoundList.clear();
		correctList.clear();
		deleteTemp();
		logger.info("pack file list - end");
		error(errorCode);
		return errorCode;
	}

	// 将filePathList里的文件打包到打包文件里
	public int packFilePathList() {
		if (checkError() < 0)
			return errorCode;
		logger.info("pack file list by path - start");

		for (int i = 0; i < filePathList.size(); i++) {
			File f = new File(filePathList.get(i));
			if (!f.exists()) {
				notFoundList.add(filePathList.get(i));
			} else {
				if (f.isDirectory()) {
					ArrayList<File> ft = Common.findFiles(f.getPath(), filter);
					fileList.addAll(ft);
				} else
					fileList.add(new File(filePathList.get(i)));
			}
		}

		if (fileList.size() == 0) {
			errorCode = ErrorCode.FILE_LIST_ZERO;
			logger.info("file list to be pack:no element");
			error(errorCode);
			return errorCode;
		}

		errorCode = packFileList();
		logger.info("pack file list by path - end");
		error(errorCode);
		return errorCode;
	}

	// 获取打包文件的header部分，内存及硬盘
	protected int getHeader() throws IOException {
		logger.info("get the file header - start");
		Common.deleteFile(headerTemp);
		try {
//            FileOutputStream fos = new FileOutputStream(headerTemp);
			if (!Common.isFileExist(packedFile)) {
				filePartSize = 0;
				headerNum = 0;
				return 1;
			}
//            FileInputStream fis = new FileInputStream(packedFile);
//            byte[] bs1 = new byte[primarySize];
//            if((fis.read(bs1)) != -1 && !(bs1[0] == '$' && bs1[1] == '$' && bs1[2] == '$' && bs1[3] == '$')){
//                endPos += primarySize;
//                fos.write(bs1);
//            }else
//                return endPos;
//            byte[] bs2 = new byte[eachCommonSize];
//            while((fis.read(bs2)) != -1){
//
//                if(bs2[0] == '$' && bs2[1] == '$' && bs2[2] == '$' && bs2[3] == '$'){
//                    break;
//                }
//                endPos += eachCommonSize;
//                fos.write(bs2);
//            }
//            fos.close();
//            fis.close();

//            File f = new File(headerTemp);
//            FileInputStream fis1 = new FileInputStream(f);
			File f = new File(packedFile);
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Header[] aHeaderList = (Header[]) ois.readObject();
			fis.close();
			ois.close();
			header = aHeaderList;
			filePartSize = 0;
			for (int i = 0; i < header.length; i++) {
				if (header[i] == null)
					continue;
				filePartSize += header[i].length;
				headerNum++;
			}
			headerSize = f.length() - filePartSize - 8;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			errorCode = ErrorCode.FILE_NOT_FOUND;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			errorCode = ErrorCode.HEADER_READ_ERROR;
		}
		logger.info("get the file header - end");
		return errorCode;
	}

	// 获取打包文件的文件部分，写入硬盘
	protected int getFileContent() {
		logger.info("get the file content - start");
		try {
			if (!Common.isFileExist(packedFile)) {
				filePartSize = 0;
				return getHeader();
			}
			Common.deleteFile(filePartTemp);

			if ((errorCode = getHeader()) < 0)
				return errorCode;
			FileInputStream fis = new FileInputStream(packedFile);
			File f = new File(packedFile);
			fis.skip(headerSize + 4);
			byte[] lastHeader = new byte[4];
			fis.read(lastHeader);
			maxOffsetHeaderIndex = Common.fourByteToInt(lastHeader);
			filePartSize = f.length() - headerSize - 8;

			FileOutputStream fos = new FileOutputStream(filePartTemp);
			byte[] b = new byte[Common.eachTimeByte];
			int i = 1;
			for (; i * Common.eachTimeByte < filePartSize; i++) {
				fis.read(b);
				fos.write(b);
				fos.flush();
			}
			b = new byte[(int) (filePartSize - Common.eachTimeByte * (i - 1))];
			fis.read(b);
			fos.write(b);
			fos.flush();
			fos.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
			errorCode = ErrorCode.IO_ERROR;
		}
		logger.info("get the file content - end");
		return errorCode;
	}

	// 查看packed文件里的文件列表
	public int lookAllPackedFileName() {
		int err = 0;
		try {
			if ((err = getHeader()) < 0)
				return err;
			for (int i = 0; i < header.length; i++) {
				if (header[i] == null)
					continue;
				System.out.println(header[i].fileName + "\t\t\t" + header[i].length / 1024 + "KB");
			}
			System.out.println("The files number: " + headerNum);
		} catch (IOException e) {
			e.printStackTrace();
			err = ErrorCode.IO_ERROR;
		}
		return err;
	}

	// 删除临时文件
	private int deleteTemp() {
		int flg;
		if ((flg = Common.deleteFile(headerTemp)) != 0) {
			return flg;
		}
		if ((flg = Common.deleteFile(filePartTemp)) != 0) {
			return flg;
		}
		return 0;
	}

	// 将重复文件命名为原名+序号的形�?
	protected String renameFile(String fullName, Integer sequence) {
		fullName = fullName.trim();
		String name = null;
		String extension = null;
		int index = fullName.length() - 1;
		for (; index >= 0; index--) {
			if (fullName.charAt(index) == '.') {
				name = fullName.substring(0, index);
				extension = fullName.substring(index);
			}
		}
		if (extension == null)
			return (fullName + "_(" + sequence + ")");
		else
			return name + "_(" + sequence + ")" + extension;
	}
}
