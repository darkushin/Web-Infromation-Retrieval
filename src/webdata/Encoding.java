package webdata;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

public class Encoding {
    //TODO Check that adding 1 (to encode 0) doesn't mess things up.
    public static String gammaEncode(int num) {
        String offset = Integer.toBinaryString(num + 1);
        return "1".repeat(offset.length() - 1) + "0" + offset.substring(1);
    }

    public static String deltaEncode(int num) {
        String offset = Integer.toBinaryString(num + 1);
        return gammaEncode(offset.length() - 1) + offset.substring(1);
    }

    public static ArrayList<Integer> gammaDecode(String encoding) {
        ArrayList<Integer> output = new ArrayList<>();
        int bitsRead = 0;
        while (bitsRead < encoding.length()) {
            int length = encoding.substring(bitsRead).indexOf('0'); // Find the first 0
            int offsetLoc = bitsRead + length + 1;
            output.add(Integer.parseInt("1" + encoding.substring(offsetLoc, offsetLoc + length), 2) - 1);
            bitsRead = offsetLoc + length;
        }
        return output;
    }

    public static ArrayList<Integer> deltaDecode(String encoding) {
        ArrayList<Integer> output = new ArrayList<>();
        int bitsRead = 0;
        while (bitsRead < encoding.length()) {
            int length = encoding.substring(bitsRead).indexOf('0'); // Find the first 0
            int offsetLoc = bitsRead + length + 1;
            int actualLength = Integer.parseInt("1" + encoding.substring(offsetLoc, offsetLoc + length), 2);
            bitsRead = offsetLoc + length;

            output.add(Integer.parseInt("1" + encoding.substring(bitsRead, bitsRead + actualLength - 1), 2) - 1);
            bitsRead += actualLength - 1;
        }
        return output;
    }

    public static ArrayList<Integer> gammaDecode(byte[] code) {
        return gammaDecode(byteToString(code));
    }

    public static ArrayList<Integer> deltaDecode(byte[] code) {
        return deltaDecode(byteToString(code));
    }

    public static byte[] toByteArray(String encoding) {
        // Pad 0s to the nearest multiple of 8
        String padded = encoding + "0".repeat((int) Math.ceil((float) encoding.length() / 8) * 8 - encoding.length());
        byte[] ret = new BigInteger(padded, 2).toByteArray();
        if (ret.length * 8 == padded.length() + 8) {
            return Arrays.copyOfRange(ret, 1, ret.length);
        } else {
            return ret;
        }
    }

    public static String byteToString(byte[] encoding) {
        StringBuilder s = new StringBuilder();
        for (byte b : encoding) {
            String binary = Integer.toBinaryString(Byte.toUnsignedInt(b));
            s.append("0".repeat(8 - binary.length())); // toBinaryString removes leading 0's
            s.append(binary);
        }
        return s.toString();
    }

    public static BitSet toBitSet(String encoding) {
        return BitSet.valueOf(toByteArray(encoding));
    }

    public static byte[] groupVarintEncode(int[] nums) {
        ByteBuffer bb = ByteBuffer.allocate(20);
        for (int i = 0; i < nums.length; i++) {
            bb.putInt(nums[i]);
        }
        return null;
    }

    public static int[] groupVarintDecode(byte[] encoding) {
        return null;
    }

    /*
    # TODO: for testing, remove later.
     */
    public static void main(String args[]) {
        // Gamma test
        String encoding = gammaEncode(56) + gammaEncode(13) + gammaEncode(1) +
                gammaEncode(2) + gammaEncode(1);
        byte[] bytearr = toByteArray(encoding);
        String fromarr = byteToString(bytearr);
        ArrayList<Integer> nums = gammaDecode(bytearr);
        System.out.println(nums);

        // Delta test
        encoding = deltaEncode(57) + deltaEncode(375);
        bytearr = toByteArray(encoding);
        fromarr = byteToString(bytearr);
        nums = deltaDecode(bytearr);
        System.out.println(nums);
    }
}

