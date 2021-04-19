package webdata;

import java.io.IOException;
import java.util.*;

public class SlowIndexWriter {
	// token_dict is a dictionary used for creating the tokens index. The keys are the different tokens in the collection
	// and the values of each token is a list of integers, where the first element is the collection frequency of the word,
	// and the reset of the elements are the ids of the files containing this word.
	private Hashtable<String, List<Integer>> token_dict;
	private Hashtable<String, HashSet<Integer>> productIds;
	private Hashtable<Integer, ArrayList<String>> reviewIds;

	public SlowIndexWriter(String inputFile, String dir) throws IOException {
		slowWrite(inputFile, dir);
	}


	/**
	* Given product review data, creates an on disk index
	* inputFile is the path to the file containing the review data
	* dir is the directory in which all index files will be created
	* if the directory does not exist, it should be created
	*/
	public void slowWrite(String inputFile, String dir) throws IOException {
		createDicts(inputFile);
	}

	private void createDicts(String inputFile) throws IOException {
		productIds = new Hashtable<>();
		token_dict = new Hashtable<>();
		reviewIds = new Hashtable<>();

		DataParser dataParser = new DataParser(inputFile);

		for (int i = 0; i< dataParser.allReviews.size(); i++){
			addProductId(dataParser.allReviews.get(i).get("productId"), i);
			int length = addReviewText(dataParser.allReviews.get(i).get("text"), i);
			addReviewId(i, dataParser.allReviews.get(i), 1);
		}
	}

	/**
	* Delete all index files by removing the given directory
	*/
	public void removeIndex(String dir) {}

	/**
	 * Split the given text of the ith review into tokens and add them to the tokens dictionary.
	 * @param reviewText the text of the review that should be added.
	 * @param i the number of the given review.
	 */
	private int addReviewText(String reviewText, int i){
		String[] words = reviewText.split("[^a-zA-Z0-9]");
		for (String word: words){
			if (token_dict.containsKey(word)){
				List<Integer> word_info = token_dict.get(word);
				word_info.set(0, word_info.get(0) + 1);

				// check if the current review was already added to the word's review list, if not add it
				if (word_info.get(word_info.size()-1) != i){
					word_info.add(i);
				}
			}
			else{
				token_dict.put(word, Arrays.asList(0, i));
			}
		}
		return 1;
	}

	private void addProductId(String productId, int reviewId) {
		if (!productIds.containsKey(productId)) {
			productIds.put(productId, new HashSet<Integer>());
		}
		productIds.get(productId).add(reviewId);
	}

	private void addReviewId(int reviewId, Hashtable<String, String> review, int length) {
		reviewIds.put(reviewId, new ArrayList<>());
		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
		for (String field : DataParser.INTEREST_FIELDS) {
			if (field.equals("text")) { continue; }
			reviewIds.get(reviewId).add(review.get(field));
		}
		reviewIds.get(reviewId).add(String.valueOf(length));
	}

	public static void main(String[] args) throws IOException {
		String inputFile = "./100.txt";
		String dir = "./data-index";

		SlowIndexWriter slw = new SlowIndexWriter(inputFile, dir);

	}
}