import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class DataParser {
    public static void main(String[] args)
            throws FileNotFoundException
    {
        File file = new File("/Users/darkushin/Downloads/1000.txt");  // todo: change to input args
        Scanner sc = new Scanner(file);

        // Split the input file to different reviews
        sc.useDelimiter("product/productId: ");  // todo: check about the space after productId

        String review = null;

        // initialize a list that will contain at line i the fields of the ith review
        ArrayList<Hashtable<String, String>> all_reviews = new ArrayList<Hashtable<String, String>>();
        while (sc.hasNext()) {
            review = sc.next();
            all_reviews.add(__parse_review(review));
        }

        System.out.println("daniel");
    }

    /**
     * Given a single review, parse the review and return a hash table containing only the relevant fields of the
     * review, i.e: productId, score, helpfulness, text.
     * @param review: the review that should be parsed.
     * @return a hash table where the keys are the relevant fields mentioned above and their corresponding values.
     */
    private static Hashtable<String, String> __parse_review(String review){
        List<String> INTEREST_FIELDS = Arrays.asList("productId", "score", "helpfulness", "text");
        List<String> fields = Arrays.asList(review.split("review/"));
        Hashtable<String, String> review_fields = new Hashtable<String, String>();

        review_fields.put("productId", fields.get(0));
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

