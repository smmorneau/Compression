import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HuffTree {

	private Node root;
	private Map<Character, Integer> freqs;
	private ArrayList<Character> keys = new ArrayList<Character>();
	private Map<Character, ArrayList<Integer>> encodingTable;
	private Map<String, Character> decodingTable;
	private long size;
	private StringBuilder sb;

	// compression
	public HuffTree(Map<Character, Integer> freqs) {
		this.freqs = freqs;
		Object[] keyArray = freqs.keySet().toArray();
		for (Object o : keyArray) {
			keys.add((Character) o);
		}
	}

	// decompression
	public HuffTree() {
	}

	private class Node implements Comparable {
		public Character ch;
		public Integer freq;
		public Node left;
		public Node right;

		public Node() {
		}

		public Node(Character ch) {
			this.ch = ch;
		}

		public Node(Character ch, Integer freq) {
			this.ch = ch;
			this.freq = freq;
		}

		@Override
		public int compareTo(Object o) {

			Node n = (Node) o;

			// lowest to highest
			if (n.freq < this.freq) {
				return 1;
			} else if (n.freq > this.freq) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	/* Compression */

	public void buildCompressionTree() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (Character c : keys) {
			nodes.add(new Node(c, freqs.get(c)));
		}
		buildCompressionTree(nodes);
	}

	private void buildCompressionTree(ArrayList<Node> nodes) {
		if (keys.size() == 1) {
			int sum = nodes.get(0).freq;
			root = new Node(null, sum);
			root.left = new Node(nodes.get(0).ch, nodes.get(0).freq);
			root.right = new Node();
		} 
		
		if (nodes.size() > 1) {
			Collections.sort(nodes);
			int sum = nodes.get(0).freq + nodes.get(1).freq;
			root = new Node(null, sum);
			root.left = nodes.remove(0);
			root.right = nodes.remove(0);
			nodes.add(root);
			buildCompressionTree(nodes);
		}
		 

	}

	public Map<Character, ArrayList<Integer>> buildEncodingTable() {
		encodingTable = new HashMap<Character, ArrayList<Integer>>();
		buildEncodingTable(root, new ArrayList<Integer>());
		return encodingTable;
	}

	private void buildEncodingTable(Node tree, ArrayList<Integer> list) {
		if (tree.left == null && tree.right == null) {
			// 9 bits for every leaf
			size += 9;
			ArrayList<Integer> sublist = new ArrayList<Integer>();
			sublist.addAll(list);
			encodingTable.put(tree.ch, sublist);
		} else {
			// 1 bit for every internal node

			if (tree.left != null) {
				size += 1;
				ArrayList<Integer> sublist = new ArrayList<Integer>();
				sublist.addAll(list);
				sublist.add(0);
				buildEncodingTable(tree.left, sublist);
			}
			if (tree.right != null) {
				size += 1;
				ArrayList<Integer> sublist = new ArrayList<Integer>();
				sublist.addAll(list);
				sublist.add(1);
				buildEncodingTable(tree.right, sublist);
			}
		}
	}

	public void writeCompressed(String input, BinaryFile outputFile, int padding) {
		// write magic number
		outputFile.writeChar('H');
		outputFile.writeChar('F');
		// write padding count
		outputFile.writeChar((char)padding);
		// write tree
		sb = new StringBuilder();
		System.out.println("Serializing tree...");
		serializeTree(root, outputFile);
		String serialized = sb.toString();
		for (char c : serialized.toCharArray()) {
			if (c == '0') {
				outputFile.writeBit(false);
			} else if (c == '1') {
				outputFile.writeBit(true);
			} else {
				outputFile.writeChar(c);
			}
		}
		// write encoded text
		TextFile inputFile = new TextFile(input, 'r');
		System.out.println("Encoding text...");
		encodeText(inputFile, outputFile);
		if (padding > 0) {
			for (int i = 0; i < padding; i++) {
				outputFile.writeBit(false);
			}
		}

		inputFile.close();
	}

	private void serializeTree(Node tree, BinaryFile outputFile) {
		if (tree != null) {
			if (tree.left == null && tree.right == null) {
				// 0 = leaf
				sb.append(0);
				sb.append(tree.ch);
			} else {
				// 1 = internal node
				sb.append(1);
			}
			serializeTree(tree.left, outputFile);
			serializeTree(tree.right, outputFile);
		}
	}

	private void encodeText(TextFile inputFile, BinaryFile outputFile) {
		while (!inputFile.EndOfFile()) {
			// text character
			Character c = new Character(inputFile.readChar());
			// write out encoded character
			for (Integer i : encodingTable.get(c)) {
				if (i.equals(0)) {
					outputFile.writeBit(false);
				} else if (i.equals(1)) {
					outputFile.writeBit(true);
				} else {
					System.err
							.println("Characters should be encoded to 0s or 1s.");
				}

			}
		}
	}

	/* Decompression */

	public void buildDecompressionTree(BinaryFile binaryFile) {
		// root is internal node
		boolean one = binaryFile.readBit();
		if (one) {
			root = new Node();
		}
		buildDecompressionTree(binaryFile, root);
	}

	private void buildDecompressionTree(BinaryFile binaryFile, Node tree) {
		if (!binaryFile.EndOfFile()) {
			boolean bit = binaryFile.readBit();
			// 1 = internal node
			if (bit) {
				tree.left = new Node();
				buildDecompressionTree(binaryFile, tree.left);
			} else {
				// 0 = leaf
				char c = binaryFile.readChar();
				tree.left = new Node(c);
			}

			bit = binaryFile.readBit();
			// 1 = internal node
			if (bit) {
				tree.right = new Node();
				buildDecompressionTree(binaryFile, tree.right);
			} else {
				// 0 = leaf
				char c = binaryFile.readChar();
				tree.right = new Node(c);
			}
		}
	}

	public Map<String, Character> buildDecodingTable() {
		decodingTable = new HashMap<String, Character>();
		buildDecodingTable(root, new ArrayList<Integer>());
		return decodingTable;
	}

	public void buildDecodingTable(Node tree, ArrayList<Integer> list) {
		if (tree.left == null && tree.right == null) {
			String code = "";
			for (int i : list)
				code += i;

			decodingTable.put(code, tree.ch);
		} else {
			if (tree.left != null) {
				ArrayList<Integer> sublist = new ArrayList<Integer>();
				sublist.addAll(list);
				sublist.add(0);
				buildDecodingTable(tree.left, sublist);
			}
			if (tree.right != null) {
				ArrayList<Integer> sublist = new ArrayList<Integer>();
				sublist.addAll(list);
				sublist.add(1);
				buildDecodingTable(tree.right, sublist);
			}
		}
	}

	public void decodeText(BinaryFile inputFile, TextFile outputFile, int padding) {

		freqs = new HashMap<Character, Integer>();

		while (!inputFile.EndOfPaddedFile(padding)) {
			String code = "";

			while (decodingTable.get(code) == null) {
				// get bit
				boolean b = inputFile.readBit();
				if (b) {
					code +=1;
				} else {
					code += 0;
				}
			}
			assert decodingTable.get(code) != null;
			Character c = decodingTable.get(code);

			Integer currFreq = freqs.get(c);
			if (currFreq == null) {
				currFreq = 0;
			}
			currFreq++;
			freqs.put(c, currFreq);

			outputFile.writeChar(c);
		}
	}

	public void print() {
		print(root);
	}

	private void print(Node tree) {
		if (tree != null) {
			System.out.println(tree.ch + ", " + tree.freq);
			System.out.println("down left");
			print(tree.left);
			System.out.println("back up to " + tree.freq + " from left");
			System.out.println("down-right");
			print(tree.right);
			System.out.println("back up to " + tree.freq + " from right");
		}
	}

	public Map<Character, Integer> getFreqs() {
		return freqs;
	}
	
	public void printTree(int offset) {
		printTree(root, offset);
	}
	
	private void printTree(Node tree, int offset) {
		if (tree != null) {
			for (int i = 0; i < offset; i++) {
				System.out.print("\t");
			}
			char c = ' ';
			if(tree.ch != null) {
				c = tree.ch;
				if (c == '\n')
					c = ' ';
			}
			
			System.out.println("(" + c + " : " + tree.freq + ")");
			printTree(tree.left, offset + 1);
			printTree(tree.right, offset-1);
		}
	}

	public long getSize() {
		return size;
	}
	
	public int height() {
		return height(root);
	}
	
	private int height(Node tree) {
		if (tree == null)
			return 0;
		return Math.max((1 + height(tree.left)), height(tree.right));
	}
}
