package webdata;

import java.util.*;

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
        return null;
    }

    private HashMap<Integer, Double> getDocScores(String token) {
        HashMap<Integer, Double> scores = new HashMap<>();
        Enumeration<Integer> docFreqs = ir.getReviewsWithToken(token);
        while (docFreqs.hasMoreElements()) {
            int docId = docFreqs.nextElement();
            int freq = docFreqs.nextElement();
            double val = ((double) freq / ir.getReviewLength(docId));
            scores.put(docId, val);
        }
        return scores;
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the language model ranking function, smoothed using a
     * mixture model with the given value of lambda
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> languageModelSearch(Enumeration<String> query,double lambda, int k) {
        HashMap<Integer, Double> scores= new HashMap<>();
        int toks = 0;
        while (query.hasMoreElements()) {
            String token = query.nextElement();
            toks++;
            double smooth = (1 - lambda) * (double) ir.getTokenCollectionFrequency(token) / ir.getTokenSizeOfReviews();
            HashMap<Integer, Double> tokenScores = getDocScores(token);
            for (Map.Entry<Integer, Double> ent : tokenScores.entrySet()) {
                double val = lambda * ent.getValue() + smooth;
                scores.merge(ent.getKey(), Math.pow(val, toks), (x, y) -> x*val);
            }
        }
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(scores.entrySet());
        list.sort(Map.Entry.comparingByValue());
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, list.size()); i++) {
            result.add(list.get(list.size() - i - 1).getKey());
        }
        return Collections.enumeration(result);
    }

    /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */
    public Collection<String> productSearch(Enumeration<String> query, int k) {
        return null;
    }

    public static void main(String[] args) {
        String dir = "./Data_Index";
//        IndexWriter iw = new IndexWriter();
//        iw.write("./1000.txt", dir);

        IndexReader ir = new IndexReader(dir);
        ReviewSearch rs = new ReviewSearch(ir);
        rs.languageModelSearch(Collections.enumeration(Arrays.asList("what", "the", "hell")), 0.4, 10);
    }
}