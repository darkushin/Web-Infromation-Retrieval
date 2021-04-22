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

    public KFront() {
        table = new LinkedList<>();
        concatString = null;
    }

    public KFront(List<List<Integer>> outputTable) {
        table = outputTable;
        concatString = null;
    }

    public String getConcatString() {
        return concatString;
    }

    public List<List<Integer>> getTable() {
        return table;
    }

    public void createKFront(int k, List<String> strings) {
        StringBuilder sBuilder = new StringBuilder();
        int offset = 0;
        String prevString = null;
        for (String str : strings) {
            ArrayList<Integer> entry = new ArrayList<>();  // Table entries: 0-pointer, 1-prefix size
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
