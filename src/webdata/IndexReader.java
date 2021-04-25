package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
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
			RandomAccessFile file = new RandomAccessFile("data-index/tokens_inverted_index.txt", "r");
			file.seek(tokenInvertedIdxPtr);
			file.read(dest);
		} catch (IOException e){  // todo: should raise the error in this case
			e.printStackTrace();
		}
		ArrayList<Integer> vals = new ArrayList<Integer>(Encoding.deltaDecode(dest).subList(0, numReviews));
		Encoding.diffToIds(vals);

		return Collections.enumeration(vals);
	}

	/**
	* Return the number of product reviews available in the system
	*/
	public int getNumberOfReviews() {return 0;}

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
	public Enumeration<Integer> getProductReviews(String productId) {return null;}

	public static void main(String[] args) {
		String dir = "./data-index";

		IndexReader indexReader = new IndexReader(dir);
		Enumeration<Integer> e = indexReader.getReviewsWithToken("1");
		int i = indexReader.getTokenSizeOfReviews();
		System.out.println(i);
	}
}