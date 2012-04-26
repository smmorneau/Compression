import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Huffman {

	private static String input;
	private static String output;

	private static Map<Character, Integer> freq;
	private static Map<Character, ArrayList<Integer>> encodingTable;
	private static Map<String, Character> decodingTable;
	
	private static HuffTree tree;

	/**
	 * @param command
	 *            line args
	 */
	public static void main(String[] args) {

		boolean compress = true; // false = uncompress
		boolean force = false;
		boolean verbose = false;

		if (args.length < 3 || args.length > 5) {
			System.err.println("Missing command line arguments.");
			System.exit(-1);
		}

		if (args[0].equals("-c")) {
			compress = true;
		} else if (args[0].equals("-u")) {
			compress = false;
		} else {
			System.err.println("First argument must be '-c' or '-u'.");
			System.exit(-1);
		}

		output = args[args.length - 1];
		input = args[args.length - 2];

		// remove existing output file
		File f = new File(output);
		f.delete();

		if (output == null || output.startsWith("-") || input == null
				|| input.startsWith("-")) {
			System.err.println("Incorrect input or output file.");
			System.exit(-1);
		}

		if (args[1].equals("-v") || args[2].equals("-v")) {
			verbose = true;
		}
		if (args[1].equals("-f") || args[2].equals("-f")) {
			force = true;
		}

		boolean decomp = true;
		
		if (compress) {
			compress(force);
		} else {
			decomp = decompress();
		}

		if (verbose && decomp) {
			System.out.println();
			printVerbose(compress);
		}

	}

	public static void compress(boolean force) {
		TextFile textFile = new TextFile(input, 'r');

		/* Calculate Character Frequencies */

		freq = new HashMap<Character, Integer>();

		System.out.println("Building Huffman tree...");

		while (!textFile.EndOfFile()) {
			Character c = new Character(textFile.readChar());
			Integer currFreq = freq.get(c);
			if (currFreq == null) {
				currFreq = new Integer(1);
			} else {
				int newFreq = currFreq.intValue() + 1;
				currFreq = new Integer(newFreq);
			}
			freq.put(c, currFreq);
		}

		textFile.close();

		MapValueComparator comp = new MapValueComparator(freq);
		Map<Character, Integer> sortedFreq = new TreeMap<Character, Integer>(
				comp);
		sortedFreq.putAll(freq);

		long numChars = 0;
		for (Character c : sortedFreq.keySet()) {
			long f = sortedFreq.get(c);
			numChars += f;
		}

		// Size of uncompressed file (bits) = (# of chars in input file) * 8
		long sizeU = numChars * 8;

		/* Build Huffman Tree */
		tree = new HuffTree(sortedFreq);
		tree.buildCompressionTree();

		/* Build Lookup Table */
		System.out.println("Building lookup table...");
		encodingTable = tree.buildEncodingTable();

		// Size of the compressed file (bits)
		long sizeC = 0;

		/* Check File Size */

		for (Character c : freq.keySet()) {
			// (frequency of c) * (size of the encoding for c)
			// Size of tree (1 bit per internal node, 9 bits per leaf)
			// 16 bits for magic number & 32 bits for header info
			sizeC += (freq.get(c) * encodingTable.get(c).size());
		}
//		System.out.println(sizeC + " bits characters");

		// 8 more for padding count
		sizeC += tree.getSize() + 48 + 8;

		// System.out.println("******************");
//		System.out.println(sizeU + " bits (Uncompressed)");
//		System.out.println(sizeC + " bits (Compressed before padding)");

		// make file size a multiple of 8
		int padding = 0;
		if (sizeC % 8 != 0) {
			padding = 8 - (int) sizeC % 8;
			sizeC += padding;
		}

//		System.out.println(sizeC + " bits (Compressed) (padding = " + padding
//				+ ")");

		if ((sizeC < sizeU) || force) {
			BinaryFile binaryFile = new BinaryFile(output, 'w');
			if(sizeC >= sizeU) {
				System.out.println("Forced compression.");
			}
			System.out.println("Writing compressed file...");
			tree.writeCompressed(input, binaryFile, padding);
			binaryFile.close();
		} else {
			System.out
					.println("The file cannot be compressed to a smaller size.");
		}

	}

	public static boolean decompress() {

		BinaryFile binaryFile = new BinaryFile(input, 'r');
		char h = binaryFile.readChar();
		char f = binaryFile.readChar();

		if (h != 'H' || f != 'F') {
			System.out
					.println("This is not a compressed file. Cannot decopress.");
			binaryFile.close();
			return false;
		}

		int padding = (int) binaryFile.readChar();
		
		tree = new HuffTree();

		/* Build Lookup Table */

		System.out.println("Building decompression tree...");
		tree.buildDecompressionTree(binaryFile);
		System.out.println("Building decoding table...");
		decodingTable = tree.buildDecodingTable();

		TextFile textFile = new TextFile(output, 'w');
		System.out.println("Decoding text...");
		tree.decodeText(binaryFile, textFile, padding);
		freq = tree.getFreqs();

		binaryFile.close();
		
		return true;
		
	}

	public static void printVerbose(boolean compress) {
		/* Print the frequency of each character in the input file */

		if (compress) {
			System.out.println("ASCII Char\tFrequency");
			System.out.println("-------------------------");
			for (Character c : freq.keySet()) {
				System.out.println((int) c + "\t\t" + freq.get(c));
			}
			System.out.println("\n");
		} else {
			System.out.println("ASCII Char\tFrequency");
			System.out.println("-------------------------");
			for (Character c : freq.keySet()) {
				System.out.println((int) c + "\t\t" + freq.get(c));
			}
			System.out.println("\n");
		}

		/* Print the Huffman tree */
		System.out.println("Huffman tree");
		System.out.println("------------");
		tree.printTree((tree.height() / 2));
		System.out.println("\n");

		/* Print the Huffman codes for each character that has a code */

		if (compress) {
			System.out.println("ASCII Char\tHuffman Code");
			System.out.println("----------------------------");
			
			for (Character c : encodingTable.keySet()) {
				if(c != null) {
					// dummy element for 1 type of element in file
					System.out.print((int) c + "\t\t");
					ArrayList<Integer> codes = encodingTable.get(c);
					for (Integer i : codes) {
						System.out.print(i);
					}
					System.out.println();
				}
			}
		} else {
			System.out.println("Huffman Code\tASCII Char");
			System.out.println("-------------------------");
			for (String code : decodingTable.keySet()) {
				System.out.print(code);
				System.out.print("\t\t" + (int) (decodingTable.get(code)));
				System.out.println();
			}
		}

	}

}
