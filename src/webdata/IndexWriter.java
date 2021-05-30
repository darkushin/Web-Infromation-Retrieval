package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Date;


public class IndexWriter {
	private HashMap<String, Integer> tokenDict;  // token: tokenId
	private ArrayList<String> invertedTokenDict;  // tokenId: token
	private TreeMap<String, ArrayList<Integer>> productIds;
	private TreeMap<Integer, ArrayList<String>> reviewIds;

	private int[][] tokenBuffer = new int[TOKEN_BUFFER_SIZE][2];
//	private ArrayList<ArrayList<Integer>> tokenBuffer = new ArrayList<ArrayList<Integer>>();
	; // Array of termID, docID pairs. Regular array to sort in-place
	private int tokenBufferPointer;
	private int tokenFilesNumber = 0;
	private String dir;

	private static final String PRODUCT_INDEX_FILE = "product_index.txt";
	private static final String REVIEW_INDEX_FILE = "review_index.txt";
	private static final String TOKEN_INDEX_FILE = "token_index.txt";
	private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";
	private static final int PAIRS_IN_BLOCK = 1000;
	private static final int M = 25000;
//	private static final int M = 25;
	private static final int TOKEN_BUFFER_SIZE = PAIRS_IN_BLOCK * (M - 1);  // Number of -pairs- in memory. Should be PAIRS_IN_BLOCK * (M-1) or something.

	int NUM_REVIEWS = 10000000;  // todo: remove before submission!


	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void write(String inputFile, String dir) {
		this.dir = dir;
		createDir();
		testParser(inputFile);
		createDicts(inputFile);
		long startTime = new Date().getTime();
		createProductIndex();
		long endTime = new Date().getTime();
		System.out.println("Create Product Index: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");

		startTime = new Date().getTime();
		createReviewIndex();
		endTime = new Date().getTime();
		System.out.println("Create Review Index: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");

		productIds = null;
		reviewIds = null; // Clears memory?

		startTime = new Date().getTime();
		createTokenIndex();
		endTime = new Date().getTime();
		System.out.println("Create Token Index: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");
		File mergedDataFile = new File(dir + "/1");
		mergedDataFile.delete();

	}

	public void testParser(String inputFile){
		DataLoader dataLoader = null;
		DataParser dataParser = new DataParser();
		try {
			dataLoader = new DataLoader(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error occurred while reading the reviews input file.");
			System.exit(1);
		}
		int i=1;
		int readTokens = 0;
		for (String s: dataLoader) {
			DataParser.Review review = dataParser.parseReview(s);
			int length = addReviewText(review.getText(), i);
			readTokens += length;
			i++;
			if (i > NUM_REVIEWS) { break;}
			if (i % 100000 == 0) {
				System.out.println("Read " + i + " reviews and " + readTokens + " tokens");
			}
		}
		System.out.println("TOTAL: " + i + " reviews and " + readTokens + " tokens");
		System.out.println("Done Reading");

	}


	/**
	 * Delete all index files by removing the given directory
	 */
	public void removeIndex(String dir) {
		File dirToRemove = new File(dir);
		File[] contents = dirToRemove.listFiles();
		if (contents != null) {
			for (File file : contents) {
				file.delete();
			}
		}
		dirToRemove.delete();
	}

	/**
	 * Create a new directory in the path specified in the instance initialization.
	 */
	private void createDir(){
		Path path = Path.of(this.dir);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create temporary dictionaries that will store all information, before saving the indices to the disk.
	 * @param inputFile the file containing all reviews
	 */
	private void createDicts(String inputFile){
		productIds = new TreeMap<>();
		tokenDict = new HashMap<>();
		reviewIds = new TreeMap<>();
		invertedTokenDict = new ArrayList<>();

		// todo: remove the directory creation from here!
		try {
			Files.createDirectories(Path.of(this.dir + ExternalMergeSort.folderName + "1"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.clearBuffer();

		DataLoader dataLoader = null;
		DataParser dataParser = new DataParser();
		try {
			dataLoader = new DataLoader(inputFile);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error occurred while reading the reviews input file.");
			System.exit(1);
		}
		long startTime = new Date().getTime();
		int i=1;
		int readTokens = 0;
		for (String s: dataLoader){
			DataParser.Review review = dataParser.parseReview(s);
			addProductId(review.getProductId(), i);
			int length = addReviewText(review.getText(), i);
			addReviewId(review, i, length);
			readTokens += length;
			i++;

			// todo: remove this part - is used only to test with specific number of reviews
			if (i > NUM_REVIEWS) { break;}
			if (i % 100000 == 0) {
				System.out.println("Read " + i + " reviews and " + readTokens + " tokens");
			}
		}
		System.out.println("Done Reading");

		this.sortBuffer();
		try {
			this.saveBuffer();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		long endTime = new Date().getTime();
		System.out.println("Data Loading And Saving Time: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");

		this.tokenBuffer = null;  // free the token buffer space
		Comparator<Integer> cmp = Comparator.comparing(a -> invertedTokenDict.get(a));

		startTime = new Date().getTime();
		ExternalMergeSort ems = new ExternalMergeSort(cmp, tokenFilesNumber, PAIRS_IN_BLOCK, dir);
		System.out.println("Number of files before merging: " + tokenFilesNumber);
		ems.sort();
		endTime = new Date().getTime();
		System.out.println("Merging Time: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");
	}

	// TODO: for debugging. Remove this later
	private boolean isFileSorted(String fileName, Comparator<Integer> cmp) {
		FileInputStream fileIn = null;
		ObjectInputStream ois = null;
		long tot = 0;
		try {
			fileIn = new FileInputStream(fileName);
			ois = new ObjectInputStream(fileIn);
			int prev = ois.readInt();
			int prevDocId = ois.readInt();
			tot++;
			while (true) {
				int cur = ois.readInt();
				int docId = ois.readInt();
				if (cmp.compare(prev, cur) > 0) {
					System.out.println("Terms not sorted. Occured in " + tot);
				} else if ((cmp.compare(prev, cur) == 0) && (prevDocId > docId)) {
					System.out.println("DocIds not sorted. Occured in " + tot);
				}
				prev = cur;
				prevDocId = docId;
				tot++;
			}
		}  catch (EOFException ex) {
			System.out.println("Read " + tot + " pairs.");
			try {
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		return true;
	}
	private long countNumsInFile(String fileName) {
		FileInputStream fileIn;
		ObjectInputStream ois = null;
		long tot = 0;
		try {
			fileIn = new FileInputStream(fileName);
			ois = new ObjectInputStream(fileIn);
			while (true) {
				ois.readInt();
				tot++;
			}
		}  catch (EOFException ex) {
			try {
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			return tot;
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		return tot;
	}


	/**
	 * Split the given text of the i-th review into tokens and add them to the tokens dictionary.
	 * @param reviewText the text of the review that should be added.
	 * @param reviewIndex the number of the given review.
	 * @return the number of tokens in the given review text.
	 */
	private int  addReviewText(String reviewText, int reviewIndex){
		String[] tokens = reviewText.split("[^a-zA-Z0-9]");  // split to alphanumeric tokens
		int reviewLength = 0;
		for (String token: tokens){
			if (!token.matches("[a-zA-Z0-9]+")){
				continue;
			}
			reviewLength += 1;
			token = token.toLowerCase();
			int termId = tokenDict.computeIfAbsent(token, k -> tokenDict.size());
			if (termId == invertedTokenDict.size()) { invertedTokenDict.add(token);}  // if a new token was added, add it also to the invertedTokenDict
			tokenBuffer[tokenBufferPointer][0] = termId;
			tokenBuffer[tokenBufferPointer][1] = reviewIndex;
			tokenBufferPointer++;
//			tokenBuffer.add(new ArrayList<>(Arrays.asList(termId, reviewIndex)));
			if (tokenBufferPointer == TOKEN_BUFFER_SIZE){
//			if (tokenBuffer.size() == TOKEN_BUFFER_SIZE){
				this.sortBuffer();
				try {
					this.saveBuffer();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				this.clearBuffer();
//				this.tokenBuffer.clear();

			}
		}
		return reviewLength;
	}

	private void sortBuffer() {
		System.out.println("In sort");
		Arrays.sort(tokenBuffer,0, tokenBufferPointer, Comparator.comparing(a -> invertedTokenDict.get(a[0])));
//		tokenBuffer.sort(Comparator.comparing(a -> invertedTokenDict.get(a.get(0))));
	}

	private void saveBuffer() throws IOException {
		this.tokenFilesNumber++;
		ObjectOutputStream tokenBufferWriter = new ObjectOutputStream(new FileOutputStream(dir + ExternalMergeSort.folderName + "1/" + tokenFilesNumber));
		for (int i = 0; i < tokenBufferPointer; i++) {
			tokenBufferWriter.writeInt(tokenBuffer[i][0]);
			tokenBufferWriter.writeInt(tokenBuffer[i][1]);
		}
//		for (int i = 0; i < tokenBuffer.size(); i++) {
//			tokenBufferWriter.writeInt(tokenBuffer.get(i).get(0));
//			tokenBufferWriter.writeInt(tokenBuffer.get(i).get(1));
//		}
		tokenBufferWriter.close();
	}

	private void clearBuffer() {
		tokenBufferPointer = 0;
	}

	/**
	 * Update the productId dictionary by adding to it the given product. If the product already exists, it adds review
	 * id to the reviews that are matching to this product.
	 */
	private void addProductId(String productId, int reviewId) {
		if (!productIds.containsKey(productId)) {
			productIds.put(productId, new ArrayList<>(Arrays.asList(reviewId, 0)));
		}
		else {
			ArrayList<Integer> product = productIds.get(productId);
			product.set(1, product.get(1) + 1);
		}
	}

	/**
	 * Adds all the information that is relevant to the given reviewId to the reviewIds dictionary.
	 */
	private void addReviewId(DataParser.Review review, int reviewId, int length) {
		reviewIds.put(reviewId, new ArrayList<>());
		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
		reviewIds.get(reviewId).add(review.getProductId());
		reviewIds.get(reviewId).add(review.getScore());
		reviewIds.get(reviewId).add(review.getHelpfulness());
		reviewIds.get(reviewId).add(String.valueOf(length));
	}

	/**
	 * Creates and saves to the disk the product index, i.e. all the information that is related to products.
	 */
	private void createProductIndex() {
		ArrayList<String> ids = new ArrayList<>(productIds.keySet());
		ArrayList<ArrayList<Integer>> vals = new ArrayList<>(productIds.values());
		int k = 8;
		KFront kf = new KFront();
		kf.createKFront(k, ids);
		for (int i = 0; i < vals.size(); i++) {
			kf.getTable().get(i).addAll(vals.get(i));
		}

		ProductIndex pIndex = new ProductIndex(k);
		pIndex.insertData(kf.getTable(), kf.getConcatString());
		saveToDir(PRODUCT_INDEX_FILE, pIndex);
	}

	/**
	 * Creates the index file for the tokens in the collection.
	 * The index is created using the k-1-in-k front coding method.
	 */
	private void createTokenIndex(){
		ArrayList<String> tokens = new ArrayList<>(tokenDict.keySet());
		long startTime = new Date().getTime();
		Collections.sort(tokens);
		long endTime = new Date().getTime();
		System.out.println("Token Index After Sort: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");

		tokenDict = null;
		startTime = new Date().getTime();

		int k = 256;
		KFront kf = new KFront(true);
		kf.createKFront(k, tokens);
		endTime = new Date().getTime();
		System.out.println("Token Index After KFront: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");

		startTime = new Date().getTime();
		TokensIndex tIdx = new TokensIndex(k, this.dir);
		tIdx.insertData(kf.getTable(), kf.getConcatString(), dir + "/1");
		endTime = new Date().getTime();
		System.out.println("Token Index Inserting Data: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");

		saveToDir(TOKEN_INDEX_FILE, tIdx);
	}

	/**
	 * Creates and saves to the disk the review index which hold all information related to reviews.
	 */
	private void createReviewIndex() {
		// Revise the review dictionary to the correct structure & change productIDs to product index
		ArrayList<List<Integer>> dictValues = new ArrayList<>();
		HashMap<String, Integer> productDict = new HashMap<>(productIds.size());
		int i = 0;
		for (String productId: productIds.keySet()){
			productDict.put(productId, i);
			i++;
		}
		for (int review : reviewIds.keySet()) {
			ArrayList<String> vals = reviewIds.get(review);
			ArrayList<Integer> new_vals = new ArrayList<>(List.of(0, 0, 0, 0, 0));
			new_vals.set(ReviewIndex.PRODUCTID_INDEX, productDict.get(vals.get(0)));
			String[] helpf = vals.get(2).split("/");
			new_vals.set(ReviewIndex.HELPFNUM_INDEX, Integer.parseInt(helpf[0]));
			new_vals.set(ReviewIndex.HELPFDNOM_INDEX, Integer.parseInt(helpf[1]));
			new_vals.set(ReviewIndex.REVIEWLENGTH_INDEX,  Integer.parseInt(vals.get(3)));
			new_vals.set(ReviewIndex.SCORE_INDEX,  (int) Float.parseFloat(vals.get(1)));
			dictValues.add(new_vals);
		}
		ReviewIndex rIndex = new ReviewIndex();
		rIndex.insertData(dictValues);

		saveToDir(REVIEW_INDEX_FILE, rIndex);
	}

	/**
	 * Save the given object to disk under the given name. The file is saved to the dir that was passed to the
	 * SlowWrite() function.
	 */
	private void saveToDir(String name, Object obj) {
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(this.dir + "/" + name);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			System.out.println("Error occurred while saving the index file: " + name);
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		String inputFile = "/Users/darkushin/Downloads/Movies_&_TV.txt";
//		String inputFile = "./1000.txt";
		String dir = "./Data_Index";
		long startTime = new Date().getTime();
		IndexWriter indexWriter = new IndexWriter();
		indexWriter.write(inputFile, dir);
		long endTime = new Date().getTime();
		System.out.println("Indexing Time: " + (endTime-startTime) + " Milliseconds = " + ((endTime - startTime) / 1000) + " Seconds");
	}
}