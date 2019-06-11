package blufi.espressif;

import blufi.espressif.params.BlufiParameter;

public class FrameCtrlData {
    private static final int FRAME_CTRL_POSITION_ENCRYPTED = 0;
    private static final int FRAME_CTRL_POSITION_CHECKSUM = 1;
    private static final int FRAME_CTRL_POSITION_DATA_DIRECTION = 2;
    private static final int FRAME_CTRL_POSITION_REQUIRE_ACK = 3;
    private static final int FRAME_CTRL_POSITION_FRAG = 4;

    private int mValue;

    public FrameCtrlData(int frameCtrlValue) {
        mValue = frameCtrlValue;
    }

    private boolean check(int position) {
        return ((mValue >> position) & 1) == 1;
    }

    public boolean isEncrypted() {
        return check(FRAME_CTRL_POSITION_ENCRYPTED);
    }

    public boolean isChecksum() {
        return check(FRAME_CTRL_POSITION_CHECKSUM);
    }

    public boolean requireAck() {
        return check(FRAME_CTRL_POSITION_REQUIRE_ACK);
    }

    public boolean hasFrag() {
        return check(FRAME_CTRL_POSITION_FRAG);
    }

    public static int getFrameCTRLValue(boolean encrypted, boolean checksum, int direction, boolean requireAck, boolean frag) {
        int frame = 0;
        if (encrypted) {
            frame = frame | (1 << FRAME_CTRL_POSITION_ENCRYPTED);
        }
        if (checksum) {
            frame = frame | (1 << FRAME_CTRL_POSITION_CHECKSUM);
        }
        if (direction == BlufiParameter.DIRECTION_INPUT) {
            frame = frame | (1 << FRAME_CTRL_POSITION_DATA_DIRECTION);
        }
        if (requireAck) {
            frame = frame | (1 << FRAME_CTRL_POSITION_REQUIRE_ACK);
        }
        if (frag) {
            frame = frame | (1 << FRAME_CTRL_POSITION_FRAG);
        }

        return frame;
    }
}
