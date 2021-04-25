package webdata;

import java.io.IOException;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TokensIndex implements Serializable {
    public class TokenInfo implements Serializable{
        private short stringInfo; // This is either a pointer to the concatenated string, or a prefix size.
        private short frequency;
        private short collectionFrequency;
        private short length;
        private int invertedIndexPtr;

        public short getFrequency(){ return frequency;}
        public short getCollectionFrequency(){ return collectionFrequency;}
        public int getInvertedIdxPtr(){ return invertedIndexPtr;}

        private void readObject(ObjectInputStream inputFile) throws IOException, ClassNotFoundException {
            stringInfo = inputFile.readShort();
            frequency = inputFile.readShort();
            collectionFrequency = inputFile.readShort();
            length = inputFile.readShort();
            invertedIndexPtr = inputFile.readInt();
        }

        private void writeObject(ObjectOutputStream outputFile) throws IOException {
            outputFile.writeShort(stringInfo);
            outputFile.writeShort(frequency);
            outputFile.writeShort(collectionFrequency);
            outputFile.writeShort(length);
            outputFile.writeInt(invertedIndexPtr);
        }
    }

    // Indices of data in the input array
    public static int POINTER_INDEX = 0;
    public static int PREFIX_INDEX = 1;
    public static int TOKEN_LENGTH = 2;
//    public static int FREQUENCY_INDEX = 0;

    private ArrayList<TokenInfo> data;
    private String dictString;
    private int numTokens;  // the total number of tokens in the collection, including repetitions
    private int k;
    private String dir;
    private RandomAccessFile invertedIndexFile;

    public TokensIndex(int k, String dir) {
        this.data = new ArrayList<>();
        this.dictString = null;
        this.numTokens = 0;
        this.k = k;
        this.dir = dir;
        createRandomAccessFile();
    }

    /**
     * Create a new RandomAccessFile to write the tokens inverted index into.
     * If such a file already exists, first remove it.
     */
    private void createRandomAccessFile(){
        try {
            File file = new File(this.dir + "/tokens_inverted_index.txt");
            if (file.exists()){
                file.delete();
            }
            this.invertedIndexFile = new RandomAccessFile(this.dir + "/tokens_inverted_index.txt", "rw");
        } catch (FileNotFoundException e) {
            System.out.println("Error occurred while creating the tokens_inverted_index file");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Insert the given information of token properties into the index format that should be saved.
     * @param tokensData the data of the token containing its pointer/prefix length and token length as created in the KFront class.
     * @param tokensVals a list of reviewId-num appearances of reviews containing every token and the number the token appeared in every review.
     * @param concatString the concatenated string of all tokens in the collection, created by the KFront class.
     */
    public void insertData(List<List<Integer>> tokensData, ArrayList<ArrayList<Integer>> tokensVals, String concatString){
        dictString = concatString;
        int offset = 0;
        for (int i=0; i< tokensData.size(); i++){
            List<Integer> tokenData = tokensData.get(i);
            List<Integer> tokenVal = tokensVals.get(i);
            TokenInfo token = new TokenInfo();
            token.length = tokenData.get(TOKEN_LENGTH).shortValue();
            token.frequency = (short) (tokenVal.size() / 2);
            token.collectionFrequency = (short) subListVals(tokenVal, "even").stream().mapToInt(Integer::intValue).sum();
            numTokens += token.getCollectionFrequency();
            try {
                token.invertedIndexPtr = (int) this.invertedIndexFile.getFilePointer();
            } catch (IOException e) {
                System.out.println("Error occurred while accessing the tokens_inverted_index file");
                e.printStackTrace();
                System.exit(1);
            }
            saveInvertedIndex(tokenVal);
            if (offset == 0){
                token.stringInfo = tokenData.get(POINTER_INDEX).shortValue();
            } else {
                token.stringInfo = tokenData.get(PREFIX_INDEX).shortValue();
            }
            offset++;
            offset = offset % k;
            this.data.add(token);
        }
    }

    /**
     * Create a sub list of the given list containing only the odd/even elements in the array
     * @param inputList the list that should be sliced
     * @param type can be `odd` or `even`
     * @return a List of integers containing only the elements in odd/even indices of the input array 
     */
    private List<Integer> subListVals(List<Integer> inputList, String type){
        int first = 0;
        List<Integer> subList = new ArrayList<>();
        if (type.equals("even")){ first = 1; }
        for (int i = first; i < inputList.size(); i = i + 2){
            subList.add(inputList.get(i));
        }
        return subList;
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

            StringBuilder stringCodes = new StringBuilder();
            for (int num : valsList) {
                String code = Encoding.deltaEncode(num);
                stringCodes.append(code);
            }
            byte[] codeBytes = Encoding.toByteArray(stringCodes.toString());
            this.invertedIndexFile.write(codeBytes);
        } catch (Exception e){
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


    @Serial
    private void readObject(ObjectInputStream inputFile) throws IOException, ClassNotFoundException {
        k = inputFile.readInt();
        dictString = inputFile.readUTF();
        numTokens = inputFile.readInt();
        data = (ArrayList<TokenInfo>) inputFile.readObject();

    }

    @Serial
    private void writeObject(ObjectOutputStream outputFile) throws IOException {
        outputFile.writeInt(this.k);
        outputFile.writeUTF(this.dictString);
        outputFile.writeInt(this.numTokens);
        outputFile.writeObject(this.data);
    }
}
