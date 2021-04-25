package webdata;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class ReviewIndex {
    private class ReviewInfo {
        private byte[] encodedInfo;
        private byte score;

        @Serial
        private void readObject(ObjectInputStream inputFile) throws ClassNotFoundException, IOException
        {
            encodedInfo = (byte[]) inputFile.readObject();
            score = inputFile.readByte();
        }

        @Serial
        private void writeObject(ObjectOutputStream outputFile) throws IOException
        {
            outputFile.writeObject(encodedInfo);
            outputFile.writeByte(score);
        }
    }

    public static int PRODUCTID_INDEX = 0;
    public static int HELPFNUM_INDEX = 1;
    public static int HELPFDNOM_INDEX = 2;
    public static int REVIEWLENGTH_INDEX = 3;
    public static int SCORE_INDEX = 4;

    private ArrayList<ReviewInfo> data;

    public void insertData(List<List<Integer>> inData) {
        data = new ArrayList<>();
        for (List<Integer> entry : inData) {
            ReviewInfo rI = new ReviewInfo();
            int[] info = new int[4];
            byte score = (byte) entry.get(4).intValue();
            info[PRODUCTID_INDEX] = entry.get(PRODUCTID_INDEX);
            info[HELPFNUM_INDEX] = entry.get(HELPFNUM_INDEX);
            info[HELPFDNOM_INDEX] = entry.get(HELPFDNOM_INDEX);
            info[REVIEWLENGTH_INDEX] = entry.get(REVIEWLENGTH_INDEX);
            rI.encodedInfo = Encoding.groupVarintEncode(info);
            rI.score = score;
            data.add(rI);
        }
    }

    public int[] getEntry(int reviewId) {
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
}
