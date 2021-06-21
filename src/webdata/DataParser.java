package webdata;

import java.io.*;
import java.util.*;


public class DataParser {
    public class Review{
        private String text;
        private String productId;
        private String score;
        private String helpfulness;

        public String getText() {
            return text;
        }

        public String getProductId() {
            return productId;
        }

        public String getHelpfulness() {
            return helpfulness;
        }

        public String getScore() {
            return score;
        }

        public void setHelpfulness(String helpfulness) {
            this.helpfulness = helpfulness;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public void setScore(String score) {
            this.score = score;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * Given product review data, parses the data and creates a new list where each entry i contains hashmap with the fields
     * of the review, i.e: productId->value, score->value, helpfulness->value, text->value.
     * inputFile is the path to the file containing the review data
     */
    public List<Review> parseData(List<String> rawReviews){
        ArrayList<Review> allReviews = new ArrayList<>();
        for (String review: rawReviews){
            allReviews.add(parseReview(review));
        }
        return allReviews;
    }

    /**
     * Given a single review, parse the review and return a Review object, containing all relevant information from the
     * given review, i.e. productId, score, helpfulness and text.
     */
    public Review parseReview(String review){
        ArrayList<String> fields = new ArrayList<>(Arrays.asList(review.split("review/")));
        Review parsedReview = new Review();

        parsedReview.setProductId(fields.get(0).split(": ")[1].split("product/")[0]);
        for (int i=1; i<fields.size(); i++){
            String field = fields.get(i);
            List<String> fieldValue = Arrays.asList(field.split(": "));
            if (fieldValue.get(0).equals("text")) {
                parsedReview.setText(String.join(": ", fieldValue.subList(1, fieldValue.size())));
            } else if (fieldValue.get(0).equals("helpfulness")) {
                parsedReview.setHelpfulness(fieldValue.get(1));
            } else if (fieldValue.get(0).equals("score")) {
                parsedReview.setScore(fieldValue.get(1));
            }
        }
        return parsedReview;
    }

    public Review parseReview(ArrayList<String> review){
        Review parsedReview = new Review();
        StringBuilder text = new StringBuilder();
        boolean readingText = false;
        for (String line : review){
            if (readingText && !line.equals("")) {
                text.append(" ");
                text.append(line);
                continue;
            }
            int prefix = line.indexOf("/");
            int delim = line.indexOf(":");
            if (prefix == -1 || delim == -1 || delim < prefix) {
                continue;
            }
            String field = line.substring(prefix + 1, delim);
            if (field.equals("text")){
                text.append(line.substring(delim + 2));
                readingText = true;
            } else if (field.equals("productId")) {
                parsedReview.setProductId(line.substring(delim + 2));
            } else if (field.equals("helpfulness")) {
                parsedReview.setHelpfulness(line.substring(delim + 2));
            } else if (field.equals("score")) {
                parsedReview.setScore(line.substring(delim + 2));
            }
        }
        parsedReview.setText(text.toString());
        return parsedReview;
    }
}

