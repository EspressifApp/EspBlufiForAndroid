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
        StringBuilder info = new StringBuilder();
        info.append("OpMode: ");
        switch (mOpMode) {
            case BlufiParameter.OP_MODE_NULL:
                info.append("NULL");
                break;
            case BlufiParameter.OP_MODE_STA:
                info.append("Station");
                break;
            case BlufiParameter.OP_MODE_SOFTAP:
                info.append("SoftAP");
                break;
            case BlufiParameter.OP_MODE_STASOFTAP:
                info.append("Station/SoftAP");
                break;
        }
        info.append('\n');

        switch (mOpMode) {
            case BlufiParameter.OP_MODE_STA:
            case BlufiParameter.OP_MODE_STASOFTAP:
                if (isStaConnectWifi()) {
                    info.append("Station connect Wi-Fi now");
                } else {
                    info.append("Station disconnect Wi-Fi now");
                }
                info.append('\n');
                if (mStaBSSID != null) {
                    info.append("Station connect Wi-Fi bssid: ").append(mStaBSSID).append('\n');
                }
                if (mStaSSID != null) {
                    info.append("Station connect Wi-Fi ssid: ").append(mStaSSID).append('\n');
                }
                if (mStaPassword != null) {
                    info.append("Station connect Wi-Fi password: ").append(mStaPassword).append('\n');
                }
                break;
        }

        switch (mOpMode) {
            case BlufiParameter.OP_MODE_SOFTAP:
            case BlufiParameter.OP_MODE_STASOFTAP:
                switch (mSoftAPSecurity) {
                    case BlufiParameter.SOFTAP_SECURITY_OPEN:
                        info.append("SoftAP security: ").append("OPEN").append('\n');
                        break;
                    case BlufiParameter.SOFTAP_SECURITY_WEP:
                        info.append("SoftAP security: ").append("WEP").append('\n');
                        break;
                    case BlufiParameter.SOFTAP_SECURITY_WPA:
                        info.append("SoftAP security: ").append("WPA").append('\n');
                        break;
                    case BlufiParameter.SOFTAP_SECURITY_WPA2:
                        info.append("SoftAP security: ").append("WPA2").append('\n');
                        break;
                    case BlufiParameter.SOFTAP_SECURITY_WPA_WPA2:
                        info.append("SoftAP security: ").append("WPA/WPA2").append('\n');
                        break;
                }
                if (mSoftAPSSID != null) {
                    info.append("SoftAP ssid: ").append(mSoftAPSSID).append('\n');
                }
                if (mSoftAPPassword != null) {
                    info.append("SoftAP password: ").append(mSoftAPPassword).append('\n');
                }
                if (mSoftAPChannel >= 0) {
                    info.append("SoftAP channel: ").append(mSoftAPChannel).append('\n');
                }
                if (mSoftAPMaxConnCount > 0) {
                    info.append("SoftAP max connection: ").append(mSoftAPMaxConnCount).append('\n');
                }
                if (mSoftAPConnCount >= 0) {
                    info.append("SoftAP current connection: ").append(mSoftAPConnCount).append('\n');
                }
                break;
        }

        return info.toString();
    }
}
