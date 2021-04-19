package webdata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class SlowIndexWriter {
	// token_dict is a dictionary used for creating the tokens index. The keys are the different tokens in the collection
	// and the values of each token is a list of integers, where the first element is the collection frequency of the word,
	// and the reset of the elements are the ids of the files containing this word.
	private Hashtable<String, List<Integer>> token_dict;

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
		DataParser dataParser = new DataParser(inputFile);
		String text;
		for (int i=0; i< dataParser.all_reviews.size(); i++){
			text = dataParser.all_reviews.get(i).get("text");
			addReviewText(text, i);
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
	private void addReviewText(String reviewText, int i){
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
	}

	public static void main(String[] args) throws IOException {
		String inputFile = "/Users/darkushin/Downloads/100.txt";
		String dir = "/Users/darkushin/Desktop/Web-Infromation-Retrieval/data-index";

		SlowIndexWriter slw = new SlowIndexWriter(inputFile, dir);

	}
}