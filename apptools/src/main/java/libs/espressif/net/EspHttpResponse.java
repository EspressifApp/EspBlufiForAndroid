package libs.espressif.net;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EspHttpResponse {
    private final Map<String, EspHttpHeader> mHeaders = new HashMap<>();

    private int mCode;
    private String mMessage;
    private byte[] mContent;

    /**
     * Get http code
     *
     * @return http status
     */
    public int getCode() {
        return mCode;
    }

    /**
     * Set http code
     *
     * @param code http status
     */
    public void setCode(int code) {
        mCode = code;
    }

    /**
     * Get http message
     *
     * @return http message
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Set http message
     *
     * @param msg http message
     */
    public void setMessage(String msg) {
        mMessage = msg;
    }

    /**
     * Get http content data
     *
     * @return http content data
     */
    public byte[] getContent() {
        return mContent;
    }

    /**
     * Set http content data
     *
     * @param content http content data
     */
    public void setContent(byte[] content) {
        mContent = content;
    }

    /**
     * Get http content string
     *
     * @return http content string
     */
    public String getContentString() {
        if (mContent == null) {
            return null;
        } else {
            return new String(mContent);
        }
    }

    /**
     * Get http content json
     *
     * @return http content json
     * @throws JSONException if content is not json format
     */
    public JSONObject getContentJSON() throws JSONException {
        if (mContent == null) {
            return null;
        } else {
            return new JSONObject(new String(mContent));
        }
    }

    /**
     * Set the header
     *
     * @param header the header
     */
    public void setHeader(EspHttpHeader header) {
        if (header == null) {
            return;
        }

        synchronized (mHeaders) {
            String key = header.getName().toLowerCase();
            mHeaders.put(key, header);
        }
    }

    /**
     * Get all headers
     *
     * @return all headers
     */
    public List<EspHttpHeader> getHeaders() {
        synchronized (mHeaders) {
            return new ArrayList<>(mHeaders.values());
        }
    }

    /**
     * Set the headers
     *
     * @param headers the headers
     */
    public void setHeaders(Collection<EspHttpHeader> headers) {
        synchronized (mHeaders) {
            for (EspHttpHeader header : headers) {
                String key = header.getName().toLowerCase();
                mHeaders.put(key, header);
            }
        }
    }

    /**
     * Get the requested name header
     *
     * @param name the name of the header
     * @return the requested header
     */
    public EspHttpHeader findHeader(String name) {
        synchronized (mHeaders) {
            return mHeaders.get(name.toLowerCase());
        }
    }

    public String findHeaderValue(String name) {
        EspHttpHeader header = findHeader(name);
        return header == null ? null : header.getValue();
    }
}
