package webdata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Class for creating a (k-1) in k Front Coding dictionary.
 */
public class KFront {
    private List<List<Integer>> table;
    private String concatString;
    private boolean saveLength;

    public KFront() {
        table = new LinkedList<>();
        concatString = null;
        saveLength = false;
    }

    public KFront(List<List<Integer>> outputTable) {
        table = outputTable;
        concatString = null;
        saveLength = false;
    }

    public KFront(boolean saveLength) {
        table = new LinkedList<>();
        concatString = null;
        this.saveLength = saveLength;
    }

    public String getConcatString() {
        return concatString;
    }

    public List<List<Integer>> getTable() {
        return table;
    }

    /**
     * Adds the given values to the given entry in the table
     * @param i the entry of the table to set the values to
     * @param vals the values that should be added to the table at the i-th entry
     */
    public void setValue(int i, List<Integer> vals){
        table.set(i, vals);
    }

    public void createKFront(int k, List<String> strings) {
        StringBuilder sBuilder = new StringBuilder();
        int offset = 0;
        String prevString = null;
        for (String str : strings) {
            ArrayList<Integer> entry = new ArrayList<>();  // Table entries: 0-pointer, 1-prefix size, 2-string length(Optional)
            if (offset == 0) {
                entry.add(sBuilder.length());  // Pointer to string
                entry.add(null);  // Prefix size
                sBuilder.append(str);
            } else {
                String commonPref = findCommonPrefix(str, prevString);
                entry.add(null);  // Pointer to string
                entry.add(commonPref.length());  // Prefix size
                sBuilder.append(str.substring(commonPref.length()));
            }
            prevString = str;
            offset++;
            if (offset == k) {
                offset = 0;
                prevString = null;
            }
            if (this.saveLength){
                entry.add(str.length());  // Save the Str length
            }
            table.add(entry);
        }
        concatString = sBuilder.toString();
    }

    private String findCommonPrefix(String s1, String s2){
        StringBuilder commonPrefix = new StringBuilder();
        int i = 0;
        while (i < s1.length() && i < s2.length() && s1.charAt(i) == s2.charAt(i)){
            commonPrefix.append(s1.charAt(i));
            i++;
        }
        return commonPrefix.toString();
    }
}
