package blufi.espressif.response;

import blufi.espressif.params.BlufiParameter;

public class BlufiStatusResponse {
    private int mOpMode = -1;

    private int mSoftAPSecurity = -1;
    private int mSoftAPConnCount = -1;
    private int mSoftAPMaxConnCount = -1;
    private int mSoftAPChannel = -1;
    private String mSoftAPPassword = null;
    private String mSoftAPSSID = null;

    private int mStaConnecionStatus = -1;
    private String mStaBSSID = null;
    private String mStaSSID = null;
    private String mStaPassword = null;

    public int getOpMode() {
        return mOpMode;
    }

    public void setOpMode(int mode) {
        mOpMode = mode;
    }

    public int getSoftAPConnectionCount() {
        return mSoftAPConnCount;
    }

    public void setSoftAPConnectionCount(int count) {
        mSoftAPConnCount = count;
    }

    public int getSoftAPMaxConnectionCount() {
        return mSoftAPMaxConnCount;
    }

    public void setSoftAPMaxConnectionCount(int maxCount) {
        mSoftAPMaxConnCount = maxCount;
    }

    public int getSoftAPSecurity() {
        return mSoftAPSecurity;
    }

    public void setSoftAPSecrity(int secrity) {
        mSoftAPSecurity = secrity;
    }

    public int getSoftAPChannel() {
        return mSoftAPChannel;
    }

    public void setSoftAPChannel(int channel) {
        mSoftAPChannel = channel;
    }

    public String getSoftAPPassword() {
        return mSoftAPPassword;
    }

    public void setSoftAPPassword(String password) {
        mSoftAPPassword = password;
    }

    public String getSoftAPSSID() {
        return mSoftAPSSID;
    }

    public void setSoftAPSSID(String ssid) {
        mSoftAPSSID = ssid;
    }

    public int getStaConnectionStatus() {
        return mStaConnecionStatus;
    }

    public boolean isStaConnectWifi() {
        return mStaConnecionStatus == 0;
    }

    public void setStaConnectionStatus(int status) {
        mStaConnecionStatus = status;
    }

    public String getStaBSSID() {
        return mStaBSSID;
    }

    public void setStaBSSID(String bssid) {
        mStaBSSID = bssid;
    }

    public String getStaSSID() {
        return mStaSSID;
    }

    public void setStaSSID(String ssid) {
        mStaSSID = ssid;
    }

    public String getStaPassword() {
        return mStaPassword;
    }

    public void setStaPassword(String password) {
        mStaPassword = password;
    }

    public String generateValidInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("op mode : ");
        switch (mOpMode) {
            case BlufiParameter.OP_MODE_NULL:
                sb.append("NULL");
                break;
            case BlufiParameter.OP_MODE_STA:
                sb.append("STA");
                break;
            case BlufiParameter.OP_MODE_SOFTAP:
                sb.append("SOFTAP");
                break;
            case BlufiParameter.OP_MODE_STASOFTAP:
                sb.append("Sta/Softap");
                break;
        }
        sb.append('\n');

        switch (mStaConnecionStatus) {
            case 0:
                sb.append("Sta connect wifi now");
                break;
            default:
                sb.append("Sta disconnect wifi now");
                break;
        }
        sb.append('\n');
        if (mStaBSSID != null) {
            sb.append("Sta connect wifi bssid: ").append(mStaBSSID).append('\n');
        }
        if (mStaSSID != null) {
            sb.append("Sta connect wifi ssid: ").append(mStaSSID).append('\n');
        }
        if (mStaPassword != null) {
            sb.append("Sta connect wifi password: ").append(mStaPassword).append('\n');
        }

        switch (mSoftAPSecurity) {
            case BlufiParameter.SOFTAP_SECURITY_OPEN:
                sb.append("SoftAP security: ").append("OPEN").append('\n');
                break;
            case BlufiParameter.SOFTAP_SECURITY_WEP:
                sb.append("SoftAP security: ").append("WEP").append('\n');
                break;
            case BlufiParameter.SOFTAP_SECURITY_WPA:
                sb.append("SoftAP security: ").append("WPA").append('\n');
                break;
            case BlufiParameter.SOFTAP_SECURITY_WPA2:
                sb.append("SoftAP security: ").append("WPA2").append('\n');
                break;
            case BlufiParameter.SOFTAP_SECURITY_WPA_WPA2:
                sb.append("SoftAP security: ").append("WPA/WPA2").append('\n');
                break;
        }
        if (mSoftAPSSID != null) {
            sb.append("SoftAP ssid: ").append(mSoftAPSSID).append('\n');
        }
        if (mSoftAPPassword != null) {
            sb.append("SoftAP password: ").append(mSoftAPPassword).append('\n');
        }
        if (mSoftAPChannel >= 0) {
            sb.append("SoftAP channel: ").append(mSoftAPChannel).append('\n');
        }
        if (mSoftAPMaxConnCount > 0) {
            sb.append("SoftAP connection limit: ").append(mSoftAPMaxConnCount).append('\n');
        }
        if (mSoftAPConnCount >= 0) {
            sb.append("SoftAP current connection: ").append(mSoftAPConnCount).append('\n');
        }

        return sb.toString();
    }
}
