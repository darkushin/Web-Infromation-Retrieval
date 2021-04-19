package webdata;

import java.io.IOException;
import java.util.*;

public class SlowIndexWriter {
	// token_dict is a dictionary used for creating the tokens index. The keys are the different tokens in the collection
	// and the values of each token is a list of integers, where the first element is the collection frequency of the word,
	// and the rest of the elements are the ids of the files containing this word.
	private HashMap<String, ArrayList<Integer>> tokenDict;
	private TreeMap<String, ArrayList<Integer>> productIds;
	private HashMap<Integer, ArrayList<String>> reviewIds;

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
		createProductIndex();
	}

	private void createDicts(String inputFile) throws IOException {
		productIds = new TreeMap<>();
		tokenDict = new HashMap<>();
		reviewIds = new HashMap<>();

		DataParser dataParser = new DataParser(inputFile);

		for (int i = 0; i< dataParser.allReviews.size(); i++){
			addProductId(dataParser.allReviews.get(i).get("productId"), i);
			int length = addReviewText(dataParser.allReviews.get(i).get("text"), i);
			addReviewId(i, dataParser.allReviews.get(i), length);
		}
	}

	/**
	* Delete all index files by removing the given directory
	*/
	public void removeIndex(String dir) {}

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
			if (tokenDict.containsKey(token)){
				List<Integer> token_info = tokenDict.get(token);
				token_info.set(0, token_info.get(0) + 1);

				// check if the current review was already added to the token's review list, if not add it
				if (token_info.size() > 1 && token_info.get(token_info.size()-1) != reviewIndex){
					token_info.add(reviewIndex);
				}
			}
			else{
				tokenDict.put(token, new ArrayList<>(Arrays.asList(1, reviewIndex)));
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

	private void addReviewId(int reviewId, HashMap<String, String> review, int length) {
		reviewIds.put(reviewId, new ArrayList<>());
		// 0 - productId, 1 - score, 2 - helpfulness, 3 - length
		for (String field : DataParser.INTEREST_FIELDS) {
			if (field.equals("text")) { continue; }
			reviewIds.get(reviewId).add(review.get(field));
		}
		reviewIds.get(reviewId).add(String.valueOf(length));
	}

	private void createProductIndex() {
		class ProductInfo {
			public String productId;
			public byte[] data;
		}
//		ArrayList<ProductInfo> productIndex = new ArrayList<>(productIds.size());
		ProductInfo[] productIndex = new ProductInfo[productIds.size()];
		int i = 0;
		for (Map.Entry<String, ArrayList<Integer>> entry : productIds.entrySet()) {
			String encoding = DeltaEncoder.gamma_encode(entry.getValue().get(0)) +
					DeltaEncoder.gamma_encode(entry.getValue().get(1)); // TODO: Encoding of 0
			byte[] data = DeltaEncoder.toByteArray(encoding);
			ProductInfo prodInfo = new ProductInfo();
			prodInfo.productId = entry.getKey();
			prodInfo.data = data;
			productIndex[i] = prodInfo;
			i++;
		}

	}

	public static void main(String[] args) throws IOException {
		String inputFile = "./1000.txt";
		String dir = "./data-index";

		SlowIndexWriter slw = new SlowIndexWriter(inputFile, dir);

	}
}