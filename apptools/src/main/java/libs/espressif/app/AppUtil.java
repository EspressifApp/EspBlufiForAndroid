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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import libs.espressif.utils.DataUtil;

public class AppUtil {
    public static byte[] getSignatureMD5Bytes(Context context) {
        String packageName = context.getApplicationInfo().packageName;
        try {
            byte[] signData;
            if (SdkUtil.isAtLeastP_28()) {
                int flag = PackageManager.GET_SIGNING_CERTIFICATES;
                PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, flag);
                Signature signature = pi.signingInfo.getApkContentsSigners()[0];
                signData = signature.toByteArray();
            } else {
                int flag = PackageManager.GET_SIGNATURES;
                PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, flag);
                Signature signature = (pi.signatures)[0];
                signData = signature.toByteArray();
            }

            MessageDigest digest = MessageDigest.getInstance("md5");
            digest.update(signData);
            return digest.digest();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get the MD5 of the apk keystore.
     *
     * @return MD5 of the apk keystore.
     */
    public static String getSignatureMD5Hex(Context context) {
        byte[] data = getSignatureMD5Bytes(context);
        if (data == null) {
            return "";
        }

        return DataUtil.bigEndianBytesToHexString(data);
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

    public static long getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            return SdkUtil.isAtLeastP_28() ? info.getLongVersionCode() : info.versionCode;
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
