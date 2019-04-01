package libs.espressif.app;

import android.os.Build.VERSION_CODES;

import static android.os.Build.VERSION.SDK_INT;

public class SdkUtil {
    /**
     * Check if the device is running on the Android ICE_CREAM_SANDWICH(14) release or newer.
     *
     * @return true if I APIs are available for use.
     */
    public static boolean isAtLeastI_14() {
        return SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * Check if the device is running on the Android ICE_CREAM_SANDWICH_MR1(15) release or newer.
     *
     * @return true if I_MR1 APIs are available for use.
     */
    public static boolean isAtLeastIMR1_15() {
        return SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
    }

    /**
     * Check if the device is running on the Android JELLY_BEAN(16) release or newer.
     *
     * @return true if J APIs are available for use.
     */
    public static boolean isAtLeastJ_16() {
        return SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Check if the device is running on the Android JELLY_BEAN_MR1(17) release or newer.
     *
     * @return true if J_MR1 APIs are available for use.
     */
    public static boolean isAtLeastJMR1_17() {
        return SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * Check if the device is running on the Android JELLY_BEAN_MR2(18) release or newer.
     *
     * @return true if J_MR2 APIs are available for use.
     */
    public static boolean isAtLeastJMR2_18() {
        return SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * Check if the device is running on the Android KITKAT(19) release or newer.
     *
     * @return true if K APIs are available for use.
     */
    public static boolean isAtLeastK_19() {
        return SDK_INT >= VERSION_CODES.KITKAT;
    }

    /**
     * Check if the device is running on the Android KITKAT_WATCH(20) release or newer.
     *
     * @return true if K_W APIs are available for use.
     */
    public static boolean isAtLeastKW_20() {
        return SDK_INT >= VERSION_CODES.KITKAT_WATCH;
    }

    /**
     * Check if the device is running on the Android LOLLIPOP(21) release or newer.
     *
     * @return true if L APIs are available for use.
     */
    public static boolean isAtLeastL_21() {
        return SDK_INT >= VERSION_CODES.LOLLIPOP;
    }

    /**
     * Check if the device is running on the Android LOLLIPOP_MR1(22) release or newer.
     *
     * @return true if L_MR1 APIs are available for use.
     */
    public static boolean isAtLeastLMR1_22() {
        return SDK_INT >= VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * Check if the device is running on the Android M(23) release or newer.
     *
     * @return true if M APIs are available for use.
     */
    public static boolean isAtLeastM_23() {
        return SDK_INT >= VERSION_CODES.M;
    }

    /**
     * Check if the device is running on the Android N(24) release or newer.
     *
     * @return true if N APIs are available for use.
     */
    public static boolean isAtLeastN_24() {
        return SDK_INT >= VERSION_CODES.N;
    }

    /**
     * Check if the device is running on the Android N(25) release or newer.
     *
     * @return true if N_MR1 APIs are available for use.
     */
    public static boolean isAtLeastNMR1_25() {
        return SDK_INT >= VERSION_CODES.N_MR1;
    }

    /**
     * Check if the device is running on the Android O(26) release or newer.
     *
     * @return true if O APIs are available for use.
     */
    public static boolean isAtLeastO_26() {
        return SDK_INT >= VERSION_CODES.O;
    }

    /**
     * Check if the device is running on the Android O(27) release or newer.
     *
     * @return true if O APIs are available for use.
     */
    public static boolean isAtLeastOMR1_27() {
        return SDK_INT >= VERSION_CODES.O_MR1;
    }

    /**
     * Check if the device is running on the Android P(28) release or newer.
     *
     * @return true if P APIs are available for use.
     */
    public static boolean isAtLeastP_28() {
        return SDK_INT >= 28;
    }
}
