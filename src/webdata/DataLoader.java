package webdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DataLoader implements Iterable<String> {
    private BufferedReader br;
    private StringBuilder stringBuffer;

    public DataLoader(String inputFile) throws FileNotFoundException {
        br = new BufferedReader(new FileReader(inputFile));
        stringBuffer = new StringBuilder();
    }

    public String readSingleReview() {
        String line;
        try {
            while((line = br.readLine()) != null) {
                if (line.contains("product/productId") && stringBuffer.length() != 0) {
                    String ret = stringBuffer.toString();
                    stringBuffer = new StringBuilder(line);
                    return ret;
                }
                stringBuffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return stringBuffer.toString();
    }

    public List<String> readMultipleReviews(int num) {
        LinkedList<String> ret = new LinkedList<>();
        for (int i = 0; i < num; i++) {
            ret.add(readSingleReview());
        }
        return ret;
    }

    public Iterator<String> iterator() {
        return new Iterator<>() {

            private int currentIndex = 0;

            @Override
            public boolean hasNext(){
                try {
                    return br.ready();
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public String next() {
                return readSingleReview();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

//    public static void main(String[] args) {
//        DataLoader dl = null;
//        try {
//            dl = new DataLoader("./100.txt");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//
//        for (int i = 0; i < 5; i++) {
//            String s = dl.readSingleReview();
//            System.out.println(s);
//        }
//
//        ArrayList<String> readd = new ArrayList<>(dl.readMultipleReviews(10));
//        System.out.println(readd.size());
//
//        for (String s : dl) {
//            readd.add(s);
//        }
//        System.out.println(readd.size());
//    }
}
