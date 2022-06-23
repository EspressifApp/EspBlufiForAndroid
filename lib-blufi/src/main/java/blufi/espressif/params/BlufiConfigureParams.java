package blufi.espressif.params;

import java.io.Serializable;
import java.util.Locale;

public class BlufiConfigureParams implements Serializable {
    private int mOpMode;

    private String mStaBSSID;
    private byte[] mStaSSIDBytes;
    private String mStaPassword;

    private int mSoftAPSecurity;
    private String mSoftAPSSID;
    private String mSoftAPPassword;
    private int mSoftAPChannel;
    private int mSoftAPMaxConnection;

    public int getOpMode() {
        return mOpMode;
    }

    public void setOpMode(int mode) {
        mOpMode = mode;
    }

    public String getStaBSSID() {
        return mStaBSSID;
    }

    public void setStaBSSID(String bssid) {
        mStaBSSID = bssid;
    }

    public void setStaSSIDBytes(byte[] staSSIDBytes) {
        mStaSSIDBytes = staSSIDBytes;
    }

    public byte[] getStaSSIDBytes() {
        return mStaSSIDBytes;
    }

    public String getStaPassword() {
        return mStaPassword;
    }

    public void setStaPassword(String password) {
        mStaPassword = password;
    }

    public int getSoftAPSecurity() {
        return mSoftAPSecurity;
    }

    public void setSoftAPSecurity(int security) {
        mSoftAPSecurity = security;
    }

    public String getSoftAPSSID() {
        return mSoftAPSSID;
    }

    public void setSoftAPSSID(String ssid) {
        mSoftAPSSID = ssid;
    }

    public String getSoftAPPassword() {
        return mSoftAPPassword;
    }

    public void setSoftAPPAssword(String password) {
        mSoftAPPassword = password;
    }

    public int getSoftAPChannel() {
        return mSoftAPChannel;
    }

    public void setSoftAPChannel(int channel) {
        mSoftAPChannel = channel;
    }

    public int getSoftAPMaxConnection() {
        return mSoftAPMaxConnection;
    }

    public void setSoftAPMaxConnection(int connectionCount) {
        mSoftAPMaxConnection = connectionCount;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "op mode = %d, sta bssid = %s, sta ssid = %s, sta password = %s, softap security = %d," +
                        " softap ssid = %s, softap password = %s, softap channel = %d, softap max connection = %d",
                mOpMode,
                mStaBSSID,
                mStaSSIDBytes == null ? null : new String(mStaSSIDBytes),
                mStaPassword,
                mSoftAPSecurity,
                mSoftAPSSID,
                mSoftAPPassword,
                mSoftAPChannel,
                mSoftAPMaxConnection);
    }
}
