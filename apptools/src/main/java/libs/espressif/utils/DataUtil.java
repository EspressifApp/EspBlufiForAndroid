package libs.espressif.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataUtil {

    public static void printBytes(byte[] bytes) {
        printBytes(bytes, 20);
    }

    /**
     * Print byte array row by row.
     */
    public static void printBytes(byte[] bytes, int colCount) {
        StringBuilder sb = new StringBuilder(":\n");
        for (int i = 0; i < colCount; i++) {
            if (i < 10) {
                sb.append(0);
            }
            sb.append(i).append('\t');
        }
        sb.append('\n');

        for (int i = 0; i < colCount; i++) {
            sb.append("-\t");
        }
        sb.append('\n');

        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toHexString(bytes[i] & 0xff)).append('\t');
            if (i % colCount == (colCount - 1)) {
                sb.append("| ").append(i / colCount).append('\n');
            }
        }

        System.out.println(sb.toString());
    }

    public static boolean equals(byte[] data1, byte[] data2) {
        if (data1 == null || data2 == null) {
            return false;
        }

        if (data1.length != data2.length) {
            return false;
        }

        for (int i = 0; i < data1.length; i++) {
            if (data1[i] != data2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convert a hex format byte string to a byte array.
     */
    public static byte[] byteStringToBytes(String string) {
        if (string.length() % 2 != 0) {
            string = "0" + string;
        }
        byte[] result = new byte[string.length() / 2];
        for (int i = 0; i < string.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(string.substring(i, i + 2), 16);
        }
        return result;
    }

    /**
     * Convert a byte arry to a hex format string.
     */
    public static String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String byteStr = String.format(Locale.ENGLISH, "%02x", b);
            sb.append(byteStr);
        }
        return sb.toString();
    }

    /**
     * Convert a Byte list to a byte array
     */
    public static byte[] byteListToArray(List<Byte> list) {
        byte[] result = new byte[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    /**
     * Convert a byte array to a Byte list
     */
    public static List<Byte> byteArrayToList(byte[] bytes) {
        List<Byte> result = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            result.add(b);
        }
        return result;
    }

    /**
     * @return true if two array contain same value
     */
    public static boolean equleBytes(byte[] b1, byte[] b2) {
        if (b1 == null || b2 == null) {
            return false;
        }

        if (b1.length != b2.length) {
            return false;
        }

        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return true if data starts with the prefix
     */
    public static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (prefix[i] != data[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return true if data ends with the suffix
     */
    public static boolean endsWith(byte[] data, byte[] suffix) {
        if (data.length < suffix.length) {
            return false;
        }
        for (int i = 0; i < suffix.length; i++) {
            if (suffix[i] != data[data.length - (suffix.length - i)]) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return true if data starts with the prefix
     */
    public static boolean startsWith(List<Byte> list, byte[] prefix) {
        if (list.size() < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (prefix[i] != list.get(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return true if data ends with the suffix
     */
    public static boolean endsWith(List<Byte> list, byte[] suffix) {
        if (list.size() < suffix.length) {
            return false;
        }
        for (int i = 0; i < suffix.length; i++) {
            if (suffix[i] != list.get(list.size() - (suffix.length - i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return true if the data is null or emtpy
     */
    public static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    /**
     * Merge the bytes arrays.
     *
     * @return a new merged array
     */
    public static byte[] mergeBytes(byte[] bytes, byte[]... moreBytes) {
        int resultLen = bytes.length;
        for (byte[] data : moreBytes) {
            resultLen += data.length;
        }

        byte[] result = new byte[resultLen];

        System.arraycopy(bytes, 0, result, 0, bytes.length);
        int offset = bytes.length;
        for (byte[] data : moreBytes) {
            System.arraycopy(data, 0, result, offset, data.length);
            offset += data.length;
        }

        return result;
    }

    public static byte[] subBytes(byte[] src, int begin) {
        int length = src.length - begin;
        byte[] result = new byte[length];
        System.arraycopy(src, begin, result, 0, length);
        return result;
    }

    public static byte[] subBytes(byte[] src, int begin, int length) {
        byte[] result = new byte[length];
        System.arraycopy(src, begin, result, 0, length);
        return result;
    }

    public static byte[] reverseBytes(byte[] src) {
        byte[] result = new byte[src.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = src[src.length - 1 - i];
        }
        return result;
    }

    public static List<byte[]> splitBytes(byte[] src, int splitLength) {
        List<byte[]> result = new ArrayList<>();
        int offset = 0;
        do {
            byte[] split;
            if (offset + splitLength < src.length) {
                split = subBytes(src, offset, splitLength);
            } else {
                split = subBytes(src, offset);
            }
            result.add(split);

            offset += splitLength;
        } while (offset < src.length);

        return result;
    }

    /**
     * Get a byte array for tlv format
     *
     * @param type data type
     * @param lLength length of the Length
     * @param value data value
     * @return tlv byte array
     */
    public static byte[] getTLV(byte[] type, int lLength, byte[] value) {
        byte[] length = new byte[lLength];
        for (int i = 0; i < length.length; i++) {
            length[i] = (byte) ((value.length >> (i * 8)) & 0xff);
        }

        return mergeBytes(type, length, value);
    }
}
