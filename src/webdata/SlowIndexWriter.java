package webdata;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SlowIndexWriter {
	private TreeMap<String, ArrayList<Integer>> tokenDict;  // keys are tokens, values are a list where odd cells are review ids including this token and even cells are the times the token appeared in the review.
	private TreeMap<String, ArrayList<Integer>> productIds;
	private TreeMap<Integer, ArrayList<String>> reviewIds;
	private String dir; // TODO Do we support multiple dirs

	public SlowIndexWriter() {
//		this.dir = dir;
//		slowWrite(inputFile);
	}


	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void slowWrite(String inputFile, String dir) throws IOException {
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
		reviewIds = new TreeMap<>();

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
//			kf.setValue(i, vals.get(i));
		}

		ProductIndex pIndex = new ProductIndex(k);
		pIndex.insertData(kf.getTable(), kf.getConcatString());
		saveToDir("products_index.txt", pIndex);
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
//		saveFile("tokens_concatenated_string", kf.getConcatString());

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

	private void createReviewIndex() {
		System.out.println("yo");
	}

	private void saveToDir(String name, Object obj) {
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(this.dir + "/" + name);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		String inputFile = "./100.txt";
		String dir = "./data-index";

		SlowIndexWriter slw = new SlowIndexWriter();
		slw.slowWrite(inputFile, dir);
	}
}