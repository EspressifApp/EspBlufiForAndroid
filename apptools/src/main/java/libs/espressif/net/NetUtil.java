package libs.espressif.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import libs.espressif.app.SdkUtil;

public class NetUtil {
    public static final int WIFI_SECURITY_OPEN = 0x00;
    public static final int WIFI_SECURITY_WEP = 0x01;
    public static final int WIFI_SECURITY_WPA = 0x02;

    public static final String WIFI_SSID_NONE = "<unknown ssid>";

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        int[] types = new int[]{ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE};
        for (int type : types) {
            NetworkInfo info = cm.getNetworkInfo(type);
            if (info != null && info.isAvailable()) {
                return true;
            }
        }

        return false;
    }

    public static InetAddress getWifiIpAddress(WifiManager wifi) {
        InetAddress result = null;
        try {
            // default to Android localhost
            result = InetAddress.getByName("10.0.0.2");

            // figure out our wifi address, otherwise bail
            WifiInfo wifiinfo = wifi.getConnectionInfo();
            int intaddr = wifiinfo.getIpAddress();
            byte[] byteaddr = new byte[]{(byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff)};
            result = InetAddress.getByAddress(byteaddr);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }

        return result;
    }

    public static String getIpStringForInt(int ip) {
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
    }

    public static byte[] getIpBytesForInt(int ip) {
        return new byte[]{
                (byte) (ip & 0xff),
                (byte) ((ip >> 8) & 0xff),
                (byte) ((ip >> 16) & 0xff),
                (byte) ((ip >> 24) & 0xff)
        };
    }

    public static byte[] getMacBytesForString(String mac) {
        byte[] result = new byte[6];
        String[] splits = mac.split(":");
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(splits[i], 16);
        }

        return result;
    }

    /**
     * Get the ssid of the connected access point.
     *
     * @param context The Application Context.
     * @return null if no wifi connection.
     */
    public static String getCurrentConnectSSID(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wm != null;
        WifiInfo connection = wm.getConnectionInfo();
        boolean isWifiConnected = connection != null && connection.getNetworkId() != -1;
        if (isWifiConnected) {
            String ssid = connection.getSSID();
            if (SdkUtil.isAtLeastJ_16()) {
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
            }

            return ssid;
        } else {
            return null;
        }
    }

    /**
     * Get the bssid of the connected access point.
     *
     * @param context The Application Context.
     * @return null if no wifi connection.
     */
    public static String getCurrentConnectBSSID(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wm != null;
        WifiInfo connection = wm.getConnectionInfo();
        boolean isWifiConnected = connection != null && connection.getNetworkId() != -1;
        if (isWifiConnected) {
            return connection.getBSSID();
        } else {
            return null;
        }
    }

    /**
     * Get current ip on the connected access point.
     *
     * @param context The Application Context.
     * @return Current connected ip, null is disconnected.
     */
    public static String getCurrentConnectIP(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wm != null;
        WifiInfo connection = wm.getConnectionInfo();
        boolean isWifiConnected = connection != null && connection.getNetworkId() != -1;
        if (isWifiConnected) {
            return getIpString(connection.getIpAddress());
        } else {
            return null;
        }
    }

    private static String getIpString(int ip) {
        StringBuilder ipSB = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            ipSB.append((ip >> (i * 8)) & 0xff);
            if (i < 3) {
                ipSB.append('.');
            }
        }

        return ipSB.toString();
    }

    /**
     * Get the connection information of the connected access point.
     *
     * @param context The Application Context.
     * @return An information string array with [ssid bssid ipAddress frequency], or null if disconnected
     */
    public static String[] getCurrentConnectionInfo(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wm != null;
        WifiInfo connection = wm.getConnectionInfo();
        boolean isWifiConnected = connection != null && connection.getNetworkId() != -1;
        if (isWifiConnected) {
            String[] result = new String[4];

            String ssid = connection.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }

            result[0] = ssid;
            result[1] = connection.getBSSID();
            result[2] = getIpString(connection.getIpAddress());
            if (SdkUtil.isAtLeastL_21()) {
                result[3] = String.valueOf(connection.getFrequency());
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Check is connected the wifi.
     *
     * @param context The Application Content.
     * @return Whether connected the wifi.
     */
    public static boolean isWifiConnected(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null) {
            return false;
        }
        WifiInfo connection = wm.getConnectionInfo();
        return connection != null && connection.getNetworkId() != -1 && !WIFI_SSID_NONE.equals(connection.getSSID());
    }

    /**
     * Check is the frequency 5G channel or not.
     *
     * @param frequency The frequency need check
     * @return true if the frequency is 5G
     */
    public static boolean is5GHz(int frequency) {
        return frequency > 4900 && frequency < 5900;
    }

    /**
     * Check is the frequency 2.4G channel or not.
     *
     * @param frequency The frequency need check
     * @return true if the frequency is 2.4G
     */
    public static boolean is24GHz(int frequency) {
        return frequency > 2400 && frequency < 2500;
    }

    /**
     * Get the channel by the frequency.
     *
     * @param frequency The wifi frequency.
     * @return The wifi channel. -1 is unknown channel.
     */
    public static int getWifiChannel(int frequency) {
        switch (frequency) {
            case 2412:
                return 1;
            case 2417:
                return 2;
            case 2422:
                return 3;
            case 2427:
                return 4;
            case 2432:
                return 5;
            case 2437:
                return 6;
            case 2442:
                return 7;
            case 2447:
                return 8;
            case 2452:
                return 9;
            case 2457:
                return 10;
            case 2462:
                return 11;
            case 2467:
                return 12;
            case 2472:
                return 13;
            case 2484:
                return 14;
            case 5035:
                return 7;
            case 5040:
                return 8;
            case 5045:
                return 9;
            case 5055:
                return 11;
            case 5060:
                return 12;
            case 5080:
                return 16;
            case 5170:
                return 34;
            case 5180:
                return 36;
            case 5190:
                return 38;
            case 5200:
                return 40;
            case 5210:
                return 42;
            case 5220:
                return 44;
            case 5230:
                return 46;
            case 5240:
                return 48;
            case 5260:
                return 52;
            case 5280:
                return 56;
            case 5300:
                return 60;
            case 5320:
                return 64;
            case 5500:
                return 100;
            case 5520:
                return 104;
            case 5540:
                return 108;
            case 5560:
                return 112;
            case 5580:
                return 116;
            case 5600:
                return 120;
            case 5620:
                return 124;
            case 5640:
                return 128;
            case 5660:
                return 132;
            case 5680:
                return 136;
            case 5700:
                return 140;
            case 5745:
                return 149;
            case 5765:
                return 153;
            case 5785:
                return 157;
            case 5805:
                return 161;
            case 5825:
                return 165;
            case 4915:
                return 183;
            case 4920:
                return 184;
            case 4925:
                return 185;
            case 4935:
                return 187;
            case 4940:
                return 188;
            case 4945:
                return 189;
            case 4960:
                return 192;
            case 4980:
                return 196;
            default:
                return -1;
        }
    }

    public static InetAddress getBroadcastAddress(Context context) throws UnknownHostException {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifi != null;
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            return InetAddress.getByName("255.255.255.255");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public static byte[] getOriginalSsidBytes(WifiInfo info) {
        try {
            Method method = info.getClass().getMethod("getWifiSsid");
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            Object wifiSsid = method.invoke(info);
            if (wifiSsid == null) {
                return null;
            }
            method = wifiSsid.getClass().getMethod("getOctets");
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getOriginalSsidBytes(ScanResult scanResult) {
        try {
            Field field = scanResult.getClass().getField("wifiSsid");
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object wifiSsid = field.get(scanResult);
            if (wifiSsid == null) {
                return null;
            }
            Method method = wifiSsid.getClass().getMethod("getOctets");
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static WifiConfiguration newWifiConfigration(int security, String ssid, String password, boolean hide) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = "\"" + ssid + "\""; // ##
        config.hiddenSSID = hide; // ##
        config.status = WifiConfiguration.Status.ENABLED;

        switch (security) {
            case WIFI_SECURITY_OPEN: // OPEN
                config.wepKeys[0] = "\"" + "\"";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case WIFI_SECURITY_WEP: // WEP
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                if (password != null && password.length() > 0) {
                    int length = password.length();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password; // ##
                    } else {
                        config.wepKeys[0] = '"' + password + '"'; // ##
                    }
                }
                break;
            case WIFI_SECURITY_WPA: // WPA
                config.preSharedKey = "\"" + password + "\""; // ##

                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                // for WPA
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                // for WPA2
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                break;
            default:
                return null;
        }
        return config;
    }
}
