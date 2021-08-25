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

/**
 * packFile
 * 
 * @author son
 */
public class PackFile extends BaseClass {

	// filePath list
	static ArrayList<String> filePathList = new ArrayList<String>();

	// file list
	static ArrayList<File> fileList = new ArrayList<File>();

	// file filter
	static String[] filter;

	// not found fileList
	protected ArrayList<String> notFoundList = new ArrayList<String>();

	// correct fileList
	protected ArrayList<String> correctList = new ArrayList<String>();

	// pacded file path
	protected static String packedFile;

	// repeated file map
	protected Map<String, Integer> repeatFileMap = new HashMap<String, Integer>();

	// header list
	protected Header[] header;

	// number of header
	protected int headerNum;

	// factor of headers and files
	protected final double factor = 1.7;

	// starter
	protected final String headerEnd = "$$$$";

	// max offset of haeder
	protected int maxOffsetHeaderIndex;

	// headerTemp
	protected String headerTemp = System.getProperty("java.io.tmpdir") + "headerTemp";

	// filePartTemp
	protected String filePartTemp = System.getProperty("java.io.tmpdir") + "filePartTemp";

	// headerSize
	protected long headerSize;

	// filePartSize
	protected long filePartSize;

	// errorCode
	protected static int errorCode;

	/**
	 * static class for singleton
	 * 
	 * @author son
	 *
	 */
	private static class InstanceHolder {
		private static final PackFile INSTANCE = new PackFile();
	}

	protected PackFile() {
	}

	/**
	 * @param aFilePathList
	 * @param aPackedFile
	 * @param aFilter
	 * @return
	 */
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

	/**
	 * @param aFileList
	 * @param aPackedFile
	 * @return
	 */
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

	/**
	 * checkError
	 * 
	 * @return
	 */
	protected int checkError() {
		if (errorCode < 0)
			error(errorCode);
		return errorCode;
	}

	/**
	 * pack a file
	 * 
	 * @param filePath
	 * @return
	 * @throws SecurityException
	 * @throws IOException
	 */
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

		header = new Header[(int) ((headerNum + 1) * factor)];
		int index = 0, next = 0;
		for (int i = 0; i < lenTemp; i++) {
			if (headerTemp[i] == null)
				continue;
			index = Common.BKDRHash(String.valueOf(headerTemp[i].fileName).trim()) % header.length;
			next = 0;
			if (header[index] == null)
				header[index] = headerTemp[i];
			else {
				// if position cover happens, find next potition
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

	/**
	 * pack files of fileList
	 * 
	 * @return
	 */
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

		// header merge
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
		header = new Header[(int) ((headerNum + correctHeader.length) * factor)];
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
			// write contents to tempFile
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

	/**
	 * pack fileList by path
	 * 
	 * @return
	 */
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

	/**
	 * get the header
	 * 
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * get the file content
	 * 
	 * @return
	 */
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

	/**
	 * see the file list
	 * 
	 * @return
	 */
	public int seeAllPackedFileName() {
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

	/**
	 * delete temp files
	 * 
	 * @return
	 */
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

	/**
	 * rename file
	 * 
	 * @param fullName
	 * @param sequence
	 * @return
	 */
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
