package webdata;

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
        HashMap<Integer, Double> scores= new HashMap<>();
        int toks = 0;
        while (query.hasMoreElements()) {
            String token = query.nextElement();
            toks++;
            double smooth = (1 - lambda) * (double) ir.getTokenCollectionFrequency(token) / ir.getTokenSizeOfReviews();
            HashMap<Integer, Double> tokenScores = getDocScores(token, "languageModel");
            for (Map.Entry<Integer, Double> ent : tokenScores.entrySet()) {
//                double val = lambda * ent.getValue() + smooth;
                double val = Math.log(lambda * ent.getValue() + smooth);
//                scores.merge(ent.getKey(), Math.pow(val, toks), (x, y) -> x*val);
                scores.merge(ent.getKey(), val * toks, (x, y) -> x + val);
            }
        }
        scores.replaceAll((key, v) -> Math.exp(v));
        return kHighestScores(scores, k);
    }

    /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */
    public Collection<String> productSearch(Enumeration<String> query, int k) {
        return null;
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

    private Enumeration<Integer> kHighestScores(HashMap<Integer, Double> scores, int k){
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(scores.entrySet());
        list.sort((x, y) -> {
            int cmp = y.getValue().compareTo(x.getValue());
            if (cmp == 0) {
                return x.getKey().compareTo(y.getKey());
            } else {
                return cmp;
            }
        });
        ArrayList<Integer> result = new ArrayList<>();
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
        rs.languageModelSearch(Collections.enumeration(Arrays.asList("what", "the", "hell")), 0.4, 10);
    }
}