package libs.espressif.utils;

import java.util.TimeZone;

public class TimeUtil {
    public static long getUTCTime(long time) {
        TimeZone timeZone = TimeZone.getDefault();
        return time - timeZone.getRawOffset();
    }

    public static long getSystemTime(long utcTime) {
        TimeZone timeZone = TimeZone.getDefault();
        return utcTime + timeZone.getRawOffset();
    }
}
