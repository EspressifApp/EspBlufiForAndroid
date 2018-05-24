package libs.espressif.net;

public class EspHttpHeader {
    private String mName;

    private String mValue;

    public EspHttpHeader(String name, String value) {
        if (name == null) {
            throw new NullPointerException("Header name is null");
        }
        mName = name;
        if (value == null) {
            throw new NullPointerException("Header value is null");
        }
        mValue = value;
    }

    /**
     * @return the http header name
     */
    public String getName() {
        return mName;
    }

    /**
     * @return the http header value
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Set the http value.
     */
    public void setValue(String value) {
        if (value == null) {
            throw new NullPointerException("Header value is null");
        }
        mValue = value;
    }

    @Override
    public String toString() {
        return String.format("name=%s, value=%s", mName, mValue);
    }

}
