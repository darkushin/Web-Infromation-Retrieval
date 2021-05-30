package webdata;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class Analysis {
    private IndexReader indexReader;
    private TokensIndex tokensIndex;
    private ArrayList<String> randomTokens;
    private long getReviewsWithTokenTime;
    private long getTokenFrequencytTime;

    public Analysis(IndexReader indexReader){
        this.indexReader = indexReader;
        this.tokensIndex = indexReader.tokenIndex;
        this.randomTokens = new ArrayList<>();
        this.getReviewsWithTokenTime = 0;
        this.getTokenFrequencytTime = 0;

        getRandomTokens(100);
        measureGetReviewsWithToken();
        measureTokenFrequencyTime();
    }

    private void measureGetReviewsWithToken() {
        long start = new Date().getTime();
        for (String token: this.randomTokens){
            indexReader.getReviewsWithToken(token);
        }
        long end = new Date().getTime();
        this.getReviewsWithTokenTime = (end - start);
    }

    private void measureTokenFrequencyTime() {
        long start = new Date().getTime();
        for (String token: this.randomTokens){
            indexReader.getTokenFrequency(token);
        }
        long end = new Date().getTime();
        this.getTokenFrequencytTime = (end - start);
    }

    /**
     * Get n random tokens from the index.
     */
    public void getRandomTokens(int n){
        Random random = new Random();
        for (int i=0; i < n; i++){
            int randIndex = random.nextInt(this.tokensIndex.data.size()); // get random index
            this.randomTokens.add(tokensIndex.getWordAt(randIndex));
        }
    }

    public static void main(String[] args) {
        IndexReader indexReader = new IndexReader("./Data_index");
        Analysis analysis = new Analysis(indexReader);
        System.out.println("getReviewsWithToken runtime: " + analysis.getReviewsWithTokenTime + "(ms)");
        System.out.println("getTokenFrequency runtime: " + analysis.getTokenFrequencytTime + "(ms)");
    }
}