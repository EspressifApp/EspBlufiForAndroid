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
        StringBuilder sb = new StringBuilder();
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
     * @return true if data end with suffix
     */
    private boolean endsWith(byte[] data, byte[] suffix) {
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
     * @return true if the data is null or emtpy
     */
    public static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    /**
     * Merge the byte arrays.
     *
     * @return a new merged array
     */
    public static byte[] mergeBytes(byte[]... bytesArray) {
        int resultLen = 0;
        for (byte[] data : bytesArray) {
            resultLen += data.length;
        }

        byte[] result = new byte[resultLen];
        int offset = 0;
        for (byte[] data : bytesArray) {
            System.arraycopy(data, 0, result, offset, data.length);
            offset += data.length;
        }

        return  result;
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
