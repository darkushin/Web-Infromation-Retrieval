package webdata;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class Encoding {

    /**
     * Encode the given number using gamma encoding.
     * The encoded output is a string representing the bytes of the number.
     */
    public static String gammaEncode(int num) {
        String offset = Integer.toBinaryString(num + 1);
        return "1".repeat(offset.length() - 1) + "0" + offset.substring(1);
    }

    /**
     * Encode the given number using delta encoding.
     * The encoded output is a string representing the bytes of the number.
     */
    public static String deltaEncode(int num) {
        String offset = Integer.toBinaryString(num + 1);
        return gammaEncode(offset.length() - 1) + offset.substring(1);
    }

    /**
     * Decode the given string, which represents a binary sequence using gamma code.
     */
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

    /**
     * Decode the given string, which represents a binary sequence using delta code.
     */
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

    /**
     * Decode the given byte array, using gamma code.
     */
    public static ArrayList<Integer> gammaDecode(byte[] code) {
        return gammaDecode(byteToString(code));
    }

    /**
     * Decode the given byte array, using delta code.
     */
    public static ArrayList<Integer> deltaDecode(byte[] code) {
        return deltaDecode(byteToString(code));
    }

    /**
     * Convert the given string representing a bit sequence of numbers to a byte array.
     */
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

    /**
     * Convert the given byte array to a string representing the bits of the byte array.
     */
    public static String byteToString(byte[] encoding) {
        StringBuilder s = new StringBuilder();
        for (byte b : encoding) {
            String binary = Integer.toBinaryString(Byte.toUnsignedInt(b));
            s.append("0".repeat(8 - binary.length())); // toBinaryString removes leading 0's
            s.append(binary);
        }
        return s.toString();
    }

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
                if (numAsBytes[j] != 0) {
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

    /**
     * Convert the given list of id-1, num-appearances-1, id-2, num-appearances-2... where the ids are given by their
     * differences to a list where every id entry are the full id number.
     */
    public static List<Integer> diffToIds(List<Integer> vals){
        for (int i = 2; i < vals.size() - 1; i = i + 2){
            vals.set(i, vals.get(i) + vals.get(i - 2));
        }
        return vals;
    }

    /*
    # TODO: for testing, remove later.
     */
//    public static void main(String args[]) {
//        // Gamma test
//        String encoding = gammaEncode(56) + gammaEncode(13) + gammaEncode(1) +
//                gammaEncode(2) + gammaEncode(1);
//        byte[] bytearr = toByteArray(encoding);
//        String fromarr = byteToString(bytearr);
//        ArrayList<Integer> nums = gammaDecode(bytearr);
//        System.out.println(nums);
//
//        // Delta test
//        encoding = deltaEncode(57) + deltaEncode(375);
//        bytearr = toByteArray(encoding);
//        fromarr = byteToString(bytearr);
//        nums = deltaDecode(bytearr);
//        System.out.println(nums);
//
//        // Group Varint test
//        int[] numgroup = {900000, 20, 450, 9};
//        byte[] bytegroup = groupVarintEncode(numgroup);
//        int[] decoded = groupVarintDecode(bytegroup);
//        System.out.println("Done.");
//    }
}

