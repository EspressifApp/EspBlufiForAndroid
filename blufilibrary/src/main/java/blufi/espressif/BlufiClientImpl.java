package blufi.espressif;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.interfaces.DHPublicKey;

import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.params.BlufiParameter;
import blufi.espressif.response.BlufiScanResult;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;
import blufi.espressif.security.BlufiAES;
import blufi.espressif.security.BlufiCRC;
import blufi.espressif.security.BlufiDH;
import blufi.espressif.security.BlufiMD5;

class BlufiClientImpl implements BlufiParameter {
    private static final String TAG = "BlufiClientImpl";

    private static final int DEFAULT_PACKAGE_LENGTH = 20;
    private static final int PACKAGE_HEADER_LENGTH = 4;
    private static final int MIN_PACKAGE_LENGTH = 6;

    private static final String DH_P = "cf5cf5c38419a724957ff5dd323b9c45c3cdd261eb740f69aa94b8bb1a5c9640" +
            "9153bd76b24222d03274e4725a5406092e9e82e9135c643cae98132b0d95f7d6" +
            "5347c68afc1e677da90e51bbab5f5cf429c291b4ba39c6b2dc5e8c7231e46aa7" +
            "728e87664532cdf547be20c9a3fa8342be6e34371a27c06f7dc0edddd2f86373";
    private static final String DH_G = "2";
    private static final String AES_TRANSFORMATION = "AES/CFB/NoPadding";

    private BlufiClient mClient;

    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mWriteCharact;
    private final Object mWriteLock;
    private BluetoothGattCharacteristic mNotiCharact;

    private volatile BlufiCallback mUserCallback;

    private int mPackageLengthLimit;

    private AtomicInteger mSendSequence;
    private AtomicInteger mReadSequence;
    private LinkedBlockingQueue<Integer> mAck;

    private volatile BlufiNotiyData mNotiyData;

    private byte[] mSecretKeyMD5;

    private boolean mEncrypted = false;
    private boolean mChecksum = false;

    private boolean mRequireAck = false;

    private SecurityCallback mSecurityCallback;
    private LinkedBlockingQueue<BigInteger> mDevicePublicKeyQueue;

    private ExecutorService mThreadPool;
    private Handler mUIHandler;

    BlufiClientImpl(BlufiClient client) {
        mClient = client;

        mPackageLengthLimit = DEFAULT_PACKAGE_LENGTH;
        mSendSequence = new AtomicInteger(0);
        mReadSequence = new AtomicInteger(-1);
        mAck = new LinkedBlockingQueue<>();

        mSecurityCallback = new SecurityCallback();
        mDevicePublicKeyQueue = new LinkedBlockingQueue<>();

        mThreadPool = Executors.newSingleThreadExecutor();
        mUIHandler = new Handler(Looper.getMainLooper());

        mWriteLock = new Object();
    }

    BlufiClientImpl(BlufiClient client, BluetoothGatt gatt, BluetoothGattCharacteristic writeCharact,
                    BluetoothGattCharacteristic notiCharact, BlufiCallback callback) {
        this(client);

        mGatt = gatt;
        mWriteCharact = writeCharact;
        mNotiCharact = notiCharact;
        mUserCallback = callback;
    }

    void setBluetoothGatt(BluetoothGatt gatt) {
        mGatt = gatt;
    }

    BluetoothGatt getBluetoothGatt() {
        return mGatt;
    }

    void setWriteGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        mWriteCharact = characteristic;
    }

    void setNotificationGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        mNotiCharact = characteristic;
    }

    void setBlufiCallback(BlufiCallback callback) {
        mUserCallback = callback;
    }

    void close() {
        if (mThreadPool != null) {
            mThreadPool.shutdownNow();
            mThreadPool = null;
        }
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
        mNotiCharact = null;
        mWriteCharact = null;
        if (mAck != null) {
            mAck.clear();
            mAck = null;
        }
        mClient = null;
        mUserCallback = null;
    }

    void setPostPackageLengthLimit(int lengthLimit) {
        mPackageLengthLimit = lengthLimit;
        if (mPackageLengthLimit < MIN_PACKAGE_LENGTH) {
            mPackageLengthLimit = MIN_PACKAGE_LENGTH;
        }
    }

    void requestDeviceVersion() {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __requestDeviceVersion();
            }
        });
    }

    void requestDeviceStatus() {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __requestDeviceStatus();
            }
        });
    }

    void negotiateSecurity() {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __negotiateSecurity();
            }
        });
    }

    void configure(final BlufiConfigureParams params) {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __configure(params);
            }
        });
    }

    void requestDeviceWifiScan() {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __requestDeviceWifiScan();
            }
        });
    }

    void postCustomData(final byte[] data) {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __postCustomData(data);
            }
        });
    }

    void requestCloseConnection() {
        mThreadPool.submit(new ThrowableRunnable() {
            @Override
            void execute() {
                __requestCloseConnection();
            }
        });
    }

    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic != mNotiCharact) {
            return;
        }

        if (mNotiyData == null) {
            mNotiyData = new BlufiNotiyData();
        }

        byte[] data = characteristic.getValue();
        // lt 0 is error, eq 0 is complete, gt 0 is continue
        int parse = parseNotification(data, mNotiyData);
        if (parse < 0) {
            onError(BlufiCallback.CODE_INVALID_NOTIFICATION);
        } else if (parse == 0) {
            parseBlufiNotiData(mNotiyData);
            mNotiyData = null;
        }
    }

    void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic != mWriteCharact) {
            return;
        }

        synchronized (mWriteLock) {
            mWriteLock.notifyAll();
        }
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }

    private void logw(String msg) {
        Log.w(TAG, msg);
    }

    private int toInt(byte b) {
        return b & 0xff;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int number = b & 0xff;
            String str = Integer.toHexString(number);
            if (str.length() == 1) {
                sb.append("0");
            }
            sb.append(str);
        }
        return sb.toString();
    }

    private byte[] toBytes(String hex) {
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }

    private int getTypeValue(int type, int subtype) {
        return (subtype << 2) | type;
    }

    private int getPackageType(int typeValue) {
        return typeValue & 0b11;
    }

    private int getSubType(int typeValue) {
        return ((typeValue & 0b11111100) >> 2);
    }

    private int generateSendSequence() {
        return mSendSequence.getAndIncrement();
    }

    private byte[] generateAESIV(int sequence) {
        byte[] result = new byte[16];
        result[0] = (byte) sequence;

        return result;
    }

    private synchronized void gattWrite(byte[] data) throws InterruptedException {
        synchronized (mWriteLock) {
            mWriteCharact.setValue(data);
            mGatt.writeCharacteristic(mWriteCharact);

            mWriteLock.wait();
        }
    }

    private boolean receiveAck(int sequence) {
        try {
            int ack = mAck.take();
            return ack == sequence;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean post(boolean encrypt, boolean checksum, boolean requireAck, int type, byte[] data)
            throws InterruptedException {
        if (data == null || data.length == 0) {
            return postNonData(encrypt, checksum, requireAck, type);
        } else {
            return postContainData(encrypt, checksum, requireAck, type, data);
        }
    }

    private boolean postNonData(boolean encrypt, boolean checksum, boolean requireAck, int type)
            throws InterruptedException {
        int frameCtrl = FrameCtrlData.getFrameCTRLValue(encrypt, checksum, DIRECTION_OUTPUT, requireAck, false);
        int sequence = generateSendSequence();
        int dataLen = 0;

        byte[] postBytes = getPostBytes(type, frameCtrl, sequence, dataLen, null);
        gattWrite(postBytes);

        return !requireAck || receiveAck(sequence);
    }

    private boolean postContainData(boolean encrypt, boolean checksum, boolean requireAck, int type, byte[] data)
            throws InterruptedException {
        ByteArrayInputStream dataIS = new ByteArrayInputStream(data);

        ByteArrayOutputStream postOS = new ByteArrayOutputStream();

        for (int b = dataIS.read(); b != -1; b = dataIS.read()) {
            postOS.write(b);
            int postDataLengthLimit = mPackageLengthLimit - PACKAGE_HEADER_LENGTH;
            if (checksum) {
                postDataLengthLimit -= 1;
            }
            if (postOS.size() >= postDataLengthLimit) {
                boolean frag = dataIS.available() > 0;
                if (frag) {
                    int frameCtrl = FrameCtrlData.getFrameCTRLValue(encrypt, checksum, DIRECTION_OUTPUT, requireAck, true);
                    int sequence = generateSendSequence();
                    int totleLen = postOS.size() + dataIS.available();
                    byte totleLen1 = (byte) (totleLen & 0xff);
                    byte totleLen2 = (byte) ((totleLen >> 8) & 0xff);
                    byte[] tempData = postOS.toByteArray();
                    postOS.reset();
                    postOS.write(totleLen1);
                    postOS.write(totleLen2);
                    postOS.write(tempData, 0, tempData.length);
                    int posDatatLen = postOS.size();

                    byte[] postBytes = getPostBytes(type, frameCtrl, sequence, posDatatLen, postOS.toByteArray());
                    gattWrite(postBytes);
                    postOS.reset();
                    if (requireAck && !receiveAck(sequence)) {
                        return false;
                    }

                    sleep(10L);
                }
            }
        }

        if (postOS.size() > 0) {
            int frameCtrl = FrameCtrlData.getFrameCTRLValue(encrypt, checksum, DIRECTION_OUTPUT, requireAck, false);
            int sequence = generateSendSequence();
            int postDataLen = postOS.size();

            byte[] postBytes = getPostBytes(type, frameCtrl, sequence, postDataLen, postOS.toByteArray());
            gattWrite(postBytes);
            postOS.reset();

            return !requireAck || receiveAck(sequence);
        }

        return true;
    }

    private byte[] getPostBytes(int type, int frameCtrl, int sequence, int dataLength, byte[] data) {
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        byteOS.write(type);
        byteOS.write(frameCtrl);
        byteOS.write(sequence);
        byteOS.write(dataLength);

        FrameCtrlData frameCtrlData = new FrameCtrlData(frameCtrl);
        byte[] checksumBytes = null;
        if (frameCtrlData.isChecksum()) {
            byte[] willCheckBytes = new byte[]{(byte) sequence, (byte) dataLength};
            if (data != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream(willCheckBytes.length + data.length);
                os.write(willCheckBytes, 0, willCheckBytes.length);
                os.write(data, 0, data.length);
                willCheckBytes = os.toByteArray();
            }
            int checksum = BlufiCRC.calcCRC(0, willCheckBytes);
            byte checksumByte1 = (byte) (checksum & 0xff);
            byte checksumByte2 = (byte) ((checksum >> 8) & 0xff);
            checksumBytes = new byte[]{checksumByte1, checksumByte2};
        }

        if (frameCtrlData.isEncrypted() && data != null) {
            BlufiAES espAES = new BlufiAES(mSecretKeyMD5, AES_TRANSFORMATION, generateAESIV(sequence));
            data = espAES.encrypt(data);
        }
        if (data != null) {
            byteOS.write(data, 0, data.length);
        }

        if (checksumBytes != null) {
            byteOS.write(checksumBytes[0]);
            byteOS.write(checksumBytes[1]);
        }

        return byteOS.toByteArray();
    }

    private int parseNotification(byte[] response, BlufiNotiyData notification) {
        if (response == null) {
            logw("parseNotification null data");
            return -1;
        }

        if (response.length < 4) {
            logw("parseNotification data length less than 4");
            return -2;
        }

        int sequence = toInt(response[2]);
        if (sequence != mReadSequence.incrementAndGet()) {
            logw("parseNotification read sequence wrong");
            return -3;
        }

        int type = toInt(response[0]);
        int pkgType = getPackageType(type);
        int subType = getSubType(type);
        notification.setType(type);
        notification.setPkgType(pkgType);
        notification.setSubType(subType);

        int frameCtrl = toInt(response[1]);
        notification.setFrameCtrl(frameCtrl);
        FrameCtrlData frameCtrlData = new FrameCtrlData(frameCtrl);

        int dataLen = toInt(response[3]);
        byte[] dataBytes = new byte[dataLen];
        int dataOffset = 4;
        try {
            System.arraycopy(response, dataOffset, dataBytes, 0, dataLen);
        } catch (Exception e) {
            e.printStackTrace();
            return -100;
        }

        if (frameCtrlData.isEncrypted()) {
            BlufiAES espAES = new BlufiAES(mSecretKeyMD5, AES_TRANSFORMATION, generateAESIV(sequence));
            dataBytes = espAES.decrypt(dataBytes);
        }

        if (frameCtrlData.isChecksum()) {
            int respChecksum1 = toInt(response[response.length - 1]);
            int respChecksum2 = toInt(response[response.length - 2]);

            ByteArrayOutputStream checkByteOS = new ByteArrayOutputStream();
            checkByteOS.write(sequence);
            checkByteOS.write(dataLen);
            for (byte b : dataBytes) {
                checkByteOS.write(b);
            }
            int checksum = BlufiCRC.calcCRC(0, checkByteOS.toByteArray());

            int calcChecksum1 = (checksum >> 8) & 0xff;
            int calcChecksum2 = checksum & 0xff;
            if (respChecksum1 != calcChecksum1 || respChecksum2 != calcChecksum2) {
                return -4;
            }
        }

        if (frameCtrlData.hasFrag()) {
            int totleLen = dataBytes[0] | (dataBytes[1] << 8);
            dataOffset = 2;
        } else {
            dataOffset = 0;
        }
        for (int i = dataOffset; i < dataBytes.length; i++) {
            notification.addData(dataBytes[i]);
        }

        return frameCtrlData.hasFrag() ? 1 : 0;
    }

    private void parseBlufiNotiData(BlufiNotiyData data) {
        int pkgType = data.getPkgType();
        int subType = data.getSubType();
        if (mUserCallback != null) {
            boolean complete = mUserCallback.onGattNotification(mClient, pkgType, subType, data.getDataArray());
            if (complete) {
                return;
            }
        }

        switch (pkgType) {
            case Type.Ctrl.PACKAGE_VALUE:
                parseCtrlData(subType, data.getDataArray());
                break;
            case Type.Data.PACKAGE_VALUE:
                parseDataData(subType, data.getDataArray());
                break;
        }
    }

    private void parseCtrlData(int subType, byte[] data) {
        switch (subType) {
            case Type.Ctrl.SUBTYPE_ACK:
                parseAck(data);
                break;
        }
    }

    private void parseDataData(int subType, byte[] data) {
        switch (subType) {
            case Type.Data.SUBTYPE_NEG:
                mSecurityCallback.onReceiveDevicePublicKey(data);
                break;
            case Type.Data.SUBTYPE_VERSION:
                parseVersion(data);
                break;
            case Type.Data.SUBTYPE_WIFI_CONNECTION_STATE:
                parseWifiState(data);
                break;
            case Type.Data.SUBTYPE_WIFI_LIST:
                parseWifiScanList(data);
                break;
            case Type.Data.SUBTYPE_CUSTOM_DATA:
                onReceiveCustomData(BlufiCallback.STATUS_SUCCESS, data);
                break;
            case Type.Data.SUBTYPE_ERROR:
                int errCode = data.length > 0 ? (data[0] & 0xff) : 0xff;
                onError(errCode);
                break;
        }
    }

    private void parseAck(byte[] data) {
        int ack = -1;
        if (data.length > 0) {
            ack = data[0] & 0xff;
        }

        mAck.add(ack);
    }

    private void parseVersion(byte[] data) {
        if (data.length != 2) {
            onVersionResponse(BlufiCallback.CODE_INVALID_DATA, null);
        }

        BlufiVersionResponse response = new BlufiVersionResponse();
        response.setVersionValues(toInt(data[0]), toInt(data[1]));
        onVersionResponse(BlufiCallback.STATUS_SUCCESS, response);
    }

    private void parseWifiState(byte[] data) {
        if (data.length < 3) {
            onStatusResponse(BlufiCallback.CODE_INVALID_DATA, null);
            return;
        }

        BlufiStatusResponse response = new BlufiStatusResponse();

        ByteArrayInputStream dataIS = new ByteArrayInputStream(data);

        int opMode = dataIS.read() & 0xff;
        response.setOpMode(opMode);
        switch (opMode) {
            case OP_MODE_NULL:
                break;
            case OP_MODE_STA:
                break;
            case OP_MODE_SOFTAP:
                break;
            case OP_MODE_STASOFTAP:
                break;
        }

        int staConn = dataIS.read() & 0xff;
        response.setStaConnectionStatus(staConn);

        int softAPConn = dataIS.read() & 0xff;
        response.setSoftAPConnectionCount(softAPConn);

        while (dataIS.available() > 0) {
            int infotype = dataIS.read() & 0xff;
            int len = dataIS.read() & 0xff;
            byte[] stateBytes = new byte[len];
            for (int i = 0; i < len; i++) {
                stateBytes[i] = (byte) dataIS.read();
            }

            parseWifiStateData(response, infotype, stateBytes);
        }

        onStatusResponse(BlufiCallback.STATUS_SUCCESS, response);
    }

    private void parseWifiStateData(BlufiStatusResponse response, int infoType, byte[] data) {
        switch (infoType) {
            case BlufiParameter.Type.Data.SUBTYPE_SOFTAP_AUTH_MODE:
                int authMode = toInt(data[0]);
                response.setSoftAPSecrity(authMode);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_SOFTAP_CHANNEL:
                int softAPChannel = toInt(data[0]);
                response.setSoftAPChannel(softAPChannel);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_SOFTAP_MAX_CONNECTION_COUNT:
                int softAPMaxConnCount = toInt(data[0]);
                response.setSoftAPMaxConnectionCount(softAPMaxConnCount);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_SOFTAP_WIFI_PASSWORD:
                String softapPassword = new String(data);
                response.setSoftAPPassword(softapPassword);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_SOFTAP_WIFI_SSID:
                String softapSSID = new String(data);
                response.setSoftAPSSID(softapSSID);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_STA_WIFI_BSSID:
                String staBssid = toHex(data);
                response.setStaBSSID(staBssid);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_STA_WIFI_SSID:
                String staSsid = new String(data);
                response.setStaSSID(staSsid);
                break;
            case BlufiParameter.Type.Data.SUBTYPE_STA_WIFI_PASSWORD:
                String staPassword = new String(data);
                response.setStaPassword(staPassword);
                break;
        }
    }

    private void parseWifiScanList(byte[] data) {
        ByteArrayInputStream dataIS = new ByteArrayInputStream(data);

        List<BlufiScanResult> result = new LinkedList<>();
        boolean readLength = true;
        boolean readRssi = false;
        boolean readSSID = false;
        int length = 0;
        byte rssi = 0;
        ByteArrayOutputStream ssidOS = new ByteArrayOutputStream();
        while (dataIS.available() > 0) {
            int read = -1;
            if (readLength) {
                read = dataIS.read();
                if (read == -1) {
                    logd("parseWifiScanList read len null");
                    break;
                }

                length = read & 0xff;
                readLength = false;
                readRssi = true;
            }

            if (readRssi) {
                read = dataIS.read();
                if (read == -1) {
                    logd("parseWifiScanList read rssi null");
                    break;
                }

                rssi = (byte) read;
                readRssi = false;
                readSSID = true;
            }

            if (readSSID) {
                read = dataIS.read();
                if (read == -1) {
                    logd("parseWifiScanList read ssid null");
                    break;
                }

                ssidOS.write(read);
                if (ssidOS.size() == length - 1) {
                    readSSID = false;
                    readLength = true;

                    BlufiScanResult sr = new BlufiScanResult();
                    sr.setType(BlufiScanResult.TYPE_WIFI);
                    sr.setRssi(rssi);
                    String ssid = new String(ssidOS.toByteArray());
                    ssidOS.reset();
                    sr.setSsid(ssid);
                    result.add(sr);
                }
            }
        }

        onDeviceScanResult(BlufiCallback.STATUS_SUCCESS, result);
    }

    private void onError(final int errCode) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onError(mClient, errCode);
            }
        });
    }

    private void __negotiateSecurity() {
        BlufiDH espDH = postNegotiateSecurity();
        if (espDH == null) {
            logw("negotiateSecurity postNegotiateSecurity failed");
            onNegotiateSecurityResult(BlufiCallback.CODE_NEG_POST_FAILED);
            return;
        }

        BigInteger devicePublicKey;
        try {
            devicePublicKey = mDevicePublicKeyQueue.take();
            if (devicePublicKey.bitLength() == 0) {
                onNegotiateSecurityResult(BlufiCallback.CODE_NEG_ERR_DEV_KEY);
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return;
        }

        try {
            espDH.generateSecretKey(devicePublicKey);
            if (espDH.getSecretKey() == null) {
                onNegotiateSecurityResult(BlufiCallback.CODE_NEG_ERR_SECURITY);
                return;
            }

            mSecretKeyMD5 = BlufiMD5.getMD5Bytes(espDH.getSecretKey());
        } catch (Exception e) {
            e.printStackTrace();
            onNegotiateSecurityResult(BlufiCallback.CODE_NEG_ERR_SECURITY);
            return;
        }

        mEncrypted = true;
        mChecksum = true;
        boolean setSecurity = false;
        try {
            setSecurity = postSetSecurity(false, false, mEncrypted, mChecksum);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (setSecurity) {
            onNegotiateSecurityResult(BlufiCallback.STATUS_SUCCESS);
        } else {
            onNegotiateSecurityResult(BlufiCallback.CODE_NEG_ERR_SET_SECURITY);
        }
    }

    private void onNegotiateSecurityResult(final int status) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onNegotiateSecurityResult(mClient, status);
            }
        });
    }

    private BlufiDH postNegotiateSecurity() {
        int type = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_NEG);

        final int radix = 16;
        final int dhLength = 1024;
        final BigInteger dhP = new BigInteger(DH_P, radix);
        final BigInteger dhG = new BigInteger(DH_G);
        BlufiDH espDH;
        String p;
        String g;
        String k;
        do {
            espDH = new BlufiDH(dhP, dhG, dhLength);
            p = espDH.getP().toString(radix);
            g = espDH.getG().toString(radix);
            k = getPublicValue(espDH);
        } while (k == null);

        byte[] pBytes = toBytes(p);
        byte[] gBytes = toBytes(g);
        byte[] kBytes = toBytes(k);

        ByteArrayOutputStream dataOS = new ByteArrayOutputStream();

        int pgkLength = pBytes.length + gBytes.length + kBytes.length + 6;
        int pgkLen1 = (pgkLength >> 8) & 0xff;
        int pgkLen2 = pgkLength & 0xff;
        dataOS.write(NEG_SET_SEC_TOTAL_LEN);
        dataOS.write((byte) pgkLen1);
        dataOS.write((byte) pgkLen2);
        try {
            boolean postLength = post(false, false, mRequireAck, type, dataOS.toByteArray());
            if (!postLength) {
                return null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return null;
        }

        sleep(10);

        dataOS.reset();
        dataOS.write(NEG_SET_SEC_ALL_DATA);

        int pLength = pBytes.length;
        int pLen1 = (pLength >> 8) & 0xff;
        int pLen2 = pLength & 0xff;
        dataOS.write(pLen1);
        dataOS.write(pLen2);
        dataOS.write(pBytes, 0, pLength);

        int gLength = gBytes.length;
        int gLen1 = (gLength >> 8) & 0xff;
        int gLen2 = gLength & 0xff;
        dataOS.write(gLen1);
        dataOS.write(gLen2);
        dataOS.write(gBytes, 0, gLength);

        int kLength = kBytes.length;
        int kLen1 = (kLength >> 8) & 0xff;
        int kLen2 = kLength & 0xff;
        dataOS.write(kLen1);
        dataOS.write(kLen2);
        dataOS.write(kBytes, 0, kLength);

        try {
            boolean postPGK = post(false, false, mRequireAck, type, dataOS.toByteArray());
            if (!postPGK) {
                return null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return null;
        }

        dataOS.reset();
        return espDH;
    }

    private String getPublicValue(BlufiDH espDH) {
        DHPublicKey publicKey = espDH.getPublicKey();
        if (publicKey != null) {
            BigInteger y = publicKey.getY();
            StringBuilder keySB = new StringBuilder(y.toString(16));
            while (keySB.length() < 256) {
                keySB.insert(0, "0");
            }
            return keySB.toString();
        }

        return null;
    }

    private boolean postSetSecurity(boolean ctrlEncrypted, boolean ctrlChecksum, boolean dataEncrypted, boolean dataChecksum) {
        int type = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_SET_SEC_MODE);
        int data = 0;
        if (dataChecksum) {
            data = data | 1;
        }
        if (dataEncrypted) {
            data = data | (1 << 1);
        }
        if (ctrlChecksum) {
            data = data | (1 << 4);
        }
        if (ctrlEncrypted) {
            data = data | (1 << 5);
        }

        byte[] postData = {(byte) data};

        try {
            return post(false, true, mRequireAck, type, postData);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private class SecurityCallback {
        void onReceiveDevicePublicKey(byte[] keyData) {
            String keyStr = toHex(keyData);
            try {
                BigInteger devicePublicValue = new BigInteger(keyStr, 16);
                mDevicePublicKeyQueue.add(devicePublicValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                mDevicePublicKeyQueue.add(new BigInteger("0"));
            }
        }
    }

    private void __configure(BlufiConfigureParams params) {
        int opMode = params.getOpMode();
        switch (opMode) {
            case OP_MODE_NULL:
                if (!postDeviceMode(opMode)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_SET_OPMODE);
                    return;
                }

                onConfigureResult(BlufiCallback.STATUS_SUCCESS);
                return;
            case OP_MODE_STA:
                if (!postDeviceMode(opMode)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_SET_OPMODE);
                    return;
                }
                if (!postStaWifiInfo(params)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_POST_STA);
                    return;
                }

                onConfigureResult(BlufiCallback.STATUS_SUCCESS);
                return;
            case OP_MODE_SOFTAP:
                if (!postDeviceMode(opMode)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_SET_OPMODE);
                    return;
                }
                if (!postSoftAPInfo(params)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_POST_SOFTAP);
                    return;
                }

                onConfigureResult(BlufiCallback.STATUS_SUCCESS);
                return;
            case OP_MODE_STASOFTAP:
                if (!postDeviceMode(opMode)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_SET_OPMODE);
                    return;
                }
                if (!postStaWifiInfo(params)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_POST_STA);
                    return;
                }
                if (!postSoftAPInfo(params)) {
                    onConfigureResult(BlufiCallback.CODE_CONF_ERR_POST_SOFTAP);
                    return;
                }

                onConfigureResult(BlufiCallback.STATUS_SUCCESS);
                break;
            default:
                onConfigureResult(BlufiCallback.CODE_CONF_INVALID_OPMODE);
                break;
        }
    }

    private void onConfigureResult(final int status) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onConfigureResult(mClient, status);
            }
        });
    }

    private boolean postDeviceMode(int deviceMode) {
        int type = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_SET_OP_MODE);
        byte[] data = {(byte) deviceMode};

        try {
            return post(mEncrypted, mChecksum, true, type, data);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean postStaWifiInfo(BlufiConfigureParams params) {
        try {
            int ssidType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_STA_WIFI_SSID);
            byte[] ssidBytes = params.getStaSSIDBytes();
            if (!post(mEncrypted, mChecksum, mRequireAck, ssidType, ssidBytes)) {
                return false;
            }
            sleep(10);

            int pwdType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_STA_WIFI_PASSWORD);
            if (!post(mEncrypted, mChecksum, mRequireAck, pwdType, params.getStaPassword().getBytes())) {
                return false;
            }
            sleep(10);

            int comfirmType = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_CONNECT_WIFI);
            return post(false, false, mRequireAck, comfirmType, (byte[]) null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean postSoftAPInfo(BlufiConfigureParams params) {
        try {
            String ssid = params.getSoftAPSSID();
            if (!TextUtils.isEmpty(ssid)) {
                int ssidType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_SOFTAP_WIFI_SSID);
                if (!post(mEncrypted, mChecksum, mRequireAck, ssidType, params.getSoftAPSSID().getBytes())) {
                    return false;
                }
                sleep(10);
            }

            String password = params.getSoftAPPassword();
            if (!TextUtils.isEmpty(password)) {
                int pwdType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_SOFTAP_WIFI_PASSWORD);
                if (!post(mEncrypted, mChecksum, mRequireAck, pwdType, password.getBytes())) {
                    return false;
                }
                sleep(10);
            }

            int channel = params.getSoftAPChannel();
            if (channel > 0) {
                int channelType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_SOFTAP_CHANNEL);
                if (!post(mEncrypted, mChecksum, mRequireAck, channelType, new byte[]{(byte) channel})) {
                    return false;
                }
                sleep(10);
            }

            int maxConn = params.getSoftAPMaxConnection();
            if (maxConn > 0) {
                int maxConnType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_SOFTAP_MAX_CONNECTION_COUNT);
                if (!post(mEncrypted, mChecksum, mRequireAck, maxConnType, new byte[]{(byte) maxConn})) {
                    return false;
                }
                sleep(10);
            }

            int securityType = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_SOFTAP_AUTH_MODE);
            byte[] securityBytes = {(byte) params.getSoftAPSecurity()};
            return post(mEncrypted, mChecksum, mRequireAck, securityType, securityBytes);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void __requestDeviceVersion() {
        int type = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_GET_VERSION);
        boolean request;
        try {
            request = post(mEncrypted, mChecksum, false, type, (byte[]) null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            request = false;
            Thread.currentThread().interrupt();
        }

        if (!request) {
            onVersionResponse(BlufiCallback.CODE_WRITE_DATA_FAILED, null);
        }
    }

    private void onVersionResponse(final int status, final BlufiVersionResponse response) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onDeviceVersionResponse(mClient, status, response);
            }
        });
    }

    private void __requestDeviceStatus() {
        int type = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_GET_WIFI_STATUS);
        boolean request;
        try {
            request = post(mEncrypted, mChecksum, false, type, (byte[]) null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            request = false;
            Thread.currentThread().interrupt();
        }

        if (!request) {
            onStatusResponse(BlufiCallback.CODE_WRITE_DATA_FAILED, null);
        }
    }

    private void onStatusResponse(final int status, final BlufiStatusResponse response) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onDeviceStatusResponse(mClient, status, response);
            }
        });
    }

    private void __requestDeviceWifiScan() {
        int type = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_GET_WIFI_LIST);
        boolean request;
        try {
            request = post(mEncrypted, mChecksum, mRequireAck, type, (byte[]) null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            request = false;
            Thread.currentThread().interrupt();
        }

        if (!request) {
            onDeviceScanResult(BlufiCallback.CODE_WRITE_DATA_FAILED, Collections.<BlufiScanResult>emptyList());
        }
    }

    private void onDeviceScanResult(final int status, final List<BlufiScanResult> results) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onDeviceScanResult(mClient, status, results);
            }
        });
    }

    private void __postCustomData(byte[] data) {
        int type = getTypeValue(Type.Data.PACKAGE_VALUE, Type.Data.SUBTYPE_CUSTOM_DATA);
        try {
            boolean suc = post(mEncrypted, mChecksum, mRequireAck, type, data);
            int status = suc ? BlufiCallback.STATUS_SUCCESS : BlufiCallback.CODE_WRITE_DATA_FAILED;
            onPostCustomDataResult(status, data);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private void onPostCustomDataResult(final int status, final byte[] data) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onPostCustomDataResult(mClient, status, data);
            }
        });
    }

    private void onReceiveCustomData(final int status, final byte[] data) {
        mUIHandler.post(() -> {
            if (mUserCallback != null) {
                mUserCallback.onReceiveCustomData(mClient, status, data);
            }
        });
    }

    private void __requestCloseConnection() {
        int type = getTypeValue(Type.Ctrl.PACKAGE_VALUE, Type.Ctrl.SUBTYPE_CLOSE_CONNECTION);
        try {
            post(false, false, false, type, (byte[]) null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private abstract class ThrowableRunnable implements Runnable {
        @Override
        public void run() {
            try {
                execute();
            } catch (Exception e) {
                e.printStackTrace();
                onError(e);
            }
        }

        abstract void execute();

        void onError(Exception e) {
        }
    }

    private void sleep(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
