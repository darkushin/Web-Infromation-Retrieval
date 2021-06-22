package webdata;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewIndex implements Serializable{
    public class ReviewInfo implements Serializable {
        public byte[] encodedInfo;
        public byte score;

        private void readObject(ObjectInputStream inputFile) throws ClassNotFoundException, IOException
        {
            encodedInfo = (byte[]) inputFile.readUnshared();
            score = inputFile.readByte();
        }

        private void writeObject(ObjectOutputStream outputFile) throws IOException
        {
            outputFile.writeUnshared(encodedInfo);
            outputFile.writeByte(score);
        }
    }

    public static int PRODUCTID_INDEX = 0;
    public static int HELPFNUM_INDEX = 1;
    public static int HELPFDNOM_INDEX = 2;
    public static int REVIEWLENGTH_INDEX = 3;
    public static int SCORE_INDEX = 4;

    private ArrayList<ReviewInfo> data;

    /**
     * insert the given data into the list containing all the information of reviews.
     */
    public void insertData(ArrayList<ReviewInfo> inData) {
        this.data = inData;
    }

    /**
     * Check if the given review id is valid, i.e. larger than 0 and smaller than #reviews.
     */
    public boolean isReviewIdValid(int reviewId) {
        return reviewId >= 0 && reviewId <= (data.size() - 1);
    }

    private int[] getEntry(int reviewId) {
        return Encoding.groupVarintDecode(data.get(reviewId).encodedInfo);
    }

    public int getProductNum(int reviewId) {
        return getEntry(reviewId)[PRODUCTID_INDEX];
    }

    public int getScore(int reviewId) {
        return data.get(reviewId).score;
    }

    public int getHelpfulnessNumerator(int reviewId) {
        return getEntry(reviewId)[HELPFNUM_INDEX];
    }

    public int getHelpfulnessDenominator(int reviewId) {
        return getEntry(reviewId)[HELPFDNOM_INDEX];
    }

    public int getLength(int reviewId) {
        return getEntry(reviewId)[REVIEWLENGTH_INDEX];
    }

    public int getNumReview(){
        return data.size();
    }

    private void readObject(ObjectInputStream inputFile) throws ClassNotFoundException, IOException
    {
        data = (ArrayList<ReviewInfo>) inputFile.readUnshared();
    }

    private void writeObject(ObjectOutputStream outputFile) throws IOException
    {
        outputFile.writeUnshared(this.data);
    }
}
