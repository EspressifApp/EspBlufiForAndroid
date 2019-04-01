package libs.espressif.log;

import android.util.Log;

public class EspLog {
    private final String mTag;
    private Level mLevel = Level.V;

    /**
     * @param cls The tag will use simple name of the cls.
     */
    public EspLog(Class cls) {
        mTag = String.format("[%s]", cls.getSimpleName());
    }

    /**
     * Set the print lowest level. It will set {@link Level#NIL} if the level is null.
     *
     * @param level The lowest level can print log.
     */
    public void setLevel(Level level) {
        if (level == null) {
            mLevel = Level.NIL;
        } else {
            mLevel = level;
        }
    }

    /**
     * Send a {@link Level#V} log message.
     *
     * @param msg The message you would like logged.
     */
    public void v(String msg) {
        if (mLevel.ordinal() <= Level.V.ordinal()) {
            Log.v(mTag, msg);
        }
    }

    /**
     * Send a {@link Level#V} log message.
     *
     * @param msg The message you would like logged.
     */
    public void d(String msg) {
        if (mLevel.ordinal() <= Level.D.ordinal()) {
            Log.d(mTag, msg);
        }
    }

    /**
     * Send a {@link Level#I} log message.
     *
     * @param msg The message you would like logged.
     */
    public void i(String msg) {
        if (mLevel.ordinal() <= Level.I.ordinal()) {
            Log.i(mTag, msg);
        }
    }

    /**
     * Send a {@link Level#W} log message.
     *
     * @param msg The message you would like logged.
     */
    public void w(String msg) {
        if (mLevel.ordinal() <= Level.W.ordinal()) {
            Log.w(mTag, msg);
        }
    }

    /**
     * Send a {@link Level#E} log message.
     *
     * @param msg The message you would like logged.
     */
    public void e(String msg) {
        if (mLevel.ordinal() <= Level.E.ordinal()) {
            Log.e(mTag, msg);
        }
    }

    /**
     * The level allow logged
     */
    public enum Level {
        V, D, I, W, E, NIL
    }
}
