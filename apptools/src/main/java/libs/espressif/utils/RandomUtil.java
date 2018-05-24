package libs.espressif.utils;

import java.util.Random;

public class RandomUtil {

    /**
     * map int to String
     *
     * @param i int value
     * @return 0-9: "0"-"9" 10-25: "a"-"z"
     */
    private static String map(int i) {
        if (i < 10)
            return Integer.toString(i);
        else {
            char c = (char) ('a' + i - 10);
            return Character.toString(c);
        }
    }

    /**
     * Generate a target length String, the value range is "0-9" and "a-z"
     *
     * @param length except string length
     * @return target length random string
     */
    public static String randomString(int length) {
        Random random = new Random();
        String token = "";
        for (int i = 0; i < length; i++) {
            int x = random.nextInt(36);
            token += map(x);
        }
        return token;
    }

    /**
     * Generate a target length byte array.
     *
     * @param length except array length
     * @return random byte array
     */
    public static byte[] randomBytes(int length) {
        Random random = new Random();
        byte[] result = new byte[length];
        random.nextBytes(result);
        return result;
    }
}
