package webdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Encoding {
    /**
     * Encode the given list of numbers using Group-Varint-Encoding. The first byte of the resulting byte array
     * holds the number of bytes required to decode each of the next four numbers.
     */
    public static byte[] groupVarintEncode(int[] nums) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        byte length = 0;
        for (int i = 0; i < nums.length; i++) {
            byte[] numAsBytes =  ByteBuffer.allocate(4).putInt(nums[i]).array();
            byte numLength = -1;
            for (int j = 0; j < numAsBytes.length; j++) {
                if (numAsBytes[j] != 0 || numLength >= 0) {
                    out.write(numAsBytes[j]);
                    numLength++;
                } else if (j == numAsBytes.length - 1 & numLength == -1) {
                    out.write(numAsBytes[j]);
                    numLength++;
                }
            }
            length = (byte) (length | (byte) (numLength << 2*(3 - i)));
        }
        byte[] output = out.toByteArray();
        output[0] = length;
        return output;
    }

    public static byte[] groupVarEncodeMultiple(List<Integer> nums) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i;
        for (i=0; i + 3 < nums.size(); i=i+4) {
            try {
                baos.write(groupVarintEncode(new int[]{nums.get(i), nums.get(i + 1), nums.get(i + 2), nums.get(i + 3)}));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        int[] remainder = new int[4];
        for (int j=0;j < nums.size() - i; j++) {
            remainder[j] = nums.get(i+j);
        }
        try {
            baos.write(groupVarintEncode(remainder));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return baos.toByteArray();
    }

    /**
     * Decode the given byte array to numbers, using Group-Varing-Encoding.
     */
    public static int[] groupVarintDecode(byte[] encoding) {
        byte lengths = encoding[0];
        int[] output = new int[4];
        int bytesRead = 1;
        for (int i = 0; i < 4; i++) {
            int bytesToRead = 1 + (lengths >> (2 * (3 - i))) & 3;
            byte[] o = new byte[bytesToRead];
            for (int b = 0; b < bytesToRead; b++) {
                o[b] = encoding[bytesRead + b];
            }
            bytesRead += bytesToRead;
            output[i] = new BigInteger(1, o).intValue();
        }
        return output;
    }

    public static ArrayList<Integer> groupVarDecodeMultiple(byte[] encoding) {
        ArrayList<Integer> ret = new ArrayList<>();
        int bytesRead = 0;
        while (bytesRead < encoding.length) {
            byte lengths = encoding[bytesRead];
            bytesRead++;
            for (int i = 0; i < 4; i++) {
                int bytesToRead = 1 + (lengths >> (2 * (3 - i))) & 3;
                byte[] o = new byte[bytesToRead];
                for (int b = 0; b < bytesToRead; b++) {
                    o[b] = encoding[bytesRead + b];
                }
                bytesRead += bytesToRead;
                ret.add(new BigInteger(1, o).intValue());
            }
        }
        for (int j=0; j < 4; j++) {
            if (ret.get(ret.size() - 1) != 0) {
                break;
            }
            ret.remove(ret.size() - 1);
        }
        return ret;
    }

    /**
     * Convert the given list of id-1, num-appearances-1, id-2, num-appearances-2... where the ids are given by their
     * differences to a list where every id entry is the full id number.
     */
    public static List<Integer> diffToIds(List<Integer> vals){
        for (int i = 2; i < vals.size() - 1; i = i + 2){
            vals.set(i, vals.get(i) + vals.get(i - 2));
        }
        return vals;
    }
}

