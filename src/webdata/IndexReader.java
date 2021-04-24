package webdata;

import java.io.*;
import java.util.Enumeration;

public class IndexReader {
	TokensIndex tokenIndex = null;

	/**
	* Creates an webdata.IndexReader which will read from the given directory
	*/
	public IndexReader(String dir) {
		ObjectInputStream in = null;
		try {
			FileInputStream fileIn = new FileInputStream(dir + "/tokens_index.txt");
			in = new ObjectInputStream(fileIn);
			tokenIndex = (TokensIndex) in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("In index reader!");
	}
	
	/**
	* Returns the product identifier for the given review
	* Returns null if there is no review with the given identifier
	*/
	public String getProductId(int reviewId) {return "";}

	/**
	* Returns the score for a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewScore(int reviewId) {return 0;}

	/**
	* Returns the numerator for the helpfulness of a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewHelpfulnessNumerator(int reviewId) {return 0;}

	/**
	* Returns the denominator for the helpfulness of a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewHelpfulnessDenominator(int reviewId) {return 0;}

	/**
	* Returns the number of tokens in a given review
	* Returns -1 if there is no review with the given identifier
	*/
	public int getReviewLength(int reviewId) {return 0;}

	/**
	* Return the number of reviews containing a given token (i.e., word)
	* Returns 0 if there are no reviews containing this token
	*/
	public int getTokenFrequency(String token) {
		token = token.toLowerCase();
		int currentTokenIdx = tokenIndex.search(token);
		if (currentTokenIdx == -1){
			return 0;
		} else {
			return 0; //tokenIndex.get().get(currentTokenIdx).getFrequency();
		}
	}

	/**
	* Return the number of times that a given token (i.e., word) appears in
	* the reviews indexed
	* Returns 0 if there are no reviews containing this token
	*/
	public int getTokenCollectionFrequency(String token) {return 0;}

	/**
	* Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
	* that id-n is the n-th review containing the given token and freq-n is the
	* number of times that the token appears in review id-n
	* Only return ids of reviews that include the token
	* Note that the integers should be sorted by id
	*
	* Returns an empty Enumeration if there are no reviews containing this token
	*/
	public Enumeration<Integer> getReviewsWithToken(String token) {return null;}

	/**
	* Return the number of product reviews available in the system
	*/
	public int getNumberOfReviews() {return 0;}

	/**
	* Return the number of number of tokens in the system
	* (Tokens should be counted as many times as they appear)
	*/
	public int getTokenSizeOfReviews() {return 0;}
	
	/**
	* Return the ids of the reviews for a given product identifier
	* Note that the integers returned should be sorted by id
	*
	* Returns an empty Enumeration if there are no reviews for this product
	*/
	public Enumeration<Integer> getProductReviews(String productId) {return null;}

	public static void main(String[] args) {
		String dir = "./data-index";

		IndexReader indexReader = new IndexReader(dir);
		int i = indexReader.getTokenFrequency("10");
		System.out.println(i);
	}
}