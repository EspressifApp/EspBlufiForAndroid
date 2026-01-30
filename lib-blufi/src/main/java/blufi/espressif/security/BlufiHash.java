package blufi.espressif.security;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BlufiHash {
    public static byte[] getMD5Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            Log.e("BlufiHash", "getMD5Bytes error", e);
        }

        return null;
    }

    public static byte[] getSHA256Bytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data);
            return digest.digest();
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            Log.e("BlufiHash", "getSHA256Bytes error", e);
        }

        return null;
    }
}
