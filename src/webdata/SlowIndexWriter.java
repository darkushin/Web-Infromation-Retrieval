package webdata;

import java.io.*;
import java.math.BigInteger;
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
		createProductIndex();
		createDir();
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
		try{
			File folder = new File(this.dir);
			if (!folder.exists()){
				boolean bool = folder.mkdir();
//				if (!bool){}  //todo: raise exception if folder creation failed
			}
		} catch (Exception e){
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
	 * Split the given text of the ith review into tokens and add them to the tokens dictionary.
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

	/**
	 * Creates the index file for the tokens in the collection.
	 * The index is created using the k-1-in-k front coding method.
	 * @param k the size of the groups that should be used.
	 * @param dir the directory in which the index should be saved.
	 */
//	private void createTokenIndex(int k, String dir){
//		try {
//			Map<String, ArrayList<Integer>> sortedTokens = new TreeMap<String, ArrayList<Integer>>(tokenDict);
//			ArrayList<ArrayList<Integer>> tokenInfo = new ArrayList<ArrayList<Integer>>(); // each entry is a token and contains the token's frequency, collection frequency, word length, prefix length/string offset, inverted index pointer
//			StringBuilder encodedString = new StringBuilder();
//			RandomAccessFile invertedIndexFile = new RandomAccessFile(dir + "/tokens_inverted_index.txt", "rw");
//			String groupString = "";
//			String prevToken = null;
//			int tokenOffset = 0;
//			for (Map.Entry<String, ArrayList<Integer>> entry : sortedTokens.entrySet()) {
//				String token = entry.getKey();
//				ArrayList<Integer> tokenValues = entry.getValue();
//				int tokenFrequency = tokenValues.size() - 1;
//				int tokenCollectionFrequency = tokenValues.get(0);
//				int tokenLength = token.length();
//				int tokenInvertedIndexPtr = (int) invertedIndexFile.getFilePointer();  // todo: check about this casting! find a way to avoid it
//
//				// add the reviewIds of the given token to the inverted index file:
//				saveInvertedIndex(invertedIndexFile, tokenValues.subList(1, tokenValues.size()));
//
//				if (tokenOffset == 0) {  // first token in group, save the entire token
//					groupString = token;
//					int stringOffset = encodedString.length();
//					tokenInfo.add(new ArrayList<>(Arrays.asList(tokenFrequency, tokenCollectionFrequency, tokenLength, stringOffset, tokenInvertedIndexPtr)));
//
//				} else {
//					String commonPrefix = findCommonPrefix(token, prevToken);
//					int prefixLength = commonPrefix.length();
//					groupString += token.substring(prefixLength);
//					tokenInfo.add(new ArrayList<>(Arrays.asList(tokenFrequency, tokenCollectionFrequency, tokenLength, prefixLength, tokenInvertedIndexPtr)));
//				}
//
//				prevToken = token;
//				tokenOffset += 1;
//				if (tokenOffset == k) {
//					tokenOffset = 0;
//					encodedString.append(groupString);
//				}
//			}
//
//			if (tokenOffset != 0) {  // need to add last groupString to the encoded string
//				encodedString.append(groupString);
//			}
//
//			// save the encodedString at the index folder:
//			saveFile(dir, "token_index_k=4", encodedString.toString());
//
//		} catch (Exception e){
//			System.out.println("Error occurred while creating the tokens dictionary");
//			e.printStackTrace();
//		}
//
//	}

	/**
	 * Encodes the integers given in the integer list using delta encoding, and saves them in the given invertedIndexFile.
	 * @param invertedIndexFile the file in which the delta encoded numbers should be saved.
	 * @param idsList a list with number that should be encoded and saved in the inverted index file.
	 */
	private void saveInvertedIndex(RandomAccessFile invertedIndexFile, List<Integer> idsList) {
		try {
			// change the idsList to a difference list (except for the first id):
			for (int i = idsList.size()-1; i>0; i--){
				idsList.set(i, idsList.get(i) - idsList.get(i-1));
			}

			StringBuilder stringCodes = new StringBuilder();
			for (int num : idsList) {
				String code = DeltaEncoder.deltaEncode(num);
				stringCodes.append(code);
			}
			byte[] codeBytes = new BigInteger(stringCodes.toString(), 2).toByteArray();
			if (codeBytes[0] == 0){codeBytes = Arrays.copyOfRange(codeBytes, 1, codeBytes.length);}  // todo: check if this condition is met
			invertedIndexFile.write(codeBytes);
		} catch (Exception e){
			System.out.println("Error occurred while save invertedIndex bytes");
			e.printStackTrace();
		}
	}

	private String findCommonPrefix(String s1, String s2){
		StringBuilder commonPrefix = new StringBuilder();
		int i = 0;
		while (i < s1.length() && i < s2.length() && s1.charAt(i) == s2.charAt(i)){
			commonPrefix.append(s1.charAt(i));
			i++;
		}
		return commonPrefix.toString();
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

		KFront kf = new KFront();
		kf.createKFront(4, ids);
		for (int i = 0; i < vals.size(); i++) {
			kf.getTable().get(i).addAll(vals.get(i));
		}

		ProductIndex pIndex = new ProductIndex(4);
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

	private void createTokenIndex(){
		LinkedList<String> tokens = new LinkedList<>(tokenDict.keySet());
		ArrayList<ArrayList<Integer>> vals = new ArrayList<>(tokenDict.values());
		KFront kf = new KFront();
		kf.createKFront(8, tokens);
		saveFile("tokens_concatenated_string", kf.getConcatString());

		RandomAccessFile invertedIndexFile = null;
		try {
			invertedIndexFile = new RandomAccessFile(this.dir + "/tokens_inverted_index.txt", "rw");
		} catch (FileNotFoundException e) {
			System.out.println("Error occurred while creating the tokens_inverted_index file");
			e.printStackTrace();
		}

		int frequency;
		int collectionFrequency;
		int invertedIndexPtr;
		int length;
		for (int i = 0; i < tokens.size(); i ++){
			ArrayList<Integer> tokenValues = vals.get(i);
			frequency = tokenValues.size() - 1;
			collectionFrequency = tokenValues.get(0);
			length = tokens.get(i).length();
			try {
				invertedIndexPtr = (int) invertedIndexFile.getFilePointer();  // todo: check about this casting! find a way to avoid it
				saveInvertedIndex(invertedIndexFile, tokenValues.subList(1, tokenValues.size()));
				kf.setValue(i, Arrays.asList(frequency, collectionFrequency, length, invertedIndexPtr));
			} catch (IOException e) {
				System.out.println("Error occurred while accessing the tokens_inverted_index file");
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		String inputFile = "./100.txt";
		String dir = "./data-index";

		SlowIndexWriter slw = new SlowIndexWriter(inputFile, dir);

	}
}