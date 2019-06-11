package blufi.espressif;

import java.io.ByteArrayOutputStream;

public class BlufiNotiData {
    private int mTypeValue;
    private int mPkgType;
    private int mSubType;

    private int mFrameCtrlValue;

    private ByteArrayOutputStream mDataOS;

    public BlufiNotiData() {
        mDataOS = new ByteArrayOutputStream();
    }

    public int getType() {
        return mTypeValue;
    }

    public void setType(int typeValue) {
        mTypeValue = typeValue;
    }

    public int getPkgType() {
        return mPkgType;
    }

    public void setPkgType(int pkgType) {
        mPkgType = pkgType;
    }

    public int getSubType() {
        return mSubType;
    }

    public void setSubType(int subType) {
        mSubType = subType;
    }

    public int getFrameCtrl() {
        return mFrameCtrlValue;
    }

    public void setFrameCtrl(int frameCtrl) {
        mFrameCtrlValue = frameCtrl;
    }

    public void addData(byte b) {
        mDataOS.write(b);
    }

    public void addData(byte[] bytes) {
        mDataOS.write(bytes, 0, bytes.length);
    }

    public byte[] getDataArray() {
        return mDataOS.toByteArray();
    }

    public void clear() {
        mTypeValue = 0;
        mPkgType = 0;
        mSubType = 0;
        mDataOS.reset();
    }
}
