package blufi.espressif.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BlufiMD5 {
    public static byte[] getMD5Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }
}
