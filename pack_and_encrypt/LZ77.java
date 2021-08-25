package pack_and_encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

//自制LZ77编码
//寻找匹配�?长串使用改进KMP算法
//原文字节前缀�?0，数组输出前�?�?1
//数组输出�?(偏移，长�?)
//偏移采用定长编码，匹配长度采用变长γ编�?
public class LZ77 extends BaseClass {

	// 静�?�内部类单例模式
	private static class InstanceHolder {
		private static final LZ77 INSTANCE = new LZ77();
	}

	protected LZ77() {
	}

	// 获取实例
	// aFilePath为待压缩文件路径
	static public final LZ77 getInstance(String aFilePath) {
		if (!Common.isPath(aFilePath)) {
			logger.info("the file to be compressed or depressed:input error");
			return null;
		}
		filePath = aFilePath;
		return InstanceHolder.INSTANCE;
	}

	// 待压缩文件路�?
	protected static String filePath;

	// 编码文件指定扩展�?
	protected static final String extension = ".slz";

	// 动�?�窗口大�?
	protected final int dynamicWindowSize = (int) Math.pow(2, shiftLength);

	// 定长偏移bit长度
	protected final static int shiftLength = 12;

	// �?短的用来编码的字节长�?(编码出来的长度必须小于非编码长度，否则不编码)
	protected final int leastEncodeByteNum = this.getLeastEncodeByteNum();

	// doLZ77
	@SuppressWarnings("resource")
	public int doLZ77() throws IOException, CloneNotSupportedException {
		logger.info("compress with LZ77 - start");
		int err = 0;
		if (!Common.isFileExist(filePath)) {
			logger.warn("file to be compressed:not exist");
			err = ErrorCode.FILE_NOT_FOUND;
			error(err);
			return err;
		}

		if (!Common.createFile(filePath + extension)) {
			logger.warn("create file to be compressed:fial");
			err = ErrorCode.CREATE_FILE_FAIL;
			error(err);
			return err;
		}

		File f = new File(filePath);
		long fullLen = f.length();
		long restLen = fullLen - dynamicWindowSize;
		long posFile = 0;
		int[] matchRes = new int[2];
		byte[] window = new byte[dynamicWindowSize];
		byte[] cache = new byte[dynamicWindowSize];
		BitOperatableData writeTemp = BitOperatableData.getInstance(Common.eachTimeByte);
		RandomAccessFile raf = new RandomAccessFile(filePath, "r");
		FileOutputStream fos = new FileOutputStream(filePath + extension);

		int readFlg = raf.read(window);
		fos.write(window);
		while (readFlg != -1 && restLen > 0) {
			if (restLen < dynamicWindowSize) {
				cache = new byte[(int) restLen];
			}
			raf.read(cache);
			matchRes = findLongestMatched(window, cache);

//            if(matchRes[1] > 5)
//                System.out.println(matchRes[1]);

			if (matchRes[1] < leastEncodeByteNum) {
				if (writeTemp.writeBitZero() == 1) {
					writeTemp.dataClear();
					writeTemp.writeBitZero();
				}
				if (writeTemp.writeBitOfByte(cache[0], 0) == 1) {
					fos.write(writeTemp.data);
					writeTemp.dataClearButSaveOverBit();
				}
				posFile++;
				restLen--;
			} else {
				if (writeTemp.writeBitOne() == 1) {
					writeTemp.dataClear();
					writeTemp.writeBitOne();
				}
				BitOperatableData tBod = intToBOD(matchRes[0], shiftLength);
				if (writeTemp.plus(tBod) == 1) {
					fos.write(writeTemp.data);
					writeTemp.dataClearButSaveOverBit();
				}
				tBod = gammaEncode(matchRes[1]);
				if (writeTemp.plus(tBod) == 1) {
					fos.write(writeTemp.data);
					writeTemp.dataClearButSaveOverBit();
				}
				posFile += matchRes[1];
				restLen -= matchRes[1];
			}
			raf.seek(posFile);
			if (posFile + dynamicWindowSize >= 71100) {
				posFile += 0;
			}
			readFlg = raf.read(window);
		}
		if (!writeTemp.isFull) {
			fos.write(writeTemp.getPaddingData());
		}
		fos.flush();
		fos.close();
		logger.info("compress with LZ77 - end");
		error(err);
		return err;
	}

	// 解压
	public int invLZ77(String tFilePath) throws IOException, CloneNotSupportedException {
		logger.info("depress with LZ77 - start");
		int err = 0;

		if (!Common.createFile(tFilePath)) {
			return ErrorCode.CREATE_FILE_FAIL;
		}

		File f = new File(filePath);
		long fullLen = (int) f.length();
		long restLen = fullLen;

		try (FileInputStream fis = new FileInputStream(filePath);
				FileOutputStream fos = new FileOutputStream(tFilePath)) {
			byte[] bTemp;
			BitOperatableData bodTemp = null;
			BitOperatableData lastTimeRemain = null;
			BitOperatableData bodToRead = null;

			int mLen = 0;

			bTemp = new byte[dynamicWindowSize];
			fis.read(bTemp);
			fos.write(bTemp);
			mLen += dynamicWindowSize;
			restLen -= dynamicWindowSize;

			byte[] window = new byte[dynamicWindowSize];
			System.arraycopy(bTemp, 0, window, 0, dynamicWindowSize);

			bTemp = new byte[Common.eachTimeByte];
			while (restLen > (fullLen - dynamicWindowSize) % Common.eachTimeByte) {
				fis.read(bTemp);
				restLen -= Common.eachTimeByte;
				bodTemp = BitOperatableData.getInstance(bTemp);
				if (lastTimeRemain != null) {
					bodToRead = BitOperatableData.getInstance(lastTimeRemain.data.length + bodTemp.data.length);
					bodToRead.plus(lastTimeRemain);
					bodToRead.plus(bodTemp);
				} else {
					bodToRead = bodTemp;
				}
				int flg;
				bodToRead.seek(0, 0);
				while ((flg = bodToRead.readBitOnPos()) != -1) {
					if (mLen >= 71100) {
						mLen += 0;
					}
					lastTimeRemain = null;
					if (flg == 0) {
						BitOperatableData t1 = bodToRead.read(8);
						if (t1 == null) {
							bodToRead.moveReadPoint(-1);
							lastTimeRemain = bodToRead.read();
							break;
						}
						fos.write(t1.data);
						mLen += 1;
						this.moveWindow(window, t1.data);
					} else {
						BitOperatableData t1 = bodToRead.read(shiftLength);
						if (t1 == null) {
							bodToRead.moveReadPoint(-1);
							lastTimeRemain = bodToRead.read();
							break;
						}
						int shift = t1.intoInt();
						int matchLen = this.invGammaEncode(bodToRead, bodToRead.pByte, bodToRead.pBit);
						if (matchLen == -1) {
							bodToRead.moveReadPoint(-1 - shiftLength);
							lastTimeRemain = bodToRead.read();
							break;
						}
						byte[] bToWrite = new byte[matchLen];
						System.arraycopy(window, shift, bToWrite, 0, matchLen);
						fos.write(bToWrite);
						mLen += matchLen;
						this.moveWindow(window, bToWrite);
					}
				}
			}
			bTemp = new byte[(int) restLen];
			fis.read(bTemp);
			bodTemp = BitOperatableData.getInstance(1);
			bodTemp.paddingDataToBOD(bTemp);
			if (lastTimeRemain != null) {
				bodToRead = BitOperatableData.getInstance(lastTimeRemain.data.length + bodTemp.data.length);
				bodToRead.plus(lastTimeRemain);
				bodToRead.plus(bodTemp);
			} else {
				bodToRead = bodTemp;
			}
			int flg;
			while ((flg = bodToRead.readBitOnPos()) != -1) {
				if (flg == 0) {
					BitOperatableData t1 = bodToRead.read(8);
					if (t1 == null) {
						return -1;
					}
					fos.write(t1.data);
					this.moveWindow(window, t1.data);
				} else {
					BitOperatableData t1 = bodToRead.read(shiftLength);
					int shift = t1.intoInt();
					int matchLen = this.invGammaEncode(bodToRead, bodToRead.pByte, bodToRead.pBit);
					if (matchLen == -1) {
						return -1;
					}
					byte[] bToWrite = new byte[matchLen];
					System.arraycopy(window, shift, bToWrite, 0, matchLen);

//                String s =new String(bToWrite,"ascii");
//                System.out.println(s);

					fos.write(bToWrite);
					this.moveWindow(window, bToWrite);
				}
			}
			fos.flush();
		}

		logger.info("depress with LZ77 - end");
		error(err);
		return err;
	}

	// 寻找�?长串
	public int[] findLongestMatched(byte[] window, byte[] cache) throws UnsupportedEncodingException {
		// res[0]:位置，res[1]长度
		int[] res = new int[2];
		int len = 0, pos = 0;
		byte[] temp = Common.copyByteArrByLen(cache, 1);
		// KMP模式匹配
		KMP k = KMP.getInstance();
		int tPos = k.findFirstIndex(window, temp, 0);
		int tLen = len;
		while (tPos + tLen < window.length && tPos != -1) {
			while (tPos + tLen < window.length && tLen < cache.length && window[tPos + tLen] == cache[tLen]) {
				tLen++;
			}
			if (tLen > len) {
				len = tLen;
				pos = tPos;
				temp = Common.copyByteArrByLen(cache, tLen);
			}
			tPos = k.findFirstIndex(window, temp, ++tPos);
		}
		res[0] = pos;
		res[1] = len;

//        byte[] tt = new byte[len];
//        System.arraycopy(window, pos, tt, 0, len);
//        String s =new String(tt,"ascii");
//        System.out.println(s);

		return res;
	}

	// 对len进行γ编码
	// x = 2^q + p
	private BitOperatableData gammaEncode(int n) {
		if (n < 1)
			return null;
		int q = (int) (Math.log(n) / Math.log(2));
		int length = q + q + 1;
		int resSize = (int) ((length - 1) / 8) + 1;
		BitOperatableData res = BitOperatableData.getInstance(resSize);
		if (n == 1) {
			return res;
		}
		for (int i = 0; i < q; i++) {
			res.writeBitOne();
		}
		res.writeBitZero();
		byte temp = (byte) (n - Math.pow(2, q));
		res.writeBitOfByte(temp, 8 - q);
		return res;
	}

	// int型转化为γ编码的bit�?
	private int getGammaEncodeLength(int n) {
		if (n < 1)
			return -1;
		int q = (int) (Math.log(n) / Math.log(2));
		return (q + q + 1);
	}

	// 计算�?短编码字�?
	private int getLeastEncodeByteNum() {
		int n = 2;
		while (true) {
			if (getGammaEncodeLength(n) + shiftLength < n * 8) {
				return n;
			}
		}
	}

	// 偏移int型转化为bitLen长度的BOS�?
	private BitOperatableData intToBOD(int num, int bitLen) {
		BitOperatableData res = BitOperatableData.getInstance((bitLen - 1) / 8 + 1);
		int lastBytePos = (bitLen - 1) / 8;
		int lastBitPos = bitLen - lastBytePos * 8 - 1;
		byte temp;
		for (int i = 0; i < lastBytePos; i++) {
			temp = (byte) (num >> (bitLen - lastBytePos * 8 + (lastBytePos - 1 - i) * 8));
			res.writeBitOfByte(temp, 0);
			bitLen -= 8;
		}
		temp = (byte) num;
		res.writeBitOfByte(temp, 7 - lastBitPos);
		return res;
	}

	// bod数据�?(n,i)位置�?始，读取gamma编码，解码返回int�?
	private int invGammaEncode(BitOperatableData bod, int n, int i) {
		int res = 0;
		// q�?大为12
		int q = bod.readNumOfContinuousOne(n, i);
		if (q == -1)
			return -1;
		// bod.move(1);
		BitOperatableData pBod;
		if ((pBod = bod.read(q)) == null) {
			bod.moveReadPoint(-1 - q);
			return -1;
		}
		int p = pBod.intoInt();

		res = (int) (Math.pow(2, q) + p);
		return res;
	}

	// 移动window操作
	private void moveWindow(byte[] window, byte[] b) {
		int len1 = window.length;
		int len2 = b.length;
		byte[] temp = new byte[len1];
		System.arraycopy(window, 0, temp, 0, len1);
		System.arraycopy(temp, len2, window, 0, len1 - len2);
		System.arraycopy(b, 0, window, len1 - len2, len2);
	}
}
