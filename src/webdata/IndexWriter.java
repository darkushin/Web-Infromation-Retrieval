package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IndexWriter {
	private TreeMap<String, ArrayList<Integer>> tokenDict;  // keys are tokens, values are a list where odd cells are review ids including this token and even cells are the times the token appeared in the review.
	private TreeMap<String, ArrayList<Integer>> productIds;
	private LinkedList<ArrayList<String>> reviewIds;
	private String dir;

	private static final String PRODUCT_INDEX_FILE = "product_index.txt";
	private static final String REVIEW_INDEX_FILE = "review_index.txt";
	private static final String TOKEN_INDEX_FILE = "token_index.txt";
	private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";


	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void write(String inputFile, String dir) {
		this.dir = dir;
		createDicts(inputFile);
		createDir();
		createProductIndex();
		createTokenIndex();
		createReviewIndex();
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
	 * @param inputFile
	 */
	private void createDicts(String inputFile){
		productIds = new TreeMap<>();
		tokenDict = new TreeMap<>();
		reviewIds = new TreeMap<>();

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
		for (ArrayList<String> s: dataLoader){
			DataParser.Review review = dataParser.parseReview(s);
			addProductId(review.getProductId(), i + 1);
			int length = addReviewText(dataParser.allReviews.get(i).get("text"), i + 1);
			addReviewId(review, i, length);
			i++;
		}
	}

	/**
	 * Split the given text of the i-th review into tokens and add them to the tokens dictionary.
	 * @param reviewText the text of the review that should be added.
	 * @param reviewIndex the number of the given review.
	 * @return the number of tokens in the given review text.
	 */
	private int addReviewText(String reviewText, int reviewIndex){
		String[] tokens = reviewText.split("[^a-zA-Z0-9]");  // split to alphanumeric tokens
		int reviewLength = 0;
		for (String token: tokens){
			if (!token.matches("[a-zA-Z0-9]+")){
				continue;
			}
			reviewLength += 1;
			token = token.toLowerCase();
			if (tokenDict.containsKey(token)){  // token already exists, update its entry
				List<Integer> tokenInfo = tokenDict.get(token);
				// check if the current review was already added to the token's review list. If yes, increase the # appearances of the token, else add it with # appearance = 1.
				if (tokenInfo.get(tokenInfo.size()-2) == reviewIndex){
					tokenInfo.set(tokenInfo.size()-1 ,tokenInfo.get(tokenInfo.size()-1) + 1);
				} else {  // token appears first time in the given review
					tokenInfo.add(reviewIndex);
					tokenInfo.add(1);
				}
			}
			else{  // token seen for the first time, add a new entry for it
				tokenDict.put(token, new ArrayList<>(Arrays.asList(reviewIndex, 1)));
			}
		}
		return reviewLength;
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
		ArrayList<String> vals = new ArrayList<>();

		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
		vals.add(review.getProductId());
		vals.add(review.getScore());
		vals.add(review.getHelpfulness());
		vals.add(String.valueOf(length));

		reviewIds.add(vals);
	}

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
	 * write() function.
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
		String input_file = "./1000.txt";
		String dir = "./Data_Index";
		IndexWriter ir = new IndexWriter();
		ir.write(input_file, dir);
	}
}