package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public class IndexReader {
	private static final String PRODUCT_INDEX_FILE = "product_index.txt";
	private static final String REVIEW_INDEX_FILE = "review_index.txt";
	private static final String TOKEN_INDEX_FILE = "token_index.txt";
	private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";

	TokensIndex tokenIndex = null;
	ProductIndex productIndex = null;
	ReviewIndex reviewIndex = null;
	String dir;

	/**
	* Creates an webdata.IndexReader which will read from the given directory
	*/
	public IndexReader(String dir) {
		this.dir = dir;
		loadIndices(dir);
	}

	/**
	 * Load the index files from the disk to the main memory.
	 * @param dir the directory from which the files should be loaded.
	 */
	private void loadIndices(String dir){
		ObjectInputStream in = null;
		try {
			FileInputStream fileIn = new FileInputStream(dir + "/" + TOKEN_INDEX_FILE);
			in = new ObjectInputStream(fileIn);
			tokenIndex = (TokensIndex) in.readObject();
			in.close();
			fileIn.close();

			fileIn = new FileInputStream(dir + "/" + PRODUCT_INDEX_FILE);
			in = new ObjectInputStream(fileIn);
			productIndex = (ProductIndex) in.readObject();
			in.close();
			fileIn.close();

			fileIn = new FileInputStream(dir + "/" + REVIEW_INDEX_FILE);
			in = new ObjectInputStream(fileIn);
			reviewIndex = (ReviewIndex) in.readObject();
			in.close();
			fileIn.close();

		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error occurred while loading an index file.");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	* Returns the product identifier for the given review
	* Returns null if there is no review with the given identifier
	*/
	public String getProductId(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId - 1)) { return null;}
		return productIndex.getWordAt(reviewIndex.getProductNum(reviewId - 1));
	}

	/**
	* Returns the score for a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewScore(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId - 1)) { return -1;}
		return reviewIndex.getScore(reviewId - 1);
	}

	/**
	* Returns the numerator for the helpfulness of a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewHelpfulnessNumerator(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId-1)) { return -1;}
		return reviewIndex.getHelpfulnessNumerator(reviewId-1);
	}

	/**
	* Returns the denominator for the helpfulness of a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewHelpfulnessDenominator(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId - 1)) { return -1;}
		return reviewIndex.getHelpfulnessDenominator(reviewId - 1);
	}

	/**
	* Returns the number of tokens in a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewLength(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId - 1)) { return -1;}
		return reviewIndex.getLength(reviewId - 1);
	}

	/**
	* Return the number of reviews containing a given token (i.e., word)
	* Returns 0 if there are no reviews containing this token
	*/
	public int getTokenFrequency(String token) {
		token = token.toLowerCase();
		int currentTokenIdx = tokenIndex.search(token);
		if (currentTokenIdx == -1){
			return 0;
		}
		return tokenIndex.get(currentTokenIdx).getFrequency();
	}

	/**
	* Return the number of times that a given token (i.e., word) appears in
	* the reviews indexed
	* Returns 0 if there are no reviews containing this token
	*/
	public int getTokenCollectionFrequency(String token) {
		token = token.toLowerCase();
		int currentTokenIdx = tokenIndex.search(token);
		if (currentTokenIdx == -1) {
			return 0;
		}
		return tokenIndex.get(currentTokenIdx).getCollectionFrequency();
	}

	/**
	* Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
	* that id-n is the n-th review containing the given token and freq-n is the
	* number of times that the token appears in review id-n
	* Only return ids of reviews that include the token
	* Note that the integers should be sorted by id
	*
	* Returns an empty Enumeration if there are no reviews containing this token
	*/
	public Enumeration<Integer> getReviewsWithToken(String token) {
		Enumeration<Integer> enumerator = Collections.emptyEnumeration();
		token = token.toLowerCase();
		int currentTokenIdx = tokenIndex.search(token);
		if (currentTokenIdx == -1){
			return enumerator;
		}
		int tokenInvertedIdxPtr = tokenIndex.get(currentTokenIdx).getInvertedIdxPtr();
		int numReviews = tokenIndex.get(currentTokenIdx).getFrequency() * 2;
		byte[] dest = null;
		int nextInvertedIdxPtr;
		try {
			RandomAccessFile file = new RandomAccessFile(this.dir + "/" + TOKEN_INVERTED_INDEX_FILE, "r");
			if (currentTokenIdx + 1 <tokenIndex.get().size()) {
				nextInvertedIdxPtr = tokenIndex.get(currentTokenIdx + 1).getInvertedIdxPtr();
			} else {
				nextInvertedIdxPtr = (int) file.length();
			}
			int bytesToRead = nextInvertedIdxPtr - tokenInvertedIdxPtr;
			dest  = new byte[bytesToRead];
			file.seek(tokenInvertedIdxPtr);
			file.read(dest);
		} catch (IOException e){
			System.out.println("Error occurred while accessing the tokens_inverted_index file.");
			e.printStackTrace();
			System.exit(1);
		}

		ArrayList<Integer> vals = new ArrayList<>();
		for (int i = 0; i < dest.length; i = i +4){
			byte[] numBytes = Arrays.copyOfRange(dest, i, i+4);
			vals.add(ByteBuffer.wrap(numBytes).getInt());
		}
		Encoding.diffToIds(vals);
		return Collections.enumeration(vals);
	}

	/**
	* Return the number of product reviews available in the system
	*/
	public int getNumberOfReviews() {
		return reviewIndex.getNumReview();
	}

	/**
	* Return the number of number of tokens in the system
	* (Tokens should be counted as many times as they appear)
	*/
	public int getTokenSizeOfReviews() {return tokenIndex.getNumTokens();}
	
	/**
	* Return the ids of the reviews for a given product identifier
	* Note that the integers returned should be sorted by id
	*
	* Returns an empty Enumeration if there are no reviews for this product
	*/
	public Enumeration<Integer> getProductReviews(String productId) {
		Enumeration<Integer> enumerator = Collections.emptyEnumeration();
		int productIdx = productIndex.search(productId);
		if (productIdx == -1){
			return enumerator;
		}
		int firstReview = productIndex.getReviewId(productIdx);
		int reviewSpan = productIndex.getReviewSpan(productIdx);
		ArrayList<Integer> reviews = new ArrayList<>();
		for (int i = 0; i <= reviewSpan; i++){
			reviews.add(firstReview + i);
		}
		return Collections.enumeration(reviews);
	}

//	public static void main(String[] args) {
//		IndexReader indexReader = new IndexReader("./Data_index");
//		indexReader.getReviewsWithToken("0");
//	}
}