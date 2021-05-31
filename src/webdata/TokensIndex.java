package webdata;

import java.io.IOException;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TokensIndex implements Serializable {
    public class TokenInfo implements Serializable{
        private int stringInfo; // This is either a pointer to the concatenated string, or a prefix size.
        private int frequency;
        private int collectionFrequency;
        private short length;
        private long invertedIndexPtr;

        public int getFrequency(){ return frequency;}
        public int getCollectionFrequency(){ return collectionFrequency;}
        public long getInvertedIdxPtr(){ return invertedIndexPtr;}

        private void readObject(ObjectInputStream inputFile) throws IOException, ClassNotFoundException {
            stringInfo = inputFile.readInt();
            frequency = inputFile.readInt();
            collectionFrequency = inputFile.readInt();
            length = inputFile.readShort();
            invertedIndexPtr = inputFile.readLong();
        }

        private void writeObject(ObjectOutputStream outputFile) throws IOException {
            outputFile.writeInt(stringInfo);
            outputFile.writeInt(frequency);
            outputFile.writeInt(collectionFrequency);
            outputFile.writeShort(length);
            outputFile.writeLong(invertedIndexPtr);
        }
    }

    // Indices of data in the input array
    public static int POINTER_INDEX = 0;
    public static int PREFIX_INDEX = 1;
    public static int TOKEN_LENGTH = 2;
    private static final String TOKEN_INVERTED_INDEX_FILE = "token_inverted_index.txt";


    public ArrayList<TokenInfo> data;
    private String dictString;
    private int dictBytes;
    private int numTokens;  // the total number of tokens in the collection, including repetitions
    private int k;
    private String dir;
    private RandomAccessFile invertedIndexFile;

    public TokensIndex(int k, String dir) {
        this.data = new ArrayList<>();
        this.dictString = null;
        this.dictBytes = 0;
        this.numTokens = 0;
        this.k = k;
        this.dir = dir;
        createOutputFile();
    }

    /**
     * Create a new RandomAccessFile to write the tokens inverted index into.
     * If such a file already exists, first remove it.
     */
    private void createOutputFile(){
        try {
            File file = new File(this.dir + "/" + TOKEN_INVERTED_INDEX_FILE);
            if (file.exists()){
                file.delete();
            }
            this.invertedIndexFile = new RandomAccessFile(this.dir + "/" + TOKEN_INVERTED_INDEX_FILE, "rw");
        } catch (FileNotFoundException e) {
            System.out.println("Error occurred while creating the tokens_inverted_index file");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void insertData(List<List<Integer>> tokensData, String concatString, String pairsFilename) {
        dictString = concatString;
        PairsLoader pl = new PairsLoader(pairsFilename);
        int offset = 0;
        int[] curPair = pl.readPair(); // This should correspond to the first token

        for (int i=0; i< tokensData.size(); i++){
            List<Integer> tokenData = tokensData.get(i);
            TokenInfo token = new TokenInfo();
            ArrayList<Integer> invertedIdx = new ArrayList<>();

            invertedIdx.add(curPair[1]);
            invertedIdx.add(1);
            token.frequency++;
            token.collectionFrequency++;
            int[] nextPair = pl.readPair();
            while (nextPair != null && nextPair[0] == curPair[0]){
                if (nextPair[1] == curPair[1]) { // Token repetition inside the same doc
                    int docFreq = invertedIdx.remove(invertedIdx.size()-1);
                    invertedIdx.add(docFreq + 1);
                } else {
                    invertedIdx.add(nextPair[1]);
                    invertedIdx.add(1);
                    token.frequency++;
                }
                token.collectionFrequency++;
                curPair = nextPair;
                nextPair = pl.readPair();
            }
            curPair = nextPair; // Save the pair for the next token

            try {
                token.invertedIndexPtr = (int) this.invertedIndexFile.getFilePointer();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            saveInvertedIndex(invertedIdx);

            numTokens += token.collectionFrequency;
            token.length = tokenData.get(TOKEN_LENGTH).shortValue();
            if (offset == 0){
                token.stringInfo = tokenData.get(POINTER_INDEX);
            } else {
                token.stringInfo = tokenData.get(PREFIX_INDEX);
            }
            offset++;
            offset = offset % k;
            this.data.add(token);

            token = null;
            invertedIdx = null;
            tokenData = null;
        }
        this.dictBytes = this.dictString.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Encodes the integers given in the integer list using delta encoding, and saves them in the invertedIndexFile.
     * @param valsList a list with number that should be encoded and saved in the inverted index file.
     */
    private void saveInvertedIndex(List<Integer> valsList) {
        try {
            // change the reviewIds (odd indices) to a difference list (except for the first id):
            for (int i = valsList.size()-2; i>0; i = i - 2){
                valsList.set(i, valsList.get(i) - valsList.get(i-2));
            }
            byte[] codeBytes = Encoding.groupVarEncodeMultiple(valsList);
            this.invertedIndexFile.write(codeBytes);
        } catch (Exception e) {
            System.out.println("Error occurred while saving invertedIndex bytes");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * @return get the entire data of all tokens.
     */
    public ArrayList<TokenInfo> get(){
        return data;
    }

    /**
     * get the data of a specific token.
     * @param tokenIndex the index of the token that should be retrieved.
     * @return the information of the token that was queried.
     */
    public TokenInfo get(int tokenIndex){
        return data.get(tokenIndex);
    }

    public int getNumTokens(){
        return numTokens;
    }

    /**
     * Retrieve the string word of the product at the given index.
     */
    public String getWordAt(int index) {
        int blockStart = index - (index % k);
        int startStringPtr = data.get(blockStart).stringInfo;
        int token_length = data.get(blockStart).length;
        // Add the first word of the block
        StringBuilder str = new StringBuilder(dictString.substring(startStringPtr, startStringPtr +  token_length));
        int read = token_length;  // Tracks how much was read from the string
        int offset = 0;
        while (blockStart + offset != index) {
            offset++;
            int prefixLength = data.get(blockStart + offset).stringInfo;
            token_length = data.get(blockStart + offset).length;
            str.delete(prefixLength, str.length());
            str.append(dictString, startStringPtr + read, startStringPtr + read + token_length - prefixLength);
            read += token_length - prefixLength;
        }
        return str.toString();
    }

    /**
     * Search the given string in the tokenIndex dictionary, using binary search.
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

    private void readObject(ObjectInputStream inputFile) throws IOException, ClassNotFoundException {
        k = inputFile.readInt();
        dictBytes = inputFile.readInt();
        dictString = new String(inputFile.readNBytes(dictBytes), StandardCharsets.UTF_8);
        numTokens = inputFile.readInt();
        data = (ArrayList<TokenInfo>) inputFile.readObject();

    }

    private void writeObject(ObjectOutputStream outputFile) throws IOException {
        outputFile.writeInt(this.k);
        outputFile.writeInt(this.dictBytes);
        outputFile.writeBytes(this.dictString);
        outputFile.writeInt(this.numTokens);
        outputFile.writeObject(this.data);
    }
}
