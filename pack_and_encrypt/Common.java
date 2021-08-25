package pack_and_encrypt;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Common {

	// 路径正则表达式pattern
	static final String _regexp_path = "^[a-zA-Z]:[/\\\\][^?'/|$&\\\\]*([^?'/|$&\\\\]+[/\\\\][^?'/|$&\\\\]*)*$";

	// 密码正则表达式pattern
	static final String _regexp_psw = "^[\\w_]{8,16}$";

	// 文件后缀过滤pattern
	static final String _regexp_file_filter = "^\\s*(.[^\".'/\\\\]+\\s+)*.[^\".'/\\\\]+\\s*$";

	// 每次读取较大文件的字节数
	static final int eachTimeByte = 1024 * 10;

	// BKDR
	public static int BKDRHash(String str) {
		int seed = 131; // 31 131 1313 13131 131313 etc..
		int hash = 0;
		for (int i = 0; i < str.length(); i++) {
			hash = (hash * seed) + str.charAt(i);
		}
		return (hash & 0x7FFFFFFF);
	}

	// 判断密码格式是否合法
	public static boolean isPsw(String psw) {
		Pattern pattern = Pattern.compile(_regexp_psw);
		Matcher match = pattern.matcher(psw);
		// System.out.println(match.matches());
		return match.matches();
	}

	// 判断是否合法路径
	public static boolean isPath(String path) {
		Pattern pattern = Pattern.compile(_regexp_path);
		Matcher match = pattern.matcher(path);
		// System.out.println(match.matches());
		return match.matches();
	}

	// 判断是否合法文件后缀输入
	public static boolean isFileFilter(String ff) {
		Pattern pattern = Pattern.compile(_regexp_file_filter);
		Matcher match = pattern.matcher(ff);
		// System.out.println(match.matches());
		return match.matches();
	}

	// 字节型转化为int
	public static int byteToInt(byte b) {
		return (int) (b & 0xFF);
	}

	// 将一个文件写入另�?�?(不覆�?)
	public static int writeToAnotherFile(String filePath1, String filePath2) {
		int err = 0;
		if (!Common.isPath(filePath1) || !Common.isPath(filePath2)) {
			return ErrorCode.PATTERN_NOT_MATCH;
		}
		try {
			FileOutputStream fos = new FileOutputStream(filePath1, true);
			FileInputStream fis = new FileInputStream(filePath2);
			File f = new File(filePath2);

			int i = 1;
			byte[] b = new byte[(int) (eachTimeByte)];
			for (; i * eachTimeByte < f.length(); i++) {
				fis.read(b);
				fos.write(b);
				fos.flush();
			}

			b = new byte[(int) (f.length() - (i - 1) * eachTimeByte)];
			fis.read(b);
			fos.write(b);
			fos.flush();

			fis.close();
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			err = ErrorCode.FILE_NOT_FOUND;
		} catch (IOException e) {
			e.printStackTrace();
			err = ErrorCode.IO_ERROR;
		}
		return err;
	}

	// 判断文件是否存在
	public static boolean isFileExist(String filePath) {
		File f = new File(filePath);
		return f.exists();
	}

	// 创建文件
	public static boolean createFile(String filePathTemp) {
		boolean existFlg = true;
		File file = new File(filePathTemp);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				existFlg = false;
			}
		}
		return existFlg;
	}

	// 4字节数据转为int
	public static int fourByteToInt(byte[] b) {
		if (b.length != 4)
			return ErrorCode.INPUT_LENGTH_ERROR;
		int res = (b[3] & 0xff) | ((b[2] << 8) & 0xff00) | ((b[1] << 16)) | (b[0] << 24);
		return res;
	}

	// int转为4字节数据
	public static byte[] intToFourByte(int n) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) ((n >> ((3 - i) * 8)) & 0xFF);
		}
		return b;
	}

	// 删除单个文件
	public static int deleteFile(String filePath) {
		if (!Common.isPath(filePath)) {
			return ErrorCode.PATTERN_NOT_MATCH;
		}
		File f = new File(filePath);
		if (!f.delete())
			return ErrorCode.DELETE_FAIL;
		return 0;
	}

	// 递归查询文件夹下�?有文�?
	public static ArrayList<File> findFiles(String dir) {
		ArrayList<File> res = new ArrayList<File>();
		if (!isPath(dir))
			return null;
		File f = new File(dir);
		if (!f.isDirectory())
			return null;
		File[] fl = f.listFiles();
		if (fl == null)
			return null;
		for (int i = 0; i < fl.length; i++) {
			File ft = fl[i];
			if (ft.isDirectory()) {
				ArrayList<File> ft1 = findFiles(ft.getPath());
				res.addAll(ft1);
			} else {
				res.add(ft);
			}
		}
		return res;
	}

	// 判断�?个文件是否符合过滤条�?
	public static boolean isFitFilter(File f, final String[] fil) {
		if (fil == null)
			return true;
		if (f.isDirectory())
			return true;
		String s = f.getName().toLowerCase();
		int flg = 0;
		for (int i = 0; i < fil.length; i++) {
			if (s.endsWith(fil[i])) {
				flg = 1;
			}
		}
		if (flg == 1)
			return true;
		else
			return false;
	}

	// 递归查询文件夹下�?有文�?,过滤
	public static ArrayList<File> findFiles(String dir, final String[] fil) {
		ArrayList<File> res = new ArrayList<File>();
		if (!isPath(dir))
			return null;
		File f = new File(dir);
		if (!f.isDirectory())
			return null;

		File[] fl = null;
		if (fil != null) {
			FileFilter ff = new FileFilter() {
				@Override
				public boolean accept(File f) {
					return isFitFilter(f, fil);
				}
			};

			fl = f.listFiles(ff);
		} else
			fl = f.listFiles();

		if (fl == null)
			return null;
		for (int i = 0; i < fl.length; i++) {
			File ft = fl[i];
			if (ft.isDirectory()) {
				ArrayList<File> ft1 = findFiles(ft.getPath(), fil);
				if (ft1 != null) {
					res.addAll(ft1);
				}
			} else {
				res.add(ft);
			}
		}
		return res;
	}

	public static byte[] copyByteArrByLen(byte[] b, int len) {
		byte[] res = new byte[len];
		System.arraycopy(b, 0, res, 0, len);
		return res;
	}

	// 判断字节变量i位置�?1�?0
	public static int getByteBitOnPos(byte b, int i) {
		if (((b >> (7 - i)) & 0x01) == 1)
			return 1;
		else
			return 0;
	}
}
