package pack_and_encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

//自制加密
public class Encrypt extends BaseClass {

	// 静�?�内部类单例模式
	private static class InstanceHolder {
		private static final Encrypt INSTANCE = new Encrypt();
	}

	protected Encrypt() {
	}

	// 获取实例
	// aFilePath为要加密或解密的文件，aPassword为加密或者解密的密码
	static public final Encrypt getInstance(String aFilePath, String aPassword) {
		if (!Common.isPsw(aPassword)) {
			logger.info("the password:input error");
			return null;
		}
		if (!Common.isPath(aFilePath)) {
			logger.info("the file to be encrypted or decrypted:input error");
			return null;
		}
		filePath = aFilePath;
		password = aPassword;
		return InstanceHolder.INSTANCE;
	}

	// 加密文件指定扩展�?
	final String extension = ".enc";

	// 加密文件路径
	static String filePath;

	// 补位填充长度(采用PKCS7填充，填充长度也是填充数值大�?)
	int paddingLength;

	// 输入密码
	static String password;

	// 密码校验码长�?4字节
	final int checkCodeLength = 4;

	// 基密�?
	byte[] baseKey = new byte[4 * 4];

	// 轮密�?
	byte[] roundKey = new byte[44 * 4];

	// S盒方�?(置换�?)
	private final byte[] sBox = {
			/* 0 1 2 3 4 5 6 7 8 9 a b c d e f */
			(byte) 0x63, (byte) 0x7c, (byte) 0x77, (byte) 0x7b, (byte) 0xf2, (byte) 0x6b, (byte) 0x6f, (byte) 0xc5,
			(byte) 0x30, (byte) 0x01, (byte) 0x67, (byte) 0x2b, (byte) 0xfe, (byte) 0xd7, (byte) 0xab, (byte) 0x76, /*
																													 * 0
																													 */
			(byte) 0xca, (byte) 0x82, (byte) 0xc9, (byte) 0x7d, (byte) 0xfa, (byte) 0x59, (byte) 0x47, (byte) 0xf0,
			(byte) 0xad, (byte) 0xd4, (byte) 0xa2, (byte) 0xaf, (byte) 0x9c, (byte) 0xa4, (byte) 0x72, (byte) 0xc0, /*
																													 * 1
																													 */
			(byte) 0xb7, (byte) 0xfd, (byte) 0x93, (byte) 0x26, (byte) 0x36, (byte) 0x3f, (byte) 0xf7, (byte) 0xcc,
			(byte) 0x34, (byte) 0xa5, (byte) 0xe5, (byte) 0xf1, (byte) 0x71, (byte) 0xd8, (byte) 0x31, (byte) 0x15, /*
																													 * 2
																													 */
			(byte) 0x04, (byte) 0xc7, (byte) 0x23, (byte) 0xc3, (byte) 0x18, (byte) 0x96, (byte) 0x05, (byte) 0x9a,
			(byte) 0x07, (byte) 0x12, (byte) 0x80, (byte) 0xe2, (byte) 0xeb, (byte) 0x27, (byte) 0xb2, (byte) 0x75, /*
																													 * 3
																													 */
			(byte) 0x09, (byte) 0x83, (byte) 0x2c, (byte) 0x1a, (byte) 0x1b, (byte) 0x6e, (byte) 0x5a, (byte) 0xa0,
			(byte) 0x52, (byte) 0x3b, (byte) 0xd6, (byte) 0xb3, (byte) 0x29, (byte) 0xe3, (byte) 0x2f, (byte) 0x84, /*
																													 * 4
																													 */
			(byte) 0x53, (byte) 0xd1, (byte) 0x00, (byte) 0xed, (byte) 0x20, (byte) 0xfc, (byte) 0xb1, (byte) 0x5b,
			(byte) 0x6a, (byte) 0xcb, (byte) 0xbe, (byte) 0x39, (byte) 0x4a, (byte) 0x4c, (byte) 0x58, (byte) 0xcf, /*
																													 * 5
																													 */
			(byte) 0xd0, (byte) 0xef, (byte) 0xaa, (byte) 0xfb, (byte) 0x43, (byte) 0x4d, (byte) 0x33, (byte) 0x85,
			(byte) 0x45, (byte) 0xf9, (byte) 0x02, (byte) 0x7f, (byte) 0x50, (byte) 0x3c, (byte) 0x9f, (byte) 0xa8, /*
																													 * 6
																													 */
			(byte) 0x51, (byte) 0xa3, (byte) 0x40, (byte) 0x8f, (byte) 0x92, (byte) 0x9d, (byte) 0x38, (byte) 0xf5,
			(byte) 0xbc, (byte) 0xb6, (byte) 0xda, (byte) 0x21, (byte) 0x10, (byte) 0xff, (byte) 0xf3, (byte) 0xd2, /*
																													 * 7
																													 */
			(byte) 0xcd, (byte) 0x0c, (byte) 0x13, (byte) 0xec, (byte) 0x5f, (byte) 0x97, (byte) 0x44, (byte) 0x17,
			(byte) 0xc4, (byte) 0xa7, (byte) 0x7e, (byte) 0x3d, (byte) 0x64, (byte) 0x5d, (byte) 0x19, (byte) 0x73, /*
																													 * 8
																													 */
			(byte) 0x60, (byte) 0x81, (byte) 0x4f, (byte) 0xdc, (byte) 0x22, (byte) 0x2a, (byte) 0x90, (byte) 0x88,
			(byte) 0x46, (byte) 0xee, (byte) 0xb8, (byte) 0x14, (byte) 0xde, (byte) 0x5e, (byte) 0x0b, (byte) 0xdb, /*
																													 * 9
																													 */
			(byte) 0xe0, (byte) 0x32, (byte) 0x3a, (byte) 0x0a, (byte) 0x49, (byte) 0x06, (byte) 0x24, (byte) 0x5c,
			(byte) 0xc2, (byte) 0xd3, (byte) 0xac, (byte) 0x62, (byte) 0x91, (byte) 0x95, (byte) 0xe4, (byte) 0x79, /*
																													 * a
																													 */
			(byte) 0xe7, (byte) 0xc8, (byte) 0x37, (byte) 0x6d, (byte) 0x8d, (byte) 0xd5, (byte) 0x4e, (byte) 0xa9,
			(byte) 0x6c, (byte) 0x56, (byte) 0xf4, (byte) 0xea, (byte) 0x65, (byte) 0x7a, (byte) 0xae, (byte) 0x08, /*
																													 * b
																													 */
			(byte) 0xba, (byte) 0x78, (byte) 0x25, (byte) 0x2e, (byte) 0x1c, (byte) 0xa6, (byte) 0xb4, (byte) 0xc6,
			(byte) 0xe8, (byte) 0xdd, (byte) 0x74, (byte) 0x1f, (byte) 0x4b, (byte) 0xbd, (byte) 0x8b, (byte) 0x8a, /*
																													 * c
																													 */
			(byte) 0x70, (byte) 0x3e, (byte) 0xb5, (byte) 0x66, (byte) 0x48, (byte) 0x03, (byte) 0xf6, (byte) 0x0e,
			(byte) 0x61, (byte) 0x35, (byte) 0x57, (byte) 0xb9, (byte) 0x86, (byte) 0xc1, (byte) 0x1d, (byte) 0x9e, /*
																													 * d
																													 */
			(byte) 0xe1, (byte) 0xf8, (byte) 0x98, (byte) 0x11, (byte) 0x69, (byte) 0xd9, (byte) 0x8e, (byte) 0x94,
			(byte) 0x9b, (byte) 0x1e, (byte) 0x87, (byte) 0xe9, (byte) 0xce, (byte) 0x55, (byte) 0x28, (byte) 0xdf, /*
																													 * e
																													 */
			(byte) 0x8c, (byte) 0xa1, (byte) 0x89, (byte) 0x0d, (byte) 0xbf, (byte) 0xe6, (byte) 0x42, (byte) 0x68,
			(byte) 0x41, (byte) 0x99, (byte) 0x2d, (byte) 0x0f, (byte) 0xb0, (byte) 0x54, (byte) 0xbb,
			(byte) 0x16 /* f */
	};

	// S盒的逆方�?
	private final byte[] invSBox = {
			/* 0 1 2 3 4 5 6 7 8 9 a b c d e f */
			(byte) 0x52, (byte) 0x09, (byte) 0x6a, (byte) 0xd5, (byte) 0x30, (byte) 0x36, (byte) 0xa5, (byte) 0x38,
			(byte) 0xbf, (byte) 0x40, (byte) 0xa3, (byte) 0x9e, (byte) 0x81, (byte) 0xf3, (byte) 0xd7, (byte) 0xfb, /*
																													 * 0
																													 */
			(byte) 0x7c, (byte) 0xe3, (byte) 0x39, (byte) 0x82, (byte) 0x9b, (byte) 0x2f, (byte) 0xff, (byte) 0x87,
			(byte) 0x34, (byte) 0x8e, (byte) 0x43, (byte) 0x44, (byte) 0xc4, (byte) 0xde, (byte) 0xe9, (byte) 0xcb, /*
																													 * 1
																													 */
			(byte) 0x54, (byte) 0x7b, (byte) 0x94, (byte) 0x32, (byte) 0xa6, (byte) 0xc2, (byte) 0x23, (byte) 0x3d,
			(byte) 0xee, (byte) 0x4c, (byte) 0x95, (byte) 0x0b, (byte) 0x42, (byte) 0xfa, (byte) 0xc3, (byte) 0x4e, /*
																													 * 2
																													 */
			(byte) 0x08, (byte) 0x2e, (byte) 0xa1, (byte) 0x66, (byte) 0x28, (byte) 0xd9, (byte) 0x24, (byte) 0xb2,
			(byte) 0x76, (byte) 0x5b, (byte) 0xa2, (byte) 0x49, (byte) 0x6d, (byte) 0x8b, (byte) 0xd1, (byte) 0x25, /*
																													 * 3
																													 */
			(byte) 0x72, (byte) 0xf8, (byte) 0xf6, (byte) 0x64, (byte) 0x86, (byte) 0x68, (byte) 0x98, (byte) 0x16,
			(byte) 0xd4, (byte) 0xa4, (byte) 0x5c, (byte) 0xcc, (byte) 0x5d, (byte) 0x65, (byte) 0xb6, (byte) 0x92, /*
																													 * 4
																													 */
			(byte) 0x6c, (byte) 0x70, (byte) 0x48, (byte) 0x50, (byte) 0xfd, (byte) 0xed, (byte) 0xb9, (byte) 0xda,
			(byte) 0x5e, (byte) 0x15, (byte) 0x46, (byte) 0x57, (byte) 0xa7, (byte) 0x8d, (byte) 0x9d, (byte) 0x84, /*
																													 * 5
																													 */
			(byte) 0x90, (byte) 0xd8, (byte) 0xab, (byte) 0x00, (byte) 0x8c, (byte) 0xbc, (byte) 0xd3, (byte) 0x0a,
			(byte) 0xf7, (byte) 0xe4, (byte) 0x58, (byte) 0x05, (byte) 0xb8, (byte) 0xb3, (byte) 0x45, (byte) 0x06, /*
																													 * 6
																													 */
			(byte) 0xd0, (byte) 0x2c, (byte) 0x1e, (byte) 0x8f, (byte) 0xca, (byte) 0x3f, (byte) 0x0f, (byte) 0x02,
			(byte) 0xc1, (byte) 0xaf, (byte) 0xbd, (byte) 0x03, (byte) 0x01, (byte) 0x13, (byte) 0x8a, (byte) 0x6b, /*
																													 * 7
																													 */
			(byte) 0x3a, (byte) 0x91, (byte) 0x11, (byte) 0x41, (byte) 0x4f, (byte) 0x67, (byte) 0xdc, (byte) 0xea,
			(byte) 0x97, (byte) 0xf2, (byte) 0xcf, (byte) 0xce, (byte) 0xf0, (byte) 0xb4, (byte) 0xe6, (byte) 0x73, /*
																													 * 8
																													 */
			(byte) 0x96, (byte) 0xac, (byte) 0x74, (byte) 0x22, (byte) 0xe7, (byte) 0xad, (byte) 0x35, (byte) 0x85,
			(byte) 0xe2, (byte) 0xf9, (byte) 0x37, (byte) 0xe8, (byte) 0x1c, (byte) 0x75, (byte) 0xdf, (byte) 0x6e, /*
																													 * 9
																													 */
			(byte) 0x47, (byte) 0xf1, (byte) 0x1a, (byte) 0x71, (byte) 0x1d, (byte) 0x29, (byte) 0xc5, (byte) 0x89,
			(byte) 0x6f, (byte) 0xb7, (byte) 0x62, (byte) 0x0e, (byte) 0xaa, (byte) 0x18, (byte) 0xbe, (byte) 0x1b, /*
																													 * a
																													 */
			(byte) 0xfc, (byte) 0x56, (byte) 0x3e, (byte) 0x4b, (byte) 0xc6, (byte) 0xd2, (byte) 0x79, (byte) 0x20,
			(byte) 0x9a, (byte) 0xdb, (byte) 0xc0, (byte) 0xfe, (byte) 0x78, (byte) 0xcd, (byte) 0x5a, (byte) 0xf4, /*
																													 * b
																													 */
			(byte) 0x1f, (byte) 0xdd, (byte) 0xa8, (byte) 0x33, (byte) 0x88, (byte) 0x07, (byte) 0xc7, (byte) 0x31,
			(byte) 0xb1, (byte) 0x12, (byte) 0x10, (byte) 0x59, (byte) 0x27, (byte) 0x80, (byte) 0xec, (byte) 0x5f, /*
																													 * c
																													 */
			(byte) 0x60, (byte) 0x51, (byte) 0x7f, (byte) 0xa9, (byte) 0x19, (byte) 0xb5, (byte) 0x4a, (byte) 0x0d,
			(byte) 0x2d, (byte) 0xe5, (byte) 0x7a, (byte) 0x9f, (byte) 0x93, (byte) 0xc9, (byte) 0x9c, (byte) 0xef, /*
																													 * d
																													 */
			(byte) 0xa0, (byte) 0xe0, (byte) 0x3b, (byte) 0x4d, (byte) 0xae, (byte) 0x2a, (byte) 0xf5, (byte) 0xb0,
			(byte) 0xc8, (byte) 0xeb, (byte) 0xbb, (byte) 0x3c, (byte) 0x83, (byte) 0x53, (byte) 0x99, (byte) 0x61, /*
																													 * e
																													 */
			(byte) 0x17, (byte) 0x2b, (byte) 0x04, (byte) 0x7e, (byte) 0xba, (byte) 0x77, (byte) 0xd6, (byte) 0x26,
			(byte) 0xe1, (byte) 0x69, (byte) 0x14, (byte) 0x63, (byte) 0x55, (byte) 0x21, (byte) 0x0c,
			(byte) 0x7d /* f */
	};

	// 返回密码校验
	private byte[] getCheckCode(String key) {
		byte[] res = new byte[checkCodeLength];
		Integer resInt = Common.BKDRHash(key);
		for (int i = 0; i < checkCodeLength; i++) {
			res[checkCodeLength - 1 - i] = (byte) (resInt >> (8 * i));
		}
		return res;
	}

	// AES
	public int doAES() {
		logger.info("encrypt with AES - start");
		int err = 0;
		File f = new File(filePath);
		int fullLen = (int) f.length();
		int restLen = fullLen;
		if (!f.exists())
			return ErrorCode.FILE_NOT_FOUND;
		if (!Common.createFile(filePath + extension))
			return ErrorCode.CREATE_FILE_FAIL;

		paddingLength = 16 - fullLen % 16;

		String aPsw = password;
		while (aPsw.length() < 16) {
			aPsw += aPsw.charAt(131 % aPsw.length());
		}
		baseKey = aPsw.getBytes();
		roundKey = keyExpansion(baseKey);

		try {
			byte[] eachIO = new byte[Common.eachTimeByte];
			int pos = 0;
			byte[] b = new byte[16];
			FileInputStream fis = new FileInputStream(filePath);
			FileOutputStream fos = new FileOutputStream(filePath + extension);

			// 写入密码校验
			byte[] pwdCode = new byte[checkCodeLength];
			pwdCode = getCheckCode(aPsw);
			fos.write(pwdCode);

			for (; restLen > fullLen % Common.eachTimeByte; restLen -= 16) {
				fis.read(b);
				b = clipher(b);
				for (int i = 0; i < 16; i++) {
					eachIO[pos] = b[i];
					pos++;
				}
				if (pos == Common.eachTimeByte) {
					fos.write(eachIO);
					pos = 0;
				}
			}
			for (; restLen >= 16; restLen -= 16) {
				fis.read(b);
				b = clipher(b);
				fos.write(b);
			}
			if (paddingLength == 16) {
				for (int i = 0; i < 16; i++) {
					b[i] = 16;
				}
			} else {
				fis.read(b);
				for (int i = 16 - paddingLength; i < 16; i++) {
					b[i] = (byte) paddingLength;
				}
			}
			b = clipher(b);
			fos.write(b);
			fos.flush();
			fos.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
			err = ErrorCode.IO_ERROR;
		}
		logger.info("encrypt with AES - end");
		error(err);
		return err;
	}

	// 解AES
	@SuppressWarnings("resource")
	public int invAES() {
		logger.info("decrypt with AES - start");
		int err = 0;
		for (int i = 0; i < 4; i++) {
			if (!filePath.endsWith(extension))
				return ErrorCode.EXTENSION_FALSE;
		}
		String cFilePath = filePath.substring(0, filePath.length() - 4);

		File f = new File(filePath);
		int fullLen = (int) f.length();
		int restLen = fullLen;
		if (!f.exists())
			return ErrorCode.FILE_NOT_FOUND;
		if (!Common.createFile(cFilePath))
			return ErrorCode.CREATE_FILE_FAIL;

		String aPsw = password;
		while (aPsw.length() < 16) {
			aPsw += aPsw.charAt(131 % aPsw.length());
		}
		baseKey = aPsw.getBytes();
		roundKey = keyExpansion(baseKey);

		try {
			byte[] eachIO = new byte[Common.eachTimeByte];
			int pos = 0;
			byte[] b = new byte[16];
			FileInputStream fis = new FileInputStream(filePath);
			FileOutputStream fos = new FileOutputStream(cFilePath);

			// 读取密码校验
			byte[] pwdCodeRead = new byte[checkCodeLength];
			byte[] pwdCodeTrue = new byte[checkCodeLength];
			fis.read(pwdCodeRead);
			restLen -= checkCodeLength;
			pwdCodeTrue = getCheckCode(aPsw);
			for (int i = 0; i < checkCodeLength; i++) {
				if (pwdCodeTrue[i] != pwdCodeRead[i])
					return ErrorCode.PASSWORD_NOT_MATCH_OR_NOT_AESFILE;
			}

			for (; restLen > (fullLen - checkCodeLength) % Common.eachTimeByte; restLen -= 16) {
				fis.read(b);
				b = invClipher(b);
				for (int i = 0; i < 16; i++) {
					eachIO[pos] = b[i];
					pos++;
				}
				if (pos == Common.eachTimeByte) {
					fos.write(eachIO);
					pos = 0;
				}
			}
			for (; restLen >= 16; restLen -= 16) {
				fis.read(b);
				b = invClipher(b);
				fos.write(b);
			}
			if (restLen != 16)
				return ErrorCode.FILE_LENGTH_NOT_16_MULTIPLE;
			fis.read(b);
			b = invClipher(b);
			if (b[15] > 16)
				return ErrorCode.NOT_AESFILE;
			if (b[15] < 16) {
				byte[] lastData = new byte[16 - b[15]];
				for (int i = 0; i < lastData.length; i++) {
					lastData[i] = b[i];
				}
				fos.write(lastData);
			}
			fos.flush();
			fos.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			err = ErrorCode.FILE_NOT_FOUND;
		} catch (IOException e) {
			e.printStackTrace();
			err = ErrorCode.IO_ERROR;
		}
		logger.info("decrypt with AES - end");
		error(err);
		return err;
	}

	// 加密主要过程
	private byte[] clipher(byte[] b) {
		if (b.length != 16)
			return null;

		byte[][] state = new byte[4][4];
		int i, r, c;

		for (r = 0; r < 4; r++) {
			for (c = 0; c < 4; c++) {
				state[r][c] = b[c * 4 + r];
			}
		}

		addRoundKey(state, 0);

		for (i = 1; i <= 10; i++) {
			subBytes(state);
			shiftRows(state);
			if (i != 10)
				mixColumns(state);
			addRoundKey(state, i);
		}

		for (r = 0; r < 4; r++) {
			for (c = 0; c < 4; c++) {
				b[c * 4 + r] = state[r][c];
			}
		}
		return b;
	}

	// 解密主要过程
	private byte[] invClipher(byte[] b) {
		if (b.length != 16)
			return null;

		byte[][] state = new byte[4][4];
		int i, r, c;

		for (r = 0; r < 4; r++) {
			for (c = 0; c < 4; c++) {
				state[r][c] = b[c * 4 + r];
			}
		}

		addRoundKey(state, 10);

		for (i = 9; i >= 0; i--) {
			invShiftRows(state);
			invSubBytes(state);
			addRoundKey(state, i);
			if (i != 0)
				invMixColumns(state);
		}

		for (r = 0; r < 4; r++) {
			for (c = 0; c < 4; c++) {
				b[c * 4 + r] = state[r][c];
			}
		}
		return b;
	}

	// 字节替代
	private void subBytes(byte state[][]) {
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 4; c++) {
				state[r][c] = (byte) sBox[Common.byteToInt(state[r][c])];
			}
		}
	}

	// 行移位变�?
	private void shiftRows(byte state[][]) {
		byte[] temp = new byte[4];
		int r, c;
		for (r = 1; r < 4; r++) {
			for (c = 0; c < 4; c++) {
				temp[c] = state[r][(r + c) % 4];
			}
			for (c = 0; c < 4; c++) {
				state[r][c] = temp[c];
			}
		}
	}

	// 列混淆变�?
	private void mixColumns(byte state[][]) {
		byte[] t = new byte[4];
		int r, c;
		for (c = 0; c < 4; c++) {
			for (r = 0; r < 4; r++) {
				t[r] = state[r][c];
			}
			for (r = 0; r < 4; r++) {
				state[r][c] = (byte) (FFmul((byte) 0x02, t[r]) // 在GF(2^8)中，加法就是异或运算
						^ FFmul((byte) 0x03, t[(r + 1) % 4]) ^ FFmul((byte) 0x01, t[(r + 2) % 4])
						^ FFmul((byte) 0x01, t[(r + 3) % 4]));
			}
		}
	}

	// 轮密钥加变换
	private void addRoundKey(byte state[][], int round) {
		for (int c = 0; c < 4; c++) {
			for (int r = 0; r < 4; r++) {
				state[r][c] ^= roundKey[round * 4 * 4 + c * 4 + r];
			}
		}
	}

	// 密钥扩展
	private byte[] keyExpansion(byte key[]) {
		if (key.length != 16)
			return null;
		byte[] roundKey = new byte[44 * 4];
		byte[] temp = new byte[4];
		byte swap;
		byte rcon[] = { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, (byte) 0x80, 0x1b, 0x36 };
		// 第一轮密钥就是原始密�?
		int i, j;
		for (i = 0; i < 4; i++) {
			for (j = 0; j < 4; j++) {
				roundKey[i * 4 + j] = key[i * 4 + j];
			}
		}

		// 其他密钥均来源于上轮密钥
		while (i < 44) {
			for (j = 0; j < 4; j++) {
				temp[j] = roundKey[(i - 1) * 4 + j]; // 取轮密钥�?后一�?
			}
			if (i % 4 == 0) {
				// 32位字�?4字节循环左移�?�?
				swap = temp[0];
				temp[0] = temp[1];
				temp[1] = temp[2];
				temp[2] = temp[3];
				temp[3] = swap;

				// S盒转�?
				for (int k = 0; k < 4; k++) {
					temp[k] = (byte) sBox[Common.byteToInt(temp[k])];
				}

				// 与rcon异或
				temp[0] = (byte) (temp[0] ^ rcon[i / 4 - 1]);
			}
			for (int k = 0; k < 4; k++) {
				roundKey[i * 4 + k] = (byte) (roundKey[(i - 4) * 4 + k] ^ temp[k]);
			}
			i++;
		}
		return roundKey;
	}

	// 逆字节替�?
	private void invSubBytes(byte state[][]) {
		for (int r = 0; r < 4; r++) {
			for (int c = 0; c < 4; c++) {
				state[r][c] = (byte) invSBox[Common.byteToInt(state[r][c])];
			}
		}
	}

	// 逆行移位
	private void invShiftRows(byte state[][]) {
		byte[] temp = new byte[4];
		int r, c;
		for (r = 1; r < 4; r++) {
			for (c = 0; c < 4; c++) {
				temp[c] = state[r][(c - r + 4) % 4];
			}
			for (c = 0; c < 4; c++) {
				state[r][c] = temp[c];
			}
		}
	}

	// 逆列混淆
	private void invMixColumns(byte state[][]) {
		byte[] temp = new byte[4];
		int r, c;
		for (c = 0; c < 4; c++) {
			for (r = 0; r < 4; r++) {
				temp[r] = state[r][c];
			}
			for (r = 0; r < 4; r++) {
				state[r][c] = (byte) (FFmul((byte) 0x0e, temp[r]) ^ FFmul((byte) 0x0b, temp[(r + 1) % 4])
						^ FFmul((byte) 0x0d, temp[(r + 2) % 4]) ^ FFmul((byte) 0x09, temp[(r + 3) % 4]));
			}
		}
	}

	// 有限域GF(2^8)上的乘法
	// 由于只涉�?128位，4x4字节的加密，实际a只有4位，但是此处还是给出了�?�用8位的算法
	private static byte FFmul(byte a, byte b) {
		byte[] bw = new byte[8];
		byte res = 0;
		bw[0] = b;
		for (int i = 1; i < bw.length; i++) {
			bw[i] = (byte) (bw[i - 1] << 1);
			if (bw[i - 1] < 0) {
				bw[i] ^= 0x1b;
			}
		}
		for (int i = 0; i < bw.length; i++) {
			if (((a >> i) & 0x01) == 0x01) {
				res ^= bw[i];
			}
		}
		return (byte) res;
	}

//    //是否是无符号8位整形范�?(0-255)
//    public static int isUnsinged8Bit(int a){
//        if(a >= 0 && a < 256)
//            return 0;
//        return ErrorCode.OUT_OF_RANGE_8BIT;
//    }
}
