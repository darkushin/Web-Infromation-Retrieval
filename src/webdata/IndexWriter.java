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
	private static final int TOKEN_BUFFER_SIZE = 5000;  // Number of -pairs- in memory. Should be PAIRS_IN_BLOCK * (M-1) or something.

	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void write(String inputFile, String dir) {
		this.dir = dir;
		createDir();
		createDicts(inputFile);
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
		int i=1;
		for (String s: dataLoader){
			DataParser.Review review = dataParser.parseReview(s);
			addProductId(review.getProductId(), i);
			int length = addReviewText(review.getText(), i);
			addReviewId(review, i, length);
			i++;
		}
		this.sortBuffer();
		try {
			this.saveBuffer();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}


		// todo: merge sort all files - maybe move to a new function
		Comparator<Integer> cmp = Comparator.comparing(a -> invertedTokenDict.get(a));

		for (int j = 1; j <= tokenFilesNumber; j++) {
			System.out.println("File " + j + " sorted: " + isFileSorted(dir + "/iteration_1/" + j, cmp));
			System.out.println("File " + j + " count: " + countNumsInFile(dir + "/iteration_1/" + j));
		}

		ExternalMergeSort ems = new ExternalMergeSort(cmp, tokenFilesNumber, PAIRS_IN_BLOCK, dir);
		ems.sort();
		System.out.println(isFileSorted(dir + "/1", cmp));
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
					System.out.println("Oops! Occured in " + tot);
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
			ArrayList<Integer> termIdArr = tokenDict.computeIfAbsent(token, k -> new ArrayList<Integer>(Arrays.asList(tokenDict.size())));
			int termId = termIdArr.get(0);
			if (termId == invertedTokenDict.size()) { invertedTokenDict.add(token);}  // if a new token was added, add it also to the invertedTokenDict
			tokenBuffer[tokenBufferPointer][0] = termId;
			tokenBuffer[tokenBufferPointer][1] = reviewIndex;
			tokenBufferPointer++;
			if (tokenBufferPointer == TOKEN_BUFFER_SIZE){
				this.sortBuffer();
				try {
					this.saveBuffer();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				this.clearBuffer();
			}
		}
		return reviewLength;
	}

	private void sortBuffer() {
		Arrays.sort(tokenBuffer, Comparator.comparing(a -> invertedTokenDict.get(a[0])));
	}

	private void saveBuffer() throws IOException {
		this.tokenFilesNumber++;
		ObjectOutputStream tokenBufferWriter = new ObjectOutputStream(new FileOutputStream(dir + ExternalMergeSort.folderName + "1/" + tokenFilesNumber));
		for (int i = 0; i < tokenBufferPointer; i++) {
			tokenBufferWriter.writeInt(tokenBuffer[i][0]);
			tokenBufferWriter.writeInt(tokenBuffer[i][1]);
		}
		tokenBufferWriter.close();
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
		// Convert the current tokenDict of {token:termId} pairs to {token:[docId1,#freq1,docId2,#freq2,...]} format.
		this.prepareTokenDict();
		// todo: need to sort the dictionary by keys!
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
	private void prepareTokenDict(){
		// todo: change the fileName to be according to the directory!
		String fileName = this.dir + "/iteration_2/1";
		FileInputStream fileIn = null;
		ObjectInputStream termFile = null;
		try {
			fileIn = new FileInputStream(fileName);
			termFile = new ObjectInputStream(fileIn);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// read all the integers from the file until reaching EOF
		try{
			int previousTermId = 0;
			int previousDocId = 0;
			ArrayList<Integer> tokenVals = new ArrayList<>();  // odd places-docId, even places-freq in doc.
			while (true){ // todo: ugly solution, any better idea?
				int termId = termFile.readInt();
				int docId = termFile.readInt();
				if (termId == previousTermId){
					if (docId == previousDocId){ // token already appeared in the doc - increment the frequency
						tokenVals.set(tokenVals.size()-1, tokenVals.get(tokenVals.size()-1) + 1);
					} else { // first appearance of the token in this doc
						tokenVals.addAll(Arrays.asList(docId, 1));
						previousDocId = docId;
					}
				} else {
					// save the values of the previous token:
					String token = invertedTokenDict.get(previousTermId);
					tokenDict.put(token, tokenVals);

					// start a new array for the new term:
					tokenVals = new ArrayList<>(Arrays.asList(docId, 1));
					previousTermId = termId;
					previousDocId = docId;
				}
			}

		} catch (EOFException e){  // reached EOF and finished converting all tokens.
			return;
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Error occurred while converting token dict.");
			System.exit(1);
		}


		// while we didn't reach EOF, read two integers at a time - termID and docID.
		// for every such pair, check if the termID is the same as the termID of the previous:
		// If not - find the token matching to the termID (using invertedTermId dict) and add the list created here to the tokenDict.
		// If yes - continue to update the list of this token - this list is the same as in ex1: pairs of docId-#appearances, i.e. for every document count the appearances of the token in the doc (can be done easily because they are consecutive in this case)/
			// For every pair, as the termID is the same, check if the docId matches the previous docId:
			// If yes - raise the count for this docId
			// If not - add a new entry for this docId and set its appearances to 1

	}

	public static void main(String[] args) {
		String inputFile = "./1000.txt";
		String dir = "./Data_Index";
		IndexWriter indexWriter = new IndexWriter();
		indexWriter.write(inputFile, dir);
		System.out.println("here");
	}
}