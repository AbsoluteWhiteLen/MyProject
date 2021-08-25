package pack_and_encrypt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFileChooser;

public class Main extends BaseClass {

	/**
	 * @param args
	 * @throws IOException
	 * @throws CloneNotSupportedException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException, CloneNotSupportedException, ClassNotFoundException {

		// Encrypt eee = Encrypt.getInstance("d:/eeee", "2942586");
		// eee.doAES();

		// Encrypt e1 = Encrypt.getInstance("d:/eeee.enc", "111111111");
		// e1.invAES();

		// ArrayList<File> c = Common.findFiles("d:/test",".dll");

//        KMP k = KMP.getInstance();
//        byte[] b = {'a','b','a','b','c','a','b','c','d','e','f'};
//        byte[] c = {'a','b','c','d'};
//        int p = k.findFirstIndex(b, c, 0);
		String dir = "d:/test/";
		String cFileName = "Hydrangeas.jpg";
		LZ77 ll = LZ77.getInstance(dir + cFileName);
		ll.doLZ77();

		Huffman h = Huffman.getInstance(dir + cFileName + LZ77.extension);
		h.doStaticHuffman();

		Huffman hh = Huffman.getInstance(dir + cFileName + LZ77.extension + Huffman.extension);
		hh.invStaticHuffman(dir + cFileName + "(1)" + LZ77.extension);

		LZ77 lll = LZ77.getInstance(dir + cFileName + "(1)" + LZ77.extension);
		lll.invLZ77(dir + cFileName + "(1)");

		while (true) {
			String packedFilePath, extractDir = null, fileName;
			int packOrExtract = 0;
			int allOrOne = 0;
			int encOrNot = 0;
			String psw;
			PackFile packFile;
			ExtractFile extractFile;
			String[] filter;

			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);

			ArrayList<File> fileList = new ArrayList<File>();

			System.out.println("You want to pack or extract? 1:pack 2:extract\n" + "any other key to exit");
			packOrExtract = Integer.valueOf(in.nextLine());
			if (packOrExtract != 1 && packOrExtract != 2) {
				System.out.println("Exit!");
				break;
			}

			while (true) {
				System.out.println("Please input the path of packed file. egg(d:/text/ooxx.ss)");
				packedFilePath = in.nextLine();
				if (Common.isPath(packedFilePath))
					break;
				else
					System.out.println("Input error!");
			}

			switch (packOrExtract) {
			case 1:

				System.out.println("Please select the files or directories which are to get packed.");
				JFileChooser fc = new JFileChooser();
				fc.setMultiSelectionEnabled(true);
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				while (true) {
					fc.showOpenDialog(null);
					File[] f = fc.getSelectedFiles();
					if (f != null) {
						System.out.println("The files or directories you have selected are shown as follows:");
						for (int i = 0; i < f.length; i++) {
							if (f[i].exists()) {
								System.out.println(f[i].getPath());
								fileList.add(f[i]);
							}
						}
						break;
					}
				}

				while (true) {
					System.out.println(
							"Please input the kind of file you want to pack,separate with space. egg:.txt .png .mp3\n"
									+ "If you don't want to filt the files,input 0");
					String fInput = in.nextLine();
					if (fInput.equals("0")) {
						System.out.println("It will not filt the files.");
						filter = null;
						break;
					}
					if (Common.isFileFilter(fInput)) {
						filter = fInput.split("\\s+");
						break;
					}
					System.out.println("Input error!");
				}

				long mili0 = System.currentTimeMillis();
				int len = fileList.size();
				for (int i = 0; i < len; i++) {
					File f = fileList.get(i);
					if (f.isDirectory()) {
						ArrayList<File> ft = Common.findFiles(f.getPath(), filter);
						fileList.remove(i);
						fileList.addAll(ft);
						len--;
						i--;
					} else {
						if (!Common.isFitFilter(f, filter)) {
							fileList.remove(i);
							len--;
							i--;
						}
					}
				}
				long mili1 = System.currentTimeMillis();

				System.out.println("The file list to be packed are shown as follows:");
				for (int i = 0; i < fileList.size(); i++) {
					System.out.println(fileList.get(i) + "\t\t\t" + fileList.get(i).length() / 1024 + "KB");
				}
				System.out.println("number of file: " + fileList.size());

				packFile = PackFile.getInstance(fileList, packedFilePath);

				long mili2 = System.currentTimeMillis();
				if (packFile.packFileList() >= 0)
					System.out.println("success!");
				else {
					System.out.println("fail!");
					break;
				}
				long mili3 = System.currentTimeMillis();
				System.out.println("cost time of finding files: " + (mili1 - mili0));
				System.out.println("cost time of packing files: " + (mili3 - mili2));

				System.out.println("Do you want to encrypt the packed file? 1:yes 2:no");
				encOrNot = Integer.valueOf(in.nextLine());
				if (encOrNot == 1) {
					while (true) {
						System.out.println("Please input the password");
						psw = in.nextLine();
						if (Common.isPsw(psw))
							break;
						System.out.println("Input error!");
					}
					Encrypt e = Encrypt.getInstance(packedFilePath, psw);
					long mili4 = System.currentTimeMillis();
					if (e.doAES() >= 0)
						System.out.println("success!");
					else {
						System.out.println("fail!");
						break;
					}
					long mili5 = System.currentTimeMillis();
					System.out.println("cost time of encrypting the packed file: " + (mili5 - mili4));
				}
				break;
			case 2:
				while (true) {
					System.out.println("You want to extract all the files or only one file? 1:all 2:one");
					allOrOne = Integer.valueOf(in.nextLine());
					if (allOrOne == 1 || allOrOne == 2)
						break;
					System.out.println("Input error!");
				}

				while (true) {
					System.out.println("Please input the path of directory to extract to. egg(d:/text/)");
					extractDir = in.nextLine();
					if (Common.isPath(extractDir))
						break;
					System.out.println("Input error!");
				}

			default:
			}

			switch (allOrOne) {
			case 1:
				extractFile = ExtractFile.getInstance(packedFilePath, extractDir);
				long mili0 = System.currentTimeMillis();
				if (extractFile.extractAllFile() >= 0)
					System.out.println("success!");
				else {
					System.out.println("fail!");
					break;
				}
				long mili1 = System.currentTimeMillis();
				System.out.println("cost time of extracting file: " + (mili1 - mili0));
				break;

			case 2:
				extractFile = ExtractFile.getInstance(packedFilePath, extractDir);
				if (extractFile.seeAllPackedFileName() >= 0)
					System.out.println("The files in the packed file are as shown above");
				else
					return;

				System.out.println("Please input the name of the file to be extracted. egg(text.txt)");
				fileName = in.nextLine();

				mili0 = System.currentTimeMillis();
				if (extractFile.extractSingleFile(fileName) >= 0)
					System.out.println("success!");
				else {
					System.out.println("fail!");
					break;
				}
				mili1 = System.currentTimeMillis();
				System.out.println("cost time of extracting file: " + (mili1 - mili0));
				break;

			default:
			}
		}
	}
}
