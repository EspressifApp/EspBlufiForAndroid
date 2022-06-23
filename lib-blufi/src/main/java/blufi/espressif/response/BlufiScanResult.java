package blufi.espressif.response;

import java.util.Locale;

public class BlufiScanResult {
    public static final int TYPE_WIFI = 0x01;

    private int mType;
    private String mSsid;
    private int mRssi;

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setSsid(String ssid) {
        mSsid = ssid;
    }

    public String getSsid() {
        return mSsid;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public int getRssi() {
        return mRssi;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "ssid: %s, rssi: %d", mSsid, mRssi);
    }
}
