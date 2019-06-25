package libs.espressif.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppUtil {
    /**
     * Execute a system command.
     *
     * @param command the command Android system supported
     * @return the system log
     */
    public static String executeSystemCommand(String command) {
        String result = "";
        try {
            Process pp = Runtime.getRuntime().exec(command);
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            String line;
            while ((line = input.readLine()) != null) {
                result += line.trim();
            }

            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get MAC address.
     *
     * @return the phone wifi MAC address.
     */
    public static String getWifiMac() {
        return executeSystemCommand("cat /sys/class/net/wlan0/address");
    }

    /**
     * Get the MD5 of the apk keystore.
     *
     * @return MD5 of the apk keystore.
     */
    public static String getSignatureMD5(Context context) {
        String packageName = context.getApplicationInfo().packageName;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature signature = (pi.signatures)[0];
            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(signature.toByteArray());
            byte[] result = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                int number = b & 0xff;
                String str = Integer.toHexString(number);
                if (str.length() == 1) {
                    sb.append("0");
                }
                sb.append(str);
            }

            return sb.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException | NullPointerException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean supportBLE(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static String getVersionName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    public static int getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void installApk(Context context, File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (SdkUtil.isAtLeastN_24()) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String authority = context.getPackageName() + ".fileProvider";
            uri = FileProvider.getUriForFile(context, authority, apk);
        } else {
            uri = Uri.fromFile(apk);
        }
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
}
