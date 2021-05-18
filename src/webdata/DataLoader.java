package webdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
            public String next() {
                return readSingleReview();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
