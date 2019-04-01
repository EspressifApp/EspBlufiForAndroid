package libs.espressif.security;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class EspPBKDF2 {
    public static byte[] getPBKDF2Bytes(char[] password, byte[] salt, int iterationCount, int keyLength) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterationCount, keyLength);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
}
