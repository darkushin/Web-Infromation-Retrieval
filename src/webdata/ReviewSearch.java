package webdata;

import javax.lang.model.type.ArrayType;
import java.util.*;
import java.lang.Math.*;

public class ReviewSearch {
    private IndexReader ir;
    /**
     * Constructor
     */
    public ReviewSearch(IndexReader iReader) {
        this.ir = iReader;
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the vector space ranking function lnn.ltc (using the
     *       SMART notation)
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> vectorSpaceSearch(Enumeration<String> query, int k) {
        // read entire query and compute query scores:
        HashMap<String, Integer> fullQuery = new HashMap<>();
        while (query.hasMoreElements()) {
            String token = query.nextElement();
            if (fullQuery.containsKey(token)){
                fullQuery.put(token, fullQuery.get(token) + 1);
            } else {
                fullQuery.put(token, 1);
            }
        }
        HashMap<String, Double> queryScores = this.computeTokenQueryScore(fullQuery);

        HashMap<Integer, Double> scores= new HashMap<>();
        for (String token: fullQuery.keySet()){
            HashMap<Integer, Double> docScores = this.getDocScores(token, "vectorSpace");
            double tokenQueryScore = queryScores.get(token);
            for (int doc: docScores.keySet()){
                double curScore = tokenQueryScore * docScores.get(doc);
                if (scores.containsKey(doc)) {
                    scores.put(doc, scores.get(doc) + curScore);
                } else {
                    scores.put(doc, curScore);
                }
            }
        }
        // sort the map and return the ids of the k highest scores:
        return kHighestScores(scores, k);
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the language model ranking function, smoothed using a
     * mixture model with the given value of lambda
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> languageModelSearch(Enumeration<String> query,double lambda, int k) {
        HashMap<Integer, Double> scores = new HashMap<>();
        double total_smooth = 1;
        while (query.hasMoreElements()) {
            String token = query.nextElement();
            double smooth = (1 - lambda) * (double) ir.getTokenCollectionFrequency(token) / ir.getTokenSizeOfReviews();
            HashMap<Integer, Double> tokenScores = getDocScores(token, "languageModel");

            // Update existing keys
            for (Map.Entry<Integer, Double> ent : scores.entrySet()) {
                if (tokenScores.containsKey(ent.getKey())){
                    scores.put(ent.getKey(), ent.getValue() * (lambda * tokenScores.get(ent.getKey()) + smooth));
                } else {
                    scores.put(ent.getKey(), ent.getValue() * smooth);
                }
            }
            Set<Integer> tokenScoresKeys = tokenScores.keySet();
            tokenScoresKeys.removeAll(scores.keySet());

            // Add remaining, new keys
            for (int key : tokenScoresKeys) {
                scores.put(key, (lambda * tokenScores.get(key) + smooth) * total_smooth);
            }
            total_smooth *= smooth;
        }
        return kHighestScores(scores, k);
    }

    /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */
    public Collection<String> productSearch(Enumeration<String> query, int k) {
        Enumeration<Integer> relevantReviews = this.vectorSpaceSearch(query, this.ir.getNumberOfReviews());
        HashMap<String, ArrayList<ArrayList<Integer>>> products = new HashMap<>();
        int reviewRank = 1;
        while (relevantReviews.hasMoreElements()) {
            int reviewId = relevantReviews.nextElement();
            String productId = ir.getProductId(reviewId);
            if (!products.containsKey(productId)){
                products.put(productId, new ArrayList<>());
            }
            products.get(productId).add(new ArrayList<>(Arrays.asList(reviewId, reviewRank)));
            reviewRank++;
        }
        HashMap<String, Double> productRelevance = new HashMap<>();
        for (Map.Entry<String, ArrayList<ArrayList<Integer>>> product: products.entrySet()){
            productRelevance.put(product.getKey() ,this.getProductRelevance(product.getValue()));
        }

        HashMap<String, Double> productQuality = new HashMap<>();
        for (String product: products.keySet()){
            productQuality.put(product, this.getProductQuality(product));
        }

        double alpha = 0.5;
        HashMap<String, Double> productScores = new HashMap<>();
        for (String product: productRelevance.keySet()){
            productScores.put(product, alpha*productRelevance.get(product) + (1-alpha)*productQuality.get(product));
        }
        Enumeration<String> topProducts = kHighestScores(productScores, k);
        return Collections.list(topProducts);
    }

    private double getProductRelevance(ArrayList<ArrayList<Integer>> reviews) {

        for (ArrayList<Integer> vals : reviews) {
            int reviewScore = ir.getReviewScore(vals.get(0));
            int reviewHelpfulnessNumerator = ir.getReviewHelpfulnessNumerator(vals.get(0));
            int reviewHelpfulnessDenominator = ir.getReviewHelpfulnessDenominator(vals.get(0));
            int reviewLength = ir.getReviewLength(vals.get(0));
        }

        return 1;
    }

    private double getProductQuality(String p) {
        return 1;
    }

    private HashMap<String, Double> computeTokenQueryScore(HashMap<String, Integer> query) {
        HashMap<String, Double> scores = new HashMap<>();

        // compute the tf and idf values of every token:
        for (String token: query.keySet()) {
            double tf = 1 + Math.log10(query.get(token));
            double df = Math.log10((double) ir.getNumberOfReviews() / ir.getTokenFrequency(token));
            scores.put(token, tf*df);
        }

        // compute the norm of the vector:
        double vectorNorm = 0;
        for (double score: scores.values()){
            vectorNorm += Math.pow(score, 2);
        }

        // normalize the values by dividing in the vector's norm:
        for (String token: scores.keySet()){
            double normalizedScore = scores.get(token) / vectorNorm;
            scores.put(token, normalizedScore);
        }
        return scores;
    }

    private HashMap<Integer, Double> getDocScores(String token, String model) {
        HashMap<Integer, Double> scores = new HashMap<>();
        Enumeration<Integer> docFreqs = ir.getReviewsWithToken(token);
        while (docFreqs.hasMoreElements()) {
            int docId = docFreqs.nextElement();
            int freq = docFreqs.nextElement();
            double val = 0;
            if (model.equals("languageModel")) {
                val = ((double) freq / ir.getReviewLength(docId));
            } else if (model.equals("vectorSpace")){
                val = ((double) 1 + Math.log10(freq));
            } else {
                System.out.println("Please provide the name of the search for computing docScores. Options are: [languageModel, vectorSpace]");
                System.exit(1);
            }
            scores.put(docId, val);
        }
        return scores;
    }

    private <T extends Comparable<T>> Enumeration<T> kHighestScores(HashMap<T, Double> scores, int k){
        List<Map.Entry<T, Double>> list = new ArrayList<>(scores.entrySet());
        list.sort((x, y) -> {
            int cmp = y.getValue().compareTo(x.getValue());
            if (cmp == 0) {
                return x.getKey().compareTo(y.getKey());
            } else {
                return cmp;
            }
        });
        ArrayList<T> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, list.size()); i++) {
            result.add(list.get(i).getKey());
        }
        return Collections.enumeration(result);
    }

    public static void main(String[] args) {
        String dir = "./Data_Index";
//        IndexWriter iw = new IndexWriter();
//        iw.write("./1000.txt", dir);

        IndexReader ir = new IndexReader(dir);
        ReviewSearch rs = new ReviewSearch(ir);
        rs.productSearch(Collections.enumeration(Arrays.asList("dog")), 10);
    }
}