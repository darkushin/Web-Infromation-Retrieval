package webdata;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class ProductIndex implements Serializable {

    private class ProductInfo  implements Serializable{
        private int stringInfo; // This is either a pointer to the concatenated string, or a prefix size.
        private int reviewId;
        private short spanLength;

        private void readObject(ObjectInputStream inputFile) throws ClassNotFoundException, IOException
        {
            stringInfo = inputFile.readInt();
            reviewId = inputFile.readInt();
            spanLength = inputFile.readShort();
        }

        private void writeObject(ObjectOutputStream outputFile) throws IOException
        {
            outputFile.writeInt(stringInfo);
            outputFile.writeInt(reviewId);
            outputFile.writeShort(spanLength);
        }
    }

    // Indices of data in the input array
    public static int POINTER_INDEX = 0;
    public static int PREFIXL_INDEX = 1;
    public static int REVIEWID_INDEX = 2;
    public static int SPANLENGTH_INDEX = 3;
    public static int WORD_LENGTH = 10;

    private ArrayList<ProductInfo> data;
    private String dictString;
    private int dictBytes;
    private int k;

    public ProductIndex(int k) {
        this.data = new ArrayList<>();
        this.dictString = null;
        this.dictBytes = 0;
        this.k = k;
    }

    /**
     * Insert the given data into the list of products and the given concatenated string.
     */
    public void insertData(List<List<Integer>> inData, String concatString) {
        dictString = concatString;
        int offset = 0;
        for (List<Integer> entry : inData) {
            ProductInfo pf = new ProductInfo();
            pf.reviewId = entry.get(REVIEWID_INDEX);
            pf.spanLength = entry.get(SPANLENGTH_INDEX).shortValue();
            if (offset == 0) {
                pf.stringInfo = entry.get(POINTER_INDEX);
            } else {
                pf.stringInfo = entry.get(PREFIXL_INDEX);
            }
            offset++;
            offset = offset % k;
            data.add(pf);
        }
        this.dictBytes = this.dictString.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Retrieve the string word of the product at the given index.
     */
    public String getWordAt(int index) {
        int blockStart = index - (index % k);
        int startStringPtr = data.get(blockStart).stringInfo;
        // Add the first word of the block
        StringBuilder str = new StringBuilder(dictString.substring(startStringPtr, startStringPtr +  WORD_LENGTH));
        int read = WORD_LENGTH;  // Tracks how much was read from the string
        int offset = 0;
        while (blockStart + offset != index) {
            offset++;
            int prefixLength = data.get(blockStart + offset).stringInfo;
            str.delete(prefixLength, str.length());
            str.append(dictString, startStringPtr + read, startStringPtr + read + WORD_LENGTH - prefixLength);
            read += WORD_LENGTH - prefixLength;
        }
        return str.toString();
    }

    /**
     * Search the given string in the productIndex dictionary, using binary search.
     */
    public int search(String str) {
        int high = data.size() / k;
        int low = 0;
        int cur_block = high / 2;
        while (low < high) {
            int cmp = str.compareTo(getWordAt(k * cur_block));
            if (cmp < 0) {
                high = cur_block - 1;  // str is in a previous block
            } else if (cmp > 0) {
                // Checks the first word of the next block (if it exists)
                if (k * (cur_block + 1) < data.size() && str.compareTo(getWordAt(k * (cur_block + 1))) < 0) {
                    // str is in inside this block
                    break;
                }
                low = cur_block + 1;  // str is in a higher block
            } else {
                return k * cur_block;  //  str is the first word in this block
            }
            cur_block = (high + low) / 2;
        }

        // Search the block for str
        int blockStart = k * cur_block;
        for (int offset = 0; offset < k && blockStart + offset < data.size(); offset++) {
            if (getWordAt(blockStart + offset).equals(str)) {  // A bit wasteful, but..
                return blockStart + offset;
            }
        }
        return -1;
    }

    private void readObject(ObjectInputStream inputFile) throws ClassNotFoundException, IOException
    {
        k = inputFile.readInt();
        dictBytes = inputFile.readInt();
        dictString = new String(inputFile.readNBytes(dictBytes), StandardCharsets.UTF_8);
        data = (ArrayList<ProductInfo>) inputFile.readObject();
    }

    private void writeObject(ObjectOutputStream outputFile) throws IOException
    {
        outputFile.writeInt(k);
        outputFile.writeInt(this.dictBytes);
        outputFile.writeBytes(this.dictString);
        outputFile.writeObject(data);
    }

    public int getReviewId(int index) {
        return data.get(index).reviewId;
    }

    public int getReviewSpan(int index) {
        return data.get(index).spanLength;
    }
}
