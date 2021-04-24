package webdata;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SlowIndexWriter {
	// tokenDict is a dictionary used for creating the tokens index. The keys are the different tokens in the collection
	// and the values of each token is a list of integers, where the first element is the collection frequency of the word,
	// and the rest of the elements are the ids of the files containing this word.
	private TreeMap<String, ArrayList<Integer>> tokenDict;
	private TreeMap<String, ArrayList<Integer>> productIds;
	private HashMap<Integer, ArrayList<String>> reviewIds;
	private String dir;

	public SlowIndexWriter(String inputFile, String dir) throws IOException {
		this.dir = dir;
		slowWrite(inputFile);
	}


	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void slowWrite(String inputFile) throws IOException {
		createDicts(inputFile);
		createDir();
		createProductIndex();
		createTokenIndex();
	}

	/**
	 * Delete all index files by removing the given directory
	 */
	public void removeIndex(String dir) {}

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

	private void saveFile(String fileName, String data){
		try {
			FileWriter file = new FileWriter(this.dir + "/" + fileName + ".txt");
			file.write(data);
			file.close();
		} catch (IOException e) {
			System.out.println("Error occurred while writing an index file.");
			e.printStackTrace();
		}
	}

	private void createDicts(String inputFile) throws IOException {
		productIds = new TreeMap<>();
		tokenDict = new TreeMap<>();
		reviewIds = new HashMap<>();

		DataParser dataParser = new DataParser(inputFile);

		for (int i = 0; i< dataParser.allReviews.size(); i++){
			addProductId(dataParser.allReviews.get(i).get("productId"), i);
			int length = addReviewText(dataParser.allReviews.get(i).get("text"), i);
			addReviewId(dataParser.allReviews.get(i), i, length);
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
				tokenInfo.set(0, tokenInfo.get(0) + 1);

				// check if the current review was already added to the token's review list, if not add it
				if (tokenInfo.get(tokenInfo.size()-1) != reviewIndex){
					tokenInfo.add(reviewIndex);
				}
			}
			else{  // token seen for the first time, add a new entry for it
				tokenDict.put(token, new ArrayList<>(Arrays.asList(1, reviewIndex)));
			}
		}
		return reviewLength;
	}

	private void addProductId(String productId, int reviewId) {
		if (!productIds.containsKey(productId)) {
			productIds.put(productId, new ArrayList<>(Arrays.asList(reviewId, 0)));
		}
		else {
			ArrayList<Integer> product = productIds.get(productId);
			product.set(1, product.get(1) + 1);
		}
	}

	private void addReviewId(HashMap<String, String> review, int reviewId, int length) {
		reviewIds.put(reviewId, new ArrayList<>());
		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
		for (String field : DataParser.INTEREST_FIELDS) {
			if (field.equals("text")) { continue; }
			reviewIds.get(reviewId).add(review.get(field));
		}
		reviewIds.get(reviewId).add(String.valueOf(length));
	}

	private void createProductIndex() {
		LinkedList<String> ids = new LinkedList<>(productIds.keySet());
		ArrayList<ArrayList<Integer>> vals = new ArrayList<>(productIds.values());
		int k = 4;
		KFront kf = new KFront();
		kf.createKFront(k, ids);
		for (int i = 0; i < vals.size(); i++) {
			kf.getTable().get(i).addAll(vals.get(i));  // todo: I added a setValue() function to KFront. we should use it here
		}

		ProductIndex pIndex = new ProductIndex(k);
		pIndex.insertData(kf.getTable(), kf.getConcatString());

		// Test that all words can be successfully retrieved & searched for
		for (int i = 0; i < kf.getTable().size(); i++) {
			String srch = pIndex.getWordAt(i);
			if (!srch.equals(ids.get(i))) {
				System.out.println("Failed to get word " + ids.get(i) + " (" + i + ")");
			}
			int idx = pIndex.search(srch);
			if (i != idx) {
				System.out.println("Failed to search for " + ids.get(i) + " (" + i + ")");
			}
		}
		// Search for things that shouldn't be in the dictionary
		if (pIndex.search("AAAAAAAA") != -1) {
			System.out.println("Oh no!");
		}
		if (pIndex.search("ZZZZZZZZ") != -1) {
			System.out.println("Oh no!");
		}
		if (pIndex.search("B0000044") != -1) {
			System.out.println("Oh no!");
		}

		System.out.println("yo");
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
		saveFile("tokens_concatenated_string", kf.getConcatString());

		TokensIndex tIdx = new TokensIndex(k, this.dir);
		tIdx.insertData(kf.getTable(), vals, kf.getConcatString());

		// Save the tokens index:
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(this.dir + "/tokens_index.txt");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(tIdx);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		String inputFile = "./100.txt";
		String dir = "./data-index";

		SlowIndexWriter slw = new SlowIndexWriter(inputFile, dir);

	}
}