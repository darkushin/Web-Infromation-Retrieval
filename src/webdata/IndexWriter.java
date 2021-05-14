package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IndexWriter {
	private HashMap<String, ArrayList<Integer>> tokenDict;  // token: tokenId
	private ArrayList<String> invertedTokenDict;  // tokenId: token
	private TreeMap<String, ArrayList<Integer>> productIds;
	private TreeMap<Integer, ArrayList<String>> reviewIds;

	private int[][] tokenBuffer; // Array of termID, docID pairs. Regular array to sort in-place
	private int tokenBufferPointer;
	private int tokenFilesNumber = 0;

	private String dir;

	private static final String PRODUCT_INDEX_FILE = "product_index.txt";
	private static final String REVIEW_INDEX_FILE = "review_index.txt";
	private static final String TOKEN_INDEX_FILE = "token_index.txt";
	private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";
	private static final int PAIRS_IN_BLOCK = 1000;
	private static final int TOKEN_BUFFER_SIZE = 100;



	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void write(String inputFile, String dir) {
		this.dir = dir;
		createDir();
		createDicts(inputFile);
//		createProductIndex();
		createTokenIndex();
//		createReviewIndex();
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
			Files.createDirectories(Path.of(dir + "/iteration_1"));
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
		int i=0;
		for (String s: dataLoader){
			DataParser.Review review = dataParser.parseReview(s);
			addProductId(review.getProductId(), i + 1);
			int length = addReviewText(review.getText(), i + 1);
//			addReviewId(review, i, length);
		}
		this.saveBuffer();

		// todo: merge sort all files - maybe move to a new function
		Comparator<Integer> cmp = Comparator.comparing(a -> invertedTokenDict.get(a));
		ExternalMergeSort externalMergeSort = new ExternalMergeSort(cmp, tokenFilesNumber, PAIRS_IN_BLOCK, dir);
		externalMergeSort.sort();

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
			ArrayList<Integer> termIdArr = tokenDict.computeIfAbsent(token, k -> new ArrayList<Integer>(tokenDict.size()));
			int termId = termIdArr.get(0);
			if (termId == invertedTokenDict.size()) { invertedTokenDict.add(token);}  // if a new token was added, add it also to the invertedTokenDict
			tokenBuffer[tokenBufferPointer][0] = termId;
			tokenBuffer[tokenBufferPointer][1] = reviewIndex;
			tokenBufferPointer++;
			if (tokenBufferPointer == TOKEN_BUFFER_SIZE){
				this.sortBuffer();
				this.saveBuffer();
				this.clearBuffer();
			}
		}
		return reviewLength;
	}

	private void sortBuffer() {
		Arrays.sort(tokenBuffer, Comparator.comparing(a -> invertedTokenDict.get(a[0])));
	}

	private void saveBuffer() {
		ObjectOutputStream tokenBufferWriter = null;
		this.tokenFilesNumber++;
		try {
			tokenBufferWriter = new ObjectOutputStream(new FileOutputStream(dir + "/iteration_1/" + tokenFilesNumber));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		for (int i = 0; i < tokenBufferPointer; i++) {
			try {
				tokenBufferWriter.writeInt(tokenBuffer[i][0]);
				tokenBufferWriter.writeInt(tokenBuffer[i][1]);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private void clearBuffer() {
		tokenBuffer = new int[TOKEN_BUFFER_SIZE][2];
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
//	private void addReviewId(DataParser.Review review, int reviewId, int length) {
//		reviewIds.put(reviewId, new ArrayList<>());
//		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
//		for (String field : DataParser.INTEREST_FIELDS) {
//			if (field.equals("text")) { continue; }
//			reviewIds.get(reviewId).add(review.get(field));
//		}
//		reviewIds.get(reviewId).add(String.valueOf(length));
//	}

	/**
	 * Creates and saves to the disk the product index, i.e. all the information that is related to products.
	 */
	private void createProductIndex() {
		LinkedList<String> ids = new LinkedList<>(productIds.keySet());
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
		LinkedList<String> tokens = new LinkedList<>(tokenDict.keySet());
		this.prepareTokenValues();

		ArrayList<ArrayList<Integer>> vals = new ArrayList<>(tokenDict.values());
		int k = 8;

		KFront kf = new KFront(true);
		kf.createKFront(k, tokens);

		TokensIndex tIdx = new TokensIndex(k, this.dir);
		tIdx.insertData(kf.getTable(), vals, kf.getConcatString());

		saveToDir(TOKEN_INDEX_FILE, tIdx);
	}

	/**
	 * Creates and saves to the disk the review index which hold all information related to reviews.
	 */
	private void createReviewIndex() {
		// Revise the review dictionary to the correct structure & change productIDs to product index
		LinkedList<List<Integer>> dictValues = new LinkedList<>();
		for (int review : reviewIds.keySet()) {
			ArrayList<String> vals = reviewIds.get(review);
			ArrayList<Integer> new_vals = new ArrayList<>(List.of(0, 0, 0, 0, 0));
			new_vals.set(ReviewIndex.PRODUCTID_INDEX, productIds.headMap(vals.get(0)).size());
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

	/**
	 * Read the termID-docID file, and convert all the appearances to the format of token:[doc1-#appearances, doc2-#appearance]
	 * this way, the same code as in ex1 can be used to create the token index.
	 */
	private void prepareTokenValues(){
		// todo: figure out how to get the file name
		String fileName = "bla";
		FileInputStream fileIn = null;
		try {
			fileIn = new FileInputStream(fileName);
			ObjectInputStream file = new ObjectInputStream(fileIn);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int previousTokenId = 0;
		int previousDocId = 0;

		// while we didn't reach EOF, read two integers at a time - termID and docID.
		// for every such pair, check if the termID is the same as the termID of the previous:
		// If not - find the token matching to the termID (using invertedTermId dict) and add the list created here to the tokenDict.
		// If yes - continue to update the list of this token - this list is the same as in ex1: pairs of docId-#appearances, i.e. for every document count the appearances of the token in the doc (can be done easily because they are consecutive in this case)/
			// For every pair, as the termID is the same, check if the docId matches the previous docId:
			// If yes - raise the count for this docId
			// If not - add a new entry for this docId and set its appearances to 1

	}

	public static void main(String[] args) {
		String inputFile = "./100.txt";
		String dir = "./Data_Index";
		IndexWriter indexWriter = new IndexWriter();
		indexWriter.write(inputFile, dir);
		System.out.println("here");
	}
}