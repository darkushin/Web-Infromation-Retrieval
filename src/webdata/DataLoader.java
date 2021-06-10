package webdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class DataLoader implements Iterable<ArrayList<String>> {
    private BufferedReader br;
    private ArrayList<String> reviewStrings;

    public DataLoader(String inputFile) throws FileNotFoundException {
        br = new BufferedReader(new FileReader(inputFile));
        reviewStrings = new ArrayList<>();
    }

    public ArrayList<String> readSingleReview() {
        String line;
        try {
            while((line = br.readLine()) != null) {
                if (line.contains("product/productId") && reviewStrings.size() != 0) {
                    ArrayList<String> ret = reviewStrings;
                    reviewStrings = new ArrayList<String>();
                    reviewStrings.add(line);
                    return ret;
                }
                reviewStrings.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return reviewStrings;
    }

    public Iterator<ArrayList<String>> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext(){
                try {
                    br.mark(1);
                    int i = br.read();
                    br.reset();
                    return (i != -1);
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public ArrayList<String> next() {
                return readSingleReview();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
