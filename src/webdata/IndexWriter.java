package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IndexWriter {
	private HashMap<String, Integer> tokenDict;  // token: tokenId
	private ArrayList<String> invertedTokenDict;  // tokenId: token
	private TreeMap<String, ArrayList<Integer>> productIds;
//	private LinkedList<ArrayList<Integer>> reviewIds;

	private int[][] tokenBuffer = new int[TOKEN_BUFFER_SIZE][2];
	// Array of termID, docID pairs. Regular array to sort in-place
	private int tokenBufferPointer;
	private int tokenFilesNumber = 0;
	private String dir;

	private static final String PRODUCT_INDEX_FILE = "product_index.txt";
	private static final String REVIEW_INDEX_FILE = "review_index.txt";
	private static final String TOKEN_INDEX_FILE = "token_index.txt";
	private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";
	private static final int PAIRS_IN_BLOCK = 1000;
	private static final int M = 20000;
	private static final int TOKEN_BUFFER_SIZE = PAIRS_IN_BLOCK * (M - 1);  // Number of -pairs- in memory. Should be PAIRS_IN_BLOCK * (M-1) or something.

	private static final int NUM_REVIEWS = 10000000;
//	todo: remove the reviewIds file after index creation!
	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	*/
	public void write(String inputFile, String dir) {
		this.dir = dir;
		createDir();
		createDicts(inputFile);
		createProductIndex();
//		invertedTokenDict = null; // TODO: remove? (1)
		try{
			createReviewIndex();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		createTokenIndex();
		File mergedDataFile = new File(dir + "/1");
		mergedDataFile.delete();
		File reviewIds = new File(dir + "/reviewIds");
		reviewIds.delete();
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
		ObjectOutputStream reviewOutput = null;
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(this.dir + "/reviewIds"));
			reviewOutput = new ObjectOutputStream(out);
		}catch (IOException e) {
			System.out.println("Error occurred while saving the index file: reviewIds");
			e.printStackTrace();
			System.exit(1);
		}

		productIds = new TreeMap<>();
		tokenDict = new HashMap<>();
//		reviewIds = new LinkedList<>();
		invertedTokenDict = new ArrayList<>();

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
		for (ArrayList<String> s: dataLoader){
			DataParser.Review review = dataParser.parseReview(s);
			addProductId(review.getProductId(), i);
			int length = addReviewText(review.getText(), i);
			addReviewId(review, reviewOutput, length);
			i++;
			if (i % 100000 == 0){
				System.out.println("Num Reviews: " + i);
				System.out.println("Total Memory: " + Runtime.getRuntime().totalMemory() / (float)(1000000) + " MB" + " (MAX: " + Runtime.getRuntime().maxMemory()/ (float)(1000000) + " MB" + ")");
				System.out.println("Used Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (float)(1000000) + " MB");
				System.out.println("Free Memory: " + Runtime.getRuntime().freeMemory() / (float)(1000000) + " MB");
			}

			if (i == NUM_REVIEWS) {
				break;
			}
		}
		this.sortBuffer();
		try {
			this.saveBuffer();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.tokenBuffer = null;  // free the token buffer space

		try {
			reviewOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Comparator<Integer> cmp = Comparator.comparing(a -> invertedTokenDict.get(a));

		ExternalMergeSort ems = new ExternalMergeSort(cmp, tokenFilesNumber, PAIRS_IN_BLOCK, dir);
		ems.sort();
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
		String[] cleanTokens = Arrays.stream(tokens).filter(value -> value != null && value.length() > 0).toArray(size -> new String[size]);

		for (String token: cleanTokens){
			reviewLength += 1;
			token = token.toLowerCase();
			int termId = tokenDict.computeIfAbsent(token, k -> tokenDict.size());
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
		Arrays.sort(tokenBuffer,0, tokenBufferPointer, Comparator.comparing(a -> invertedTokenDict.get(a[0])));
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
	private void addReviewId(DataParser.Review review, ObjectOutputStream reviewOutput, int length) {
		ArrayList<String> vals = new ArrayList<>();

		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
		vals.add(review.getProductId());
		vals.add(review.getScore());
		vals.add(review.getHelpfulness());
		vals.add(String.valueOf(length));
		try {
			reviewOutput.writeObject(vals);
			reviewOutput.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		reviewIds.add(vals);
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
		Collections.sort(tokens);

		tokenDict = null;
		int k = 256;
		KFront kf = new KFront(true);
		kf.createKFront(k, tokens);
		TokensIndex tIdx = new TokensIndex(k, this.dir);
		tIdx.insertData(kf.getTable(), kf.getConcatString(), dir + "/1");
		saveToDir(TOKEN_INDEX_FILE, tIdx);
	}

	/**
	 * Creates and saves to the disk the review index which hold all information related to reviews.
	 */
	private void createReviewIndex() throws IOException, ClassNotFoundException {
		ObjectInputStream reviewIds = new ObjectInputStream(new FileInputStream(this.dir + "/reviewIds"));

		// Revise the review dictionary to the correct structure & change productIDs to product index
//		ArrayList<List<Integer>> dictValues = new ArrayList<>();
		ArrayList<ReviewIndex.ReviewInfo> data = new ArrayList<>();

		HashMap<String, Integer> productDict = new HashMap<>(productIds.size());
		int i = 0;
		for (String productId: productIds.keySet()){
			productDict.put(productId, i);
			i++;
		}
//		productIds = null; // TODO: remove? (2)
		ReviewIndex rIndex = new ReviewIndex();
		while (true) {
			ArrayList<String> vals = null;
			try {
				vals = (ArrayList<String>) reviewIds.readObject();
			} catch (EOFException ex) {
				break;
			}
			ReviewIndex.ReviewInfo rI = rIndex.new ReviewInfo();
			int[] info = new int[4];
			byte score = (byte) (int) Float.parseFloat(vals.get(1));
			info[ReviewIndex.PRODUCTID_INDEX] = productDict.get(vals.get(0));
			String[] helpf = vals.get(2).split("/");
			info[ReviewIndex.HELPFNUM_INDEX] = Integer.parseInt(helpf[0]);
			info[ReviewIndex.HELPFDNOM_INDEX] = Integer.parseInt(helpf[1]);
			info[ReviewIndex.REVIEWLENGTH_INDEX] = Integer.parseInt(vals.get(3));
			rI.encodedInfo = Encoding.groupVarintEncode(info);
			rI.score = score;
			data.add(rI);


//			new_vals.set(ReviewIndex.PRODUCTID_INDEX, productDict.get(vals.get(0)));
//			String[] helpf = vals.get(2).split("/");
//			new_vals.set(ReviewIndex.HELPFNUM_INDEX, Integer.parseInt(helpf[0]));
//			new_vals.set(ReviewIndex.HELPFDNOM_INDEX, Integer.parseInt(helpf[1]));
//			new_vals.set(ReviewIndex.REVIEWLENGTH_INDEX,  Integer.parseInt(vals.get(3)));
//			new_vals.set(ReviewIndex.SCORE_INDEX,  (int) Float.parseFloat(vals.get(1)));
//			dictValues.add(new_vals);
		}
		reviewIds.close();
//		productDict = null; // TODO: remove? (3)
//		ReviewIndex rIndex = new ReviewIndex();
		rIndex.insertData(data);
//		saveToDir(REVIEW_INDEX_FILE, rIndex);
		rIndex.save(this.dir + "/" + REVIEW_INDEX_FILE);
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

//	public static void main(String[] args) {
//		String inputFile = "./1000.txt";
//		String dir = "./Data_Index";
//		IndexWriter indexWriter = new IndexWriter();
//		indexWriter.write(inputFile, dir);
//	}
}