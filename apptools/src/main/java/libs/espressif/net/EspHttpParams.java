package libs.espressif.net;

public class EspHttpParams {
    private int mConnectTimeout = -1;
    private int mSOTimeout = -1;
    private int mTryCount = 1;
    private boolean mRequireResponse = true;
    private boolean mTrustAllCerts = false;

    /**
     * Get the connect timeout milliseconds
     *
     * @return the connect timeout
     */
    public int getConnectTimeout() {
        return mConnectTimeout;
    }

    /**
     * Set the connect timeout milliseconds
     *
     * @param timeout the connect timeout
     */
    public void setConnectTimeout(int timeout) {
        mConnectTimeout = timeout;
    }

    /**
     * Get the so timeout milliseconds
     *
     * @return the so timeout
     */
    public int getSOTimeout() {
        return mSOTimeout;
    }

    /**
     * Set the so timeout milliseconds
     *
     * @param timeout the so timeout
     */
    public void setSOTimeout(int timeout) {
        mSOTimeout = timeout;
    }

    /**
     * Get connect task try count
     *
     * @return connect try count
     */
    public int getTryCount() {
        return mTryCount;
    }

    /**
     * Set connect task try count.
     *
     * @param count connect try count
     */
    public void setTryCount(int count) {
        mTryCount = count;
    }

    /**
     * Get is response required.
     *
     * @return true if response is required
     */
    public boolean isRequireResponse() {
        return mRequireResponse;
    }

    /**
     * Set response is requered, if false, the connection will disconnect immediately after posting http request.
     */
    public void setRequireResponse(boolean require) {
        mRequireResponse = require;
    }

    public boolean isTrustAllCerts() {
        return mTrustAllCerts;
    }

    public void setTrustAllCerts(boolean trustAllCerts) {
        mTrustAllCerts = trustAllCerts;
    }
}
