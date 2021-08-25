package pack_and_encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

//霍夫曼树
//�?0，右1，左权小，右权大
public class Huffman extends BaseClass {

	// 静�?�内部类单例模式
	private static class InstanceHolder {
		private static final Huffman INSTANCE = new Huffman();
	}

	protected Huffman() {
		for (int i = 0; i < byteNode.length; i++) {
			byteNode[i] = new HuffmanNode();
			byteNode[i].value = i;
		}
	}

	// 获取实例
	static public final Huffman getInstance(String aFilePath) {
		if (!Common.isPath(aFilePath)) {
			logger.info("the file to be encoded or decoded with Huffman:input error");
			return null;
		}
		filePath = aFilePath;
		return InstanceHolder.INSTANCE;
	}

	// 输入文件路径
	public static String filePath;

	// 对每�?种字节�?�建立一个节点对象，数量�?256+1
	// 字节数�??(x)跟节点对象在数组中的序号(n)有f(x)对应关系。根节点在数组中的序号为256
	// f(x)为对x进行强制int转换，即节点value为byte的int�?
	HuffmanNode[] byteNode = new HuffmanNode[257];

	// 用于表示空的节点value�?
	private final int nullNodeValue = 0xffff;

	// 辅助节点，非实际字节的节点对�?
	ArrayList<HuffmanNode> midNode = new ArrayList<HuffmanNode>();

	// 用于保存编码结果
	String[] codingRes = new String[256];

	// 编码文件指定扩展�?
	protected static final String extension = ".shf";

	// 通过字节的�?�获取到相应的字节节�?
	private HuffmanNode getNodeByByte(byte b) {
		if (b >= 0) {
			return byteNode[b];
		} else {
			return byteNode[(b & 0xff)];
		}
	}

	// 静�?�霍夫曼编码
	public int doStaticHuffman() throws IOException, CloneNotSupportedException {
		logger.info("huffman encoding - start");
		int err = 0;
		if (!Common.isFileExist(filePath)) {
			logger.info("the file to be encoded with Huffman:not found");
			return ErrorCode.NOT_DIRECTORY;
		}

		String targetFilePath = filePath + extension;
		if (!Common.createFile(targetFilePath)) {
			return ErrorCode.CREATE_FILE_FAIL;
		}

		byte[] b = new byte[Common.eachTimeByte];

		File f = new File(filePath);
		long fullLen = f.length();
		long restLen = fullLen;
		FileInputStream fis;
		FileOutputStream fos = new FileOutputStream(targetFilePath);

		// 得到霍夫曼树
		this.makeHuffmanTree(f);
		// 将霍夫曼树写入文件开�?
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(byteNode);
		oos.writeObject(midNode);
		oos.flush();
		oos.close();
		fos.close();
		File f1 = new File(targetFilePath);
		// 写入文件部分长度4字节
		fos = new FileOutputStream(targetFilePath, true);
		fos.write(Common.intToFourByte((int) f1.length() + 4));

		// 第二次遍历开始编�?
		fis = new FileInputStream(f);
		BitOperatableData bod = BitOperatableData.getInstance(Common.eachTimeByte);
		BitOperatableData code;
		while (restLen > fullLen % Common.eachTimeByte) {
			fis.read(b);
			restLen -= Common.eachTimeByte;
			for (int i = 0; i < Common.eachTimeByte; i++) {
				code = this.byteToHuffmanCode(b[i]);
				bod.plus(code);
				if (bod.isFull == true) {
					fos.write(bod.data);
					bod.dataClearButSaveOverBit();
				}
			}
		}
		b = new byte[(int) restLen];
		fis.read(b);
		for (int i = 0; i < restLen; i++) {
			code = this.byteToHuffmanCode(b[i]);
			bod.plus(code);
			if (bod.isFull == true) {
				fos.write(bod.data);
				bod.dataClearButSaveOverBit();
			}
		}
		fos.write(bod.getPaddingData());
		fis.close();
		fos.flush();
		oos.close();
		fos.close();
		for (int i = 0; i < codingRes.length; i++)
			System.out.println(codingRes[i]);
		logger.info("huffman encoding - end");
		error(err);
		return err;
	}

	// 静�?�霍夫曼解码
	@SuppressWarnings("unchecked")
	public int invStaticHuffman(String tFilePath)
			throws IOException, CloneNotSupportedException, ClassNotFoundException {
		logger.info("huffman decoding - start");
		int err = 0;
		if (!Common.isFileExist(filePath)) {
			logger.info("the file to be encoded with Huffman:not found");
			return ErrorCode.NOT_DIRECTORY;
		}

		if (!Common.createFile(tFilePath)) {
			return ErrorCode.CREATE_FILE_FAIL;
		}

		File f = new File(filePath);
		FileInputStream fis = new FileInputStream(f);
		FileOutputStream fos = new FileOutputStream(tFilePath);

		// 得到霍夫曼树和各种参�?
		ObjectInputStream ois = new ObjectInputStream(fis);
		byteNode = (HuffmanNode[]) ois.readObject();
		midNode = (ArrayList<HuffmanNode>) ois.readObject();
		byte[] t = new byte[4];
		fis.read(t);
		long fullLen = (long) f.length() - (long) Common.fourByteToInt(t);
		long restLen = fullLen;

		// 解码
		BitOperatableData bodToRead = null;
		BitOperatableData lastTimeRemain = null;
		int nodeValue = nullNodeValue;
		byte[] b = new byte[Common.eachTimeByte];
		byte[] bToWrite = new byte[Common.eachTimeByte];
		int p = 0;
		while (restLen > fullLen % Common.eachTimeByte) {
			fis.read(b);
			restLen -= Common.eachTimeByte;
			BitOperatableData bodTemp = BitOperatableData.getInstance(b);
			if (lastTimeRemain != null) {
				bodToRead = BitOperatableData.getInstance(lastTimeRemain.data.length + bodTemp.data.length);
				bodToRead.plus(lastTimeRemain);
				bodToRead.plus(bodTemp);
			} else {
				bodToRead = bodTemp;
			}
			bodToRead.seek(0, 0);
			while ((nodeValue = this.huffmanDecode(bodToRead)) != nullNodeValue) {
				bToWrite[p++] = (byte) nodeValue;
				if (p == Common.eachTimeByte) {
					fos.write(bToWrite);
					p = 0;
				}
			}
			lastTimeRemain = bodToRead.read();
		}
		b = new byte[(int) restLen];
		fis.read(b);
		BitOperatableData bodTemp = BitOperatableData.getInstance(1);
		bodTemp.paddingDataToBOD(b);
		if (lastTimeRemain != null) {
			bodToRead = BitOperatableData.getInstance(lastTimeRemain.data.length + bodTemp.data.length);
			bodToRead.plus(lastTimeRemain);
			bodToRead.plus(bodTemp);
		} else {
			bodToRead = bodTemp;
		}
		bodToRead.seek(0, 0);
		while ((nodeValue = this.huffmanDecode(bodToRead)) != nullNodeValue) {
			bToWrite[p++] = (byte) nodeValue;
			if (p == Common.eachTimeByte) {
				fos.write(bToWrite);
				p = 0;
			}
		}
		if (p > 0) {
			fos.write(bToWrite, 0, p);
		}
		fis.close();
		fos.flush();
		fos.close();
		logger.info("huffman decoding - end");
		error(err);
		return err;
	}

	// 建立霍夫曼树
	private void makeHuffmanTree(File f) throws IOException {
		byte[] b = new byte[Common.eachTimeByte];
		long fullLen = f.length();
		long restLen = fullLen;
		FileInputStream fis = new FileInputStream(f);

		// 第一次遍历计算权�?
		while (restLen > fullLen % Common.eachTimeByte) {
			fis.read(b);
			restLen -= Common.eachTimeByte;
			for (int i = 0; i < Common.eachTimeByte; i++) {
				getNodeByByte(b[i]).weight++;
			}
		}
		b = new byte[(int) restLen];
		fis.read(b);
		for (int i = 0; i < restLen; i++) {
			getNodeByByte(b[i]).weight++;
		}
		fis.close();

		// 建立霍夫曼树
		ArrayList<HuffmanNode> sortList = new ArrayList<HuffmanNode>();
		for (int i = 0; i < byteNode.length - 1; i++) {
			if (byteNode[i].weight > 0)
				sortList.add(byteNode[i]);
		}
		firstSort(sortList);
		while (sortList.size() > 2) {
			makeSubtree(sortList);
		}
		sortList.get(0).parent = byteNode[256];
		sortList.get(1).parent = byteNode[256];
		byteNode[256].left = sortList.get(0);
		byteNode[256].right = sortList.get(1);
	}

	// 每次处理两个节点，建立子�?
	private void makeSubtree(ArrayList<HuffmanNode> hfm) {
		HuffmanNode tHfm = new HuffmanNode();
		tHfm.left = hfm.get(0);
		tHfm.right = hfm.get(1);
		tHfm.weight = hfm.get(0).weight + hfm.get(1).weight;
		hfm.get(0).parent = tHfm;
		hfm.get(1).parent = tHfm;
		midNode.add(tHfm);
		hfm.remove(0);
		hfm.set(0, tHfm);
		secondSort(hfm);
	}

	// 根据权重进行第一次排�?
	private void firstSort(ArrayList<HuffmanNode> hfm) {

		// 创建比较规则
//        Comparator<HuffmanNode> comparator = new Comparator<HuffmanNode>(){
//            public int compare(HuffmanNode h1, HuffmanNode h2) {
//                return (((HuffmanNode)h1).weight - ((HuffmanNode)h2).weight);
//            }
//        };

		Collections.sort(hfm, (h1, h2) -> h1.weight - h2.weight);
	}

	// 根据权重进行第二次排�?
	private void secondSort(ArrayList<HuffmanNode> hfm) {

		for (int i = 0; i < hfm.size() - 1; i++) {
			if (hfm.get(i).weight > hfm.get(i + 1).weight) {
				HuffmanNode t = hfm.get(i);
				hfm.set(i, hfm.get(i + 1));
				hfm.set(i + 1, t);
			} else
				break;
		}
	}

	// 将字节转化为编码
	private BitOperatableData byteToHuffmanCode(byte b) throws CloneNotSupportedException {
		BitOperatableData code = BitOperatableData.getInstance(256 / 8);
		Stack<Integer> s = new Stack<Integer>(256);
		HuffmanNode t = getNodeByByte(b);
		while (t.parent != null) {
			if (t == t.parent.left) {
				s.push(0);
			} else {
				s.push(1);
			}
			t = t.parent;
		}
//        if(s.getTop() > 100)
//            System.out.println(s.getTop());
		while (!s.isEmpty()) {
			if (s.pop().equals(0)) {
				code.writeBitZero();
			} else
				code.writeBitOne();
		}

		// 保存编码用于输出
		if (codingRes[(int) (b & 0xff)] == null) {
			codingRes[(int) (b & 0xff)] = (int) (b & 0xff) + ":";
			int flg;
			code.seek(0, 0);
			while ((flg = code.readBitOnPos()) != -1) {
				if (flg == 1) {
					codingRes[(int) (b & 0xff)] += 1;
				} else {
					codingRes[(int) (b & 0xff)] += 0;
				}
			}
			code.seek(0, 0);
			// System.out.println(codingRes[(int)(b & 0xff)]);
		}

		return code;
	}

	// bod数据的当前read位置�?始，读取霍夫曼编码，解码返回int�?
	private int huffmanDecode(BitOperatableData bod) {
		int flg = bod.readBitOnPos();
		HuffmanNode h = null;
		HuffmanNode r = null;
		int len = 0;
		if (flg == 0) {
			h = byteNode[256].left;
			len++;
		} else if (flg == 1) {
			h = byteNode[256].right;
			len++;
		} else if (flg == -1) {
			return nullNodeValue;
		}
		while (h != null) {
			flg = bod.readBitOnPos();
			if (flg == 0) {
				r = h;
				h = h.left;
				len++;
			} else if (flg == 1) {
				r = h;
				h = h.right;
				len++;
			} else if (flg == -1) {
				if (h.value != nullNodeValue) {
					return h.value;
				} else {
					bod.moveReadPoint(-len);
					return nullNodeValue;
				}
			}
		}
		bod.moveReadPoint(-1);
		return r.value;
	}
}
