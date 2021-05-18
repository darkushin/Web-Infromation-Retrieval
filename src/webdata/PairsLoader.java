package webdata;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class PairsLoader {
    ObjectInputStream ois = null;

    public PairsLoader(String file) {
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int[] readPair() {
        int[] pair = new int[2];
        try {
            pair[0] = ois.readInt();
            pair[1] = ois.readInt();
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return pair;
    }
}
