package pack_and_encrypt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ExtractFile extends PackFile {

	// 解包到的文件�?
	static String packToDir;

	// 静�?�内部类单例模式
	private static class InstanceHolder {
		private static final ExtractFile INSTANCE = new ExtractFile();
	}

	protected ExtractFile() {
	}

	// 获取实例
	// aPackedFilePath为打包文件路径，aPackToDir为解包到的文件夹
	static public final ExtractFile getInstance(String aPackedFilePath, String aPackToDir) {
		errorCode = 0;
		if (aPackToDir == null) {
			logger.info("pack to directory:input null");
			errorCode = ErrorCode.NULL_INPUT;
		}
		if (aPackedFilePath == null) {
			logger.info("packed file:input null");
			errorCode = ErrorCode.NULL_INPUT;
		}
		packedFile = aPackedFilePath;
		packToDir = aPackToDir;
		return InstanceHolder.INSTANCE;
	}

	// 解压�?有文件到指定文件�?
	@SuppressWarnings("resource")
	public int extractAllFile() {
		if (checkError() < 0)
			return errorCode;
		logger.info("extract all file - start");
		int flg = getFileContent();

		if (flg < 0) {
			errorCode = flg;
			error(errorCode);
			return errorCode;
		}

		if (!Common.isPath(packedFile)) {
			errorCode = ErrorCode.PATTERN_NOT_MATCH;
			logger.info("pack file path:input error");
		}

		if (!Common.isPath(packToDir)) {
			errorCode = ErrorCode.PATTERN_NOT_MATCH;
			logger.info("pack to directory:input error");
		}

		File f = new File(packToDir);
		if (!f.isDirectory()) {
			f.mkdirs();
		}

		try {
			RandomAccessFile raf = new RandomAccessFile(filePartTemp, "r");
			String aFileName;
			String aFilePath;
			for (int i = 0; i < header.length; i++) {
				if (header[i] == null)
					continue;
				byte[] b = new byte[(int) header[i].length];
				raf.seek(header[i].offset);
				raf.read(b);

				aFileName = String.valueOf(header[i].fileName).trim();
				aFilePath = f.getPath() + "\\" + aFileName;

				if (Common.isFileExist(aFilePath)) {
					if (repeatFileMap.get(aFileName) == null) {
						repeatFileMap.put(aFileName, 1);
					} else {
						Integer val = repeatFileMap.get(aFileName);
						val++;
						repeatFileMap.put(aFileName, val);
					}
					// aFilePath = aFilePath + "_" + repeatFileMap.get(aFileName);
					aFilePath = f.getPath() + "\\" + renameFile(aFileName, repeatFileMap.get(aFileName));
					if (!Common.createFile(aFilePath)) {
						return ErrorCode.CREATE_FILE_FAIL;
					}
				} else if (!Common.createFile(aFilePath)) {
					return ErrorCode.CREATE_FILE_FAIL;
				}
				FileOutputStream fos = new FileOutputStream(aFilePath);
				fos.write(b);
				fos.flush();
				fos.close();
			}
			raf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			errorCode = ErrorCode.FILE_NOT_FOUND;
		} catch (IOException e) {
			e.printStackTrace();
			errorCode = ErrorCode.IO_ERROR;
		}
		logger.info("extract all file - end");
		error(errorCode);
		return errorCode;
	}

	// 解压�?个文件到指定目录
	@SuppressWarnings("resource")
	public int extractSingleFile(String fileName) {
		logger.info("extract single file - start");
		int flg = getFileContent();
		if (flg < 0)
			return flg;

		if (!Common.isPath(packedFile)) {
			errorCode = ErrorCode.PATTERN_NOT_MATCH;
			logger.info("pack file path:input error");
			return errorCode;
		}
		if (!Common.isPath(packToDir)) {
			errorCode = ErrorCode.PATTERN_NOT_MATCH;
			logger.info("pack to directory:input error");
			return errorCode;
		}

		File f = new File(packToDir);
		if (!f.isDirectory()) {
			f.mkdirs();
		}
		fileName = fileName.trim();
		int index = Common.BKDRHash(fileName) % header.length;
		int targetPos = -1;
		// 获取该file的header
		int foundFlg = 0;
		String resFileName;
		for (int next = 0; next + index < header.length && foundFlg == 0; next++) {
			if (header[index + next] == null)
				continue;
			resFileName = String.valueOf(header[index + next].fileName).trim();
			if (resFileName.equals(fileName)) {
				targetPos = index + next;
				foundFlg = 1;
			}
		}
		if (foundFlg == 0) {
			for (int next = -1; next + index >= 0 && foundFlg == 0; next--) {
				if (header[index + next] == null)
					continue;
				resFileName = String.valueOf(header[index + next].fileName).trim();
				if (resFileName.equals(fileName)) {
					targetPos = index + next;
					foundFlg = 1;
				}
			}
		}
		if (foundFlg == 0) {
			errorCode = ErrorCode.FILE_NOT_FOUND;
			logger.info("file name:not found in the packed file");
			error(errorCode);
			return errorCode;
		}

		try {
			RandomAccessFile raf = new RandomAccessFile(filePartTemp, "r");
			byte[] b = new byte[(int) header[targetPos].length];
			raf.seek(header[targetPos].offset);
			raf.read(b);
			String aFilePath = f.getPath() + "\\" + fileName;

			while (Common.isFileExist(aFilePath)) {
				if (repeatFileMap.get(fileName) == null) {
					repeatFileMap.put(fileName, 1);
				} else {
					Integer val = repeatFileMap.get(fileName);
					val++;
					repeatFileMap.put(fileName, val);
				}
				aFilePath = f.getPath() + "\\" + renameFile(fileName, repeatFileMap.get(fileName));
			}
			if (!Common.createFile(aFilePath)) {
				return ErrorCode.CREATE_FILE_FAIL;
			}
			FileOutputStream fos = new FileOutputStream(aFilePath);
			fos.write(b);
			fos.flush();
			fos.close();
			raf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			errorCode = ErrorCode.FILE_NOT_FOUND;
		} catch (IOException e) {
			e.printStackTrace();
			errorCode = ErrorCode.IO_ERROR;
		}
		logger.info("extract single file - end");
		error(errorCode);
		return errorCode;
	}
}
