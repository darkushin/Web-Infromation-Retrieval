package webdata;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;

public class DeltaEncoder {
    public static String gamma_encode(int num){
        String offset = Integer.toBinaryString(num);
        return "1".repeat(offset.length() - 1) + "0" + offset.substring(1);
    }

    public static String delta_encode(int num){
        String offset = Integer.toBinaryString(num);
        return gamma_encode(offset.length()) + offset.substring(1);
    }

    public static byte[] toByteArray(String encoding) {
        byte[] ret = new BigInteger(encoding, 2).toByteArray();
        if (ret.length * 8 == encoding.length() + 8) {
            return Arrays.copyOfRange(ret, 1, ret.length);
        } else {
            return ret;
        }
    }

    public static BitSet toBitSet(String encoding) {
        return BitSet.valueOf(toByteArray(encoding));
    }

    public static void main(String[] args){
        int[] vals = new int[]{43, 2, 10, 200};
        LinkedList<BitSet> codes = new LinkedList<>();

        for (int x : vals) {
            String gc = DeltaEncoder.delta_encode(x);
            byte[] arr = new BigInteger(gc, 2).toByteArray();
            codes.add(BitSet.valueOf(arr));
        }

        for (BitSet bits : codes) {
            byte[] code = bits.toByteArray();
            continue;
        }
        System.out.println("Hi.");
    }
}

