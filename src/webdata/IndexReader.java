package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public class IndexReader {
	private static String[] indicesNames = {"products_index.txt", "review_index.txt", "token_index.txt"};

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

	private void loadIndices(String dir){
		ObjectInputStream in = null;
		try {
			FileInputStream fileIn = new FileInputStream(dir + "/tokens_index.txt");
			in = new ObjectInputStream(fileIn);
			tokenIndex = (TokensIndex) in.readObject();
			in.close();
			fileIn.close();

			fileIn = new FileInputStream(dir + "/products_index.txt");
			in = new ObjectInputStream(fileIn);
			productIndex = (ProductIndex) in.readObject();
			in.close();
			fileIn.close();

			fileIn = new FileInputStream(dir + "/review_index.txt");
			in = new ObjectInputStream(fileIn);
			reviewIndex = (ReviewIndex) in.readObject();
			in.close();
			fileIn.close();

		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error occurred while loading a index file.");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	* Returns the product identifier for the given review
	* Returns null if there is no review with the given identifier
	*/
	public String getProductId(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId)) { return null;}
		return productIndex.getWordAt(reviewIndex.getProductNum(reviewId));
	}

	/**
	* Returns the score for a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewScore(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId)) { return -1;}
		return reviewIndex.getScore(reviewId);
	}

	/**
	* Returns the numerator for the helpfulness of a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewHelpfulnessNumerator(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId)) { return -1;}
		return reviewIndex.getHelpfulnessNumerator(reviewId);
	}

	/**
	* Returns the denominator for the helpfulness of a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewHelpfulnessDenominator(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId)) { return -1;}
		return reviewIndex.getHelpfulnessDenominator(reviewId);
	}

	/**
	* Returns the number of tokens in a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewLength(int reviewId) {
		if (!reviewIndex.isReviewIdValid(reviewId)) { return -1;}
		return reviewIndex.getLength(reviewId);
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
		int nextInvertedIdxPtr = tokenIndex.get(currentTokenIdx + 1).getInvertedIdxPtr();
		int numReviews = tokenIndex.get(currentTokenIdx).getFrequency() * 2;
		int bytesToRead = nextInvertedIdxPtr - tokenInvertedIdxPtr;
		byte[] dest = new byte[bytesToRead];
		try {
			RandomAccessFile file = new RandomAccessFile(this.dir + "/tokens_inverted_index.txt", "r");
			file.seek(tokenInvertedIdxPtr);
			file.read(dest);
		} catch (IOException e){
			System.out.println("Error occurred while accessing the tokens_inverted_index file.");
			e.printStackTrace();
			System.exit(1);
		}
		ArrayList<Integer> vals = new ArrayList<Integer>(Encoding.deltaDecode(dest).subList(0, numReviews));
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
//		String dir = "./data-index";
//
//		IndexReader indexReader = new IndexReader(dir);
//		Enumeration<Integer> e = indexReader.getReviewsWithToken("1");
//		int i = indexReader.getTokenSizeOfReviews();
//		System.out.println(i);
//	}
}