package webdata;

import java.io.*;
import java.util.*;


public class DataParser {
    ArrayList<HashMap<String, String>> allReviews = new ArrayList<>();
    public static final List<String> INTEREST_FIELDS = Arrays.asList("productId", "score", "helpfulness", "text");


    /**
     * Given product review data, parses the data and creates a new list where each entry i contains hashmap with the fields
     * of the review, i.e: productId->value, score->value, helpfulness->value, text->value.
     * inputFile is the path to the file containing the review data
     */
    public DataParser(String inputFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line;
        String review = "";
        while((line = br.readLine()) != null) {
            if (line.contains("product/productId")){
                if (!review.equals("")){
                    allReviews.add(__parse_review(review));
                }
                review = line;
            }
            else{
                review += line;
            }
        }
        allReviews.add(__parse_review(review));  // add the last review
    }

    /**
     * Given a single review, parse the review and return a hash table containing only the relevant fields of the
     * review, i.e: productId, score, helpfulness, text.
     * @param review: the review that should be parsed.
     * @return a hash table where the keys are the relevant fields mentioned above and their corresponding values.
     */
    private static HashMap<String, String> __parse_review(String review){
        List<String> fields = Arrays.asList(review.split("review/"));
        HashMap<String, String> review_fields = new HashMap<String, String>();

        review_fields.put("productId", fields.get(0).split(":")[1]);
        for (int i=1; i<fields.size(); i++){
            String field = fields.get(i);
            List<String> field_value = Arrays.asList(field.split(":"));
            if (INTEREST_FIELDS.contains(field_value.get(0))) {
                review_fields.put(field_value.get(0), String.join(":", field_value.subList(1, field_value.size())));
            }
        }
        return review_fields;
    }
}

