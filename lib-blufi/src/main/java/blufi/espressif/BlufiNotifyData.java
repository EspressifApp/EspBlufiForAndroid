package blufi.espressif;

import java.io.ByteArrayOutputStream;

class BlufiNotifyData {
    private int mTypeValue;
    private int mPkgType;
    private int mSubType;

    private int mFrameCtrlValue;

    private final ByteArrayOutputStream mDataOS;

    BlufiNotifyData() {
        mDataOS = new ByteArrayOutputStream();
    }

    int getType() {
        return mTypeValue;
    }

    void setType(int typeValue) {
        mTypeValue = typeValue;
    }

    int getPkgType() {
        return mPkgType;
    }

    void setPkgType(int pkgType) {
        mPkgType = pkgType;
    }

    int getSubType() {
        return mSubType;
    }

    void setSubType(int subType) {
        mSubType = subType;
    }

    int getFrameCtrl() {
        return mFrameCtrlValue;
    }

    void setFrameCtrl(int frameCtrl) {
        mFrameCtrlValue = frameCtrl;
    }

    void addData(byte[] bytes, int offset) {
        mDataOS.write(bytes, offset, bytes.length - offset);
    }

    byte[] getDataArray() {
        return mDataOS.toByteArray();
    }
}
