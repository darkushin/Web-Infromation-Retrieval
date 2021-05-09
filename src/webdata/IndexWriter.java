package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IndexWriter {
	private HashMap<String, Integer> tokenDict;  // token: tokenId
	private ArrayList<String> invertedTokenDict;  // tokenId: token
	private TreeMap<String, ArrayList<Integer>> productIds;
	private TreeMap<Integer, ArrayList<String>> reviewIds;

	private int[][] tokenBuffer; // Array of termID, docID pairs. Regular array to sort in-place
	private int tokenBufferPointer;
	private ObjectOutputStream tokenBufferWriter;

	private String dir;

	private static final String PRODUCT_INDEX_FILE = "product_index.txt";
	private static final String REVIEW_INDEX_FILE = "review_index.txt";
	private static final String TOKEN_INDEX_FILE = "token_index.txt";
	private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";
	private static final int REVIEWS_TO_LOAD = 1000;
	private static final int TOKEN_BUFFER_SIZE = 10;


	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void write(String inputFile, String dir) {
		this.dir = dir;
		createDir();
		createDicts(inputFile);
//		createProductIndex();
//		createTokenIndex();
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

		tokenBuffer = new int[2][TOKEN_BUFFER_SIZE];
		tokenBufferPointer = 0;
		try {
			tokenBufferWriter = new ObjectOutputStream(new FileOutputStream(dir + "/tokenpairs.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		DataLoader dataLoader = null;
		DataParser dataParser = new DataParser();
		try {
			dataLoader = new DataLoader(inputFile);
		} catch (IOException e) {
			System.out.println("Error occurred while reading the reviews input file.");
			System.exit(1);
		}
		int i=0;
		for (String s: dataLoader){
			DataParser.Review review = dataParser.parseReview(s);
			addProductId(review.getProductId(), i + 1);
			int length = addReviewText(review.getText(), i + 1);
//			addReviewId(review, i, length);
			i++;
			if (i == REVIEWS_TO_LOAD){
				i = 0;
				productIds.clear();
				tokenDict.clear();
				reviewIds.clear();
				// todo: save the current dicts to disk
			}

		}
		try {
			tokenBufferWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// todo: merge all dictionaries
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
		tokens = new String[]{"I", "bought", "I", "I"};
		for (String token: tokens){
			if (!token.matches("[a-zA-Z0-9]+")){
				continue;
			}
			reviewLength += 1;
			token = token.toLowerCase();
			int termId = tokenDict.computeIfAbsent(token, k -> tokenDict.size());
			if (termId == invertedTokenDict.size()) { invertedTokenDict.add(token);}  // if a new token was added, add it also to the invertedTokenDict
			tokenBuffer[0][tokenBufferPointer] = termId;
			tokenBuffer[1][tokenBufferPointer] = reviewIndex;
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
		// TODO Currently this is not in-place.
		Arrays.sort(tokenBuffer, Comparator.comparingInt(a -> a[0]));
//		Arrays.sort(tokenBuffer,  (a, b) -> invertedTokenDict.get(a[0]).compareTo(invertedTokenDict.get(a[1])));
	}

	private void saveBuffer() {
		for (int i = 0; i < TOKEN_BUFFER_SIZE; i++) {
			try {
				tokenBufferWriter.writeInt(tokenBuffer[0][i]);
				tokenBufferWriter.writeInt(tokenBuffer[1][i]);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// TODO should we write the entire buffer?
//		try {
//			tokenBufferWriter.writeObject(tokenBuffer);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private void clearBuffer() {
		tokenBuffer = new int[2][TOKEN_BUFFER_SIZE];
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
//	private void createTokenIndex(){
//		LinkedList<String> tokens = new LinkedList<>(tokenDict.keySet());
//		ArrayList<ArrayList<Integer>> vals = new ArrayList<>(tokenDict.values());
//		int k = 8;
//
//		KFront kf = new KFront(true);
//		kf.createKFront(k, tokens);
//
//		TokensIndex tIdx = new TokensIndex(k, this.dir);
//		tIdx.insertData(kf.getTable(), vals, kf.getConcatString());
//
//		saveToDir(TOKEN_INDEX_FILE, tIdx);
//	}

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

	public static void main(String[] args) {
		String inputFile = "./100.txt";
		String dir = "./Data_Index";
		IndexWriter indexWriter = new IndexWriter();
		indexWriter.write(inputFile, dir);
		System.out.println("here");
	}
}