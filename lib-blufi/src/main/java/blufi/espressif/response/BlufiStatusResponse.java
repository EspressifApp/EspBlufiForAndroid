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

    private int mStaConnectionStatus = -1;
    private String mStaBSSID = null;
    private String mStaSSID = null;
    private String mStaPassword = null;

    final private int mConnectionRssiLimit = -60;

    private int mConnectionMaxRetry = -1;
    private int mConnectionEndReason = -1;
    private int mConnectionRssi = -128;

    private boolean isReasonValid(int reason) {
        return ((reason >= 0 && reason <= 24) || (reason == 53) || (reason >= 200 && reason <= 207));
    }

    private boolean isRssiValid(int rssi) {
        /* define rssi -128 as N/A */
        return (rssi > -128 && rssi <= 127);
    }

    private String getEndInfo() {
        StringBuilder msg = new StringBuilder();

        msg.append("Reason code: ").append(isReasonValid(mConnectionEndReason) ? mConnectionEndReason : "N/A").append(", ");
        msg.append("Rssi: ").append(isRssiValid(mConnectionRssi) ? mConnectionRssi : "N/A").append("\n");

        if (mConnectionEndReason == BlufiParameter.WIFI_REASON_NO_AP_FOUND) {
            msg.append("NO AP FOUND").append("\n");
        } else if (mConnectionEndReason == BlufiParameter.WIFI_REASON_CONNECTION_FAIL) {
            msg.append("AP IN BLACKLIST, PLEASE RETRY").append("\n");
        } else if (isRssiValid(mConnectionRssi)){
            if (mConnectionRssi < mConnectionRssiLimit) {
                msg.append("RSSI IS TOO LOW").append("\n");
            } else if (mConnectionEndReason == BlufiParameter.WIFI_REASON_4WAY_HANDSHAKE_TIMEOUT || mConnectionEndReason == BlufiParameter.WIFI_REASON_HANDSHAKE_TIMEOUT) {
                msg.append("WRONG PASSWORD").append("\n");
            }
        }

        return msg.toString();
    }

    private String getConnectingInfo() {
        StringBuilder msg = new StringBuilder();
        msg.append("Max Retry is ");
        if (mConnectionMaxRetry == -1) {
            msg.append("N/A\n");
        } else {
            msg.append(mConnectionMaxRetry).append("\n");
        }
        return msg.toString();
    }

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
        return mStaConnectionStatus;
    }

    public boolean isStaConnectWifi() {
        return mStaConnectionStatus == 0;
    }

    public void setStaConnectionStatus(int status) {
        mStaConnectionStatus = status;
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

    public void setMaxRetry(int maxRetry) {
        mConnectionMaxRetry = maxRetry;
    }

    public void setEndReason(int reason) {
        mConnectionEndReason = reason;
    }

    public void setRssi(int rssi) {
        mConnectionRssi = rssi;
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
                    info.append("Station connect Wi-Fi now, got IP\n");
                } else if (getStaConnectionStatus() == BlufiParameter.STA_CONN_NO_IP) {
                    info.append("Station connect Wi-Fi now, no IP found\n");
                } else if (getStaConnectionStatus() == BlufiParameter.STA_CONN_FAIL) {
                    info.append("Station disconnect Wi-Fi now\n");
                    info.append(getEndInfo());
                } else {
                    info.append("Station is connecting WiFi now\n");
                    info.append(getConnectingInfo());
                }
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
