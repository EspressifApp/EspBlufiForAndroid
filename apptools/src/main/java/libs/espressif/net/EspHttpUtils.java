package libs.espressif.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import libs.espressif.log.EspLog;
import libs.espressif.utils.DataUtil;

public class EspHttpUtils {
    public static final String H_NAME_CONNECTION = "Connection";
    public static final String H_NAME_CONTENT_TYPE = "Content-Type";
    public static final String H_NAME_CONTENT_LENGTH = "Content-Length";
    public static final String H_NAME_TRANSFER_ENCODING = "Transfer-Encoding";

    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    public static final EspHttpHeader HEADER_KEEP_ALIVE = new ConstHeader(H_NAME_CONNECTION, "Keep-Alive");
    public static final EspHttpHeader HEADER_CONTENT_JSON = new ConstHeader(H_NAME_CONTENT_TYPE, "application/json");
    public static final EspHttpHeader HEADER_CHUNKED = new ConstHeader(H_NAME_TRANSFER_ENCODING, "chunked");

    private static final EspLog log = new EspLog(EspHttpUtils.class);

    private static final String H_VALUE_CLOSE = "close";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";

    private static final int TIMEOUT_CONNECT = 4000;
    private static final int TIMEOUT_SO_GET = 5000;
    private static final int TIMEOUT_SO_POST = 5000;
    private static final int TIMEOUT_NO_RESPONSE = 1;

    private static final char[] SPEC_CHARS = {'+', '*', ':'};
    /**
     * Execute Http Get request.
     *
     * @param url     target url
     * @param headers http headers
     * @return response. null is failed.
     */
    public static EspHttpResponse Get(String url, EspHttpParams params, EspHttpHeader... headers) {
        return execute(url, METHOD_GET, null, params, headers);
    }

    /**
     * Execute Http Post request.
     *
     * @param url     target url
     * @param content content bytes
     * @param headers http headers
     * @return response. null is failed.
     */
    public static EspHttpResponse Post(String url, byte[] content, EspHttpParams params, EspHttpHeader... headers) {
        return execute(url, METHOD_POST, content, params, headers);
    }

    /**
     * Execute Http Put request.
     *
     * @param url     target url
     * @param content content bytes
     * @param headers http headers
     * @return response. null is failed.
     */
    public static EspHttpResponse Put(String url, byte[] content, EspHttpParams params, EspHttpHeader... headers) {
        return execute(url, METHOD_PUT, content, params, headers);
    }

    /**
     * Execute Http Delete request.
     *
     * @param url     target url
     * @param content content bytes
     * @param headers http headers
     * @return response. null is failed.
     */
    public static EspHttpResponse Delete(String url, byte[] content, EspHttpParams params, EspHttpHeader... headers) {
        return execute(url, METHOD_DELETE, content, params, headers);
    }

    private static EspHttpResponse execute(String url, String method, byte[] content,
                                           EspHttpParams params, EspHttpHeader... headers) {
        EspHttpResponse response = null;

        int tryCount = 1;
        boolean requireResp = true;
        if (params != null) {
            tryCount = Math.max(tryCount, params.getTryCount());
            requireResp = params.isRequireResponse();
        }

        for (int i = 0; i < tryCount; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            HttpURLConnection connection = createURLConnection(url, method, params, headers);
            response = executeHttpRequest(connection, content, requireResp);
            if (connection != null) {
                connection.disconnect();
            }
            if (response != null) {
                break;
            }
        }
        return response;
    }

    private static HttpURLConnection createURLConnection(String url, String method, EspHttpParams params, EspHttpHeader... headers) {
        try {
            URL targetURL = new URL(url);
            String file = targetURL.getFile();
            if (file != null) {
                for (char c : SPEC_CHARS) {
                    String asciiStr = String.format(Locale.ENGLISH, "%%%02X", (int)c);
                    file = file.replace(String.valueOf(c), asciiStr);
                }
                targetURL = new URL(targetURL.getProtocol(), targetURL.getHost(), targetURL.getPort(), file);
            }
            HttpURLConnection connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod(method);
            int timeoutConn = -1;
            int timeoutSO = -1;
            for (EspHttpHeader head : headers) {
                if (head == null) {
                    continue;
                }

                connection.addRequestProperty(head.getName(), head.getValue());
            }
            String connValue = connection.getRequestProperty(H_NAME_CONNECTION);
            if (connValue == null) {
                connection.addRequestProperty(H_NAME_CONNECTION, H_VALUE_CLOSE);
            }

            if (params != null) {
                timeoutConn = params.getConnectTimeout();
                timeoutSO = params.getSOTimeout();
            }
            if (timeoutConn <= 0) {
                timeoutConn = TIMEOUT_CONNECT;
            }
            connection.setConnectTimeout(timeoutConn);
            if (timeoutSO < 0) {
                timeoutSO = method.equals(METHOD_GET) ? TIMEOUT_SO_GET : TIMEOUT_SO_POST;
            }
            connection.setReadTimeout(timeoutSO);

            if (params != null && params.isTrustAllCerts()
                    && targetURL.getProtocol().toLowerCase(Locale.ENGLISH).equals(HTTPS)) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
                SSLUtils.trustAllHosts(httpsConn);
                httpsConn.setHostnameVerifier(SSLUtils.DO_NOT_VERIFY);
            }

            return connection;
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    private static EspHttpResponse executeHttpRequest(HttpURLConnection connection, byte[] content, boolean requireResponse) {
        if (connection == null) {
            return null;
        }

        if (!requireResponse) {
            connection.setReadTimeout(TIMEOUT_NO_RESPONSE);
        }

        try {
            log.d("executeHttpRequest url = " + connection.getURL().toString());
            Map<String, List<String>> requestProperties = connection.getRequestProperties();
            for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    log.d(key + ": " + value);
                }
            }
            if (!isEmpty(content)) {
                connection.setDoOutput(true);
                log.d("executeHttpRequest execute write " + new String(content));
                connection.setFixedLengthStreamingMode(content.length);
                connection.getOutputStream().write(content);
            } else {
                log.d("executeHttpRequest execute connect");
                connection.setFixedLengthStreamingMode(0);
            }
        } catch (IOException e) {
            connection.disconnect();
            log.w("executeHttpRequest Connect failed");
            return null;
        }

        EspHttpResponse response = null;
        try {
            response = readResponse(connection);
        } catch (IOException e) {
            log.w("executeHttpRequest read response IOException " + e.getMessage());
            if (requireResponse) {
                response = null;
            } else {
                response = new EspHttpResponse();
            }
        } finally {
            connection.disconnect();
        }

        return response;
    }

    private static EspHttpResponse readResponse(HttpURLConnection connection) throws IOException {
        EspHttpResponse response = new EspHttpResponse();

        // Get http code and message
        int code = connection.getResponseCode();
        String msg = connection.getResponseMessage();
        response.setCode(code);
        response.setMessage(msg);

        // Get http headers
        Map<String, List<String>> respHeaders = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : respHeaders.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                String statusHeader = entry.getValue().get(0);
                log.i(statusHeader);
                continue;
            }
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            String value = values.get(0);

            EspHttpHeader respHeader = new EspHttpHeader(key, value);
            response.setHeader(respHeader);
            log.i(key + ": " + value);
        }

        // Get http content
        LinkedList<Byte> contentList = new LinkedList<>();
        InputStream is;
        if (code >= 200 && code < 300) {
            is = connection.getInputStream();
        } else {
            is = connection.getErrorStream();
        }
        try {
            for (int data = is.read(); data != -1; data = is.read()) {
                contentList.add((byte) data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!contentList.isEmpty()) {
            response.setContent(DataUtil.byteListToArray(contentList));
            log.i(response.getContentString());
        }

        return response;
    }

    public static EspHttpResponse getResponseWithFixedLengthData(byte[] data) {
        if (isEmpty(data)) {
            log.w("null data");
            return null;
        }

        EspHttpResponse result = new EspHttpResponse();

        List<Byte> dataList = DataUtil.byteArrayToList(data);
        List<Byte> headerDataList = new LinkedList<>();
        List<Byte> contentDataList = new LinkedList<>();
        boolean readContent = false;
        for (Byte b : dataList) {
            if (!readContent) {
                headerDataList.add(b);
                if (headEnd(headerDataList)) {
                    readContent = true;
                }
            } else {
                contentDataList.add(b);
            }
        }
        dataList.clear();

        String headersStr = new String(DataUtil.byteListToArray(headerDataList));
        String[] headers = headersStr.split("\r\n");
        if (headers.length <= 0) {
            log.w("no status header");
            return null;
        }
        String statusHeader = headers[0];
        String[] statusValues = statusHeader.split(" ");
        if (statusValues.length < 3) {
            log.w("invalid status header " + statusHeader);
            return null;
        } else if (!statusValues[0].toUpperCase().startsWith("HTTP")) {
            log.w("invalid status protocol " + statusHeader);
            return null;
        } else {
            try {
                int statusCode = Integer.parseInt(statusValues[1]);
                result.setCode(statusCode);
            } catch (NumberFormatException nfe) {
                log.w("invalid status code " + statusHeader);
                return null;
            }

            StringBuilder statusMessage = new StringBuilder();
            for (int statusIndex = 2; statusIndex < statusValues.length; statusIndex ++) {
                statusMessage.append(statusValues[statusIndex]);
                if (statusIndex < statusValues.length - 1) {
                    statusMessage.append(" ");
                }
            }
            result.setMessage(statusMessage.toString());
        }

        for (int i = 1; i < headers.length; i++) {
            String headerStr = headers[i];
            int index = headerStr.indexOf(": ");
            if (index == -1) {
                log.w("invalid header : " + headerStr);
                return null;
            }
            String name = headerStr.substring(0, index);
            String value = headerStr.substring(index + 2, headerStr.length());
            EspHttpHeader h = new EspHttpHeader(name, value);
            result.setHeader(h);
        }

        if (!contentDataList.isEmpty()) {
            byte[] content = DataUtil.byteListToArray(contentDataList);
            result.setContent(content);
        }

        return result;
    }

    private static boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    private static boolean headEnd(List<Byte> bytes) {
        int size = bytes.size();
        if (size < 4) {
            return false;
        }

        if (bytes.get(size - 1) != '\n') {
            return false;
        }
        if (bytes.get(size - 2) != '\r') {
            return false;
        }
        if (bytes.get(size - 3) != '\n') {
            return false;
        }
        if (bytes.get(size - 4) != '\r') {
            return false;
        }

        return true;
    }

    private final static class ConstHeader extends EspHttpHeader {
        ConstHeader(String name, String value) {
            super(name, value);
        }

        @Override
        public void setValue(String value) {
            throw new IllegalArgumentException("Esp const header forbid change value");
        }
    }
}
