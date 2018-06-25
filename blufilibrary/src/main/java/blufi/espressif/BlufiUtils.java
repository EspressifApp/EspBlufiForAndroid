package blufi.espressif;

public class BlufiUtils {
    public static final String VERSION = "2.0.1";
    public static final int[] SUPPORT_BLUFI_VERSION = {1, 2};

    public static void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
