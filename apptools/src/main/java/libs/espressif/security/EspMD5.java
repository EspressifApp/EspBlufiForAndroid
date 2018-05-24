package libs.espressif.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EspMD5 {

    public static byte[] getMD5Byte(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getMD5String(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(data);
            byte[] result = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                int number = b & 0xff;
                String str = Integer.toHexString(number);
                if (str.length() == 1) {
                    sb.append("0");
                }
                sb.append(str);
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            e.printStackTrace();
        }

        return "";
    }
}
