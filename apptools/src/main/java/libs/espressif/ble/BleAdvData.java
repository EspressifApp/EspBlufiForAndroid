package libs.espressif.ble;

import libs.espressif.utils.DataUtil;

public class BleAdvData {
    private int mType;
    private byte[] mData;

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setData(byte[] data) {
        mData = data;
    }

    public byte[] getData() {
        return mData;
    }

    @Override
    public String toString() {
        return String.format("Adv type=%02x, data=%s", mType, (mData == null ? null : DataUtil.bytesToString(mData)));
    }
}
