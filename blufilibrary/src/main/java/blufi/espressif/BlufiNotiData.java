package blufi.espressif;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BlufiNotiData {
    private int mTypeValue;
    private int mPkgType;
    private int mSubType;

    private int mFrameCtrlValue;

    private int mSequence;

    private LinkedList<Byte> mDataList;

    public BlufiNotiData() {
        mDataList = new LinkedList<>();
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
        mDataList.add(b);
    }

    public void addData(byte[] bytes) {
        for (byte b : bytes) {
            mDataList.add(b);
        }
    }

    public byte[] getDataArray() {
        byte[] result = new byte[mDataList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = mDataList.get(i);
        }

        return result;
    }

    public List<Byte> getDataList() {
        return new ArrayList<>(mDataList);
    }

    public void clear() {
        mTypeValue = 0;
        mPkgType = 0;
        mSubType = 0;
        mDataList.clear();
    }
}
