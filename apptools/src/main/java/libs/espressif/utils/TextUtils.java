package libs.espressif.utils;

import java.util.Locale;

public final class TextUtils {
    public static boolean isEmpty(CharSequence text) {
        return android.text.TextUtils.isEmpty(text);
    }

    public static String format(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }
}
