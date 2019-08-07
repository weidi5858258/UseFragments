package com.weidi.usefragments.tool;

import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpAccessor extends DataAccessor {
    private static final String TAG =
            HttpAccessor.class.getSimpleName();
    private static final boolean DEBUG = true;

    // Timeout time connect(ms).
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5 * 60 * 1000;
    // Timeout time from read(ms).
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 5 * 60 * 1000;

    // Request method to server.
    private static final String REQUEST_METHOD_HEAD = "HEAD";
    private static final String REQUEST_METHOD_GET = "GET";

    // Regular expression.
    private static final Pattern RESPONSE_HEADER_CONTENT_RANGE =
            Pattern.compile("^bytes (\\d+)-(\\d+)/(\\d+)$");

    private URL url;
    private Map<String, String> headerInfoEx = null;
    private HttpURLConnection connection = null;
    private InputStream inputStream = null;
    private boolean needHeadRequest = true;

    public HttpAccessor(URL url, Map<String, String> headers) {
        this(url, headers, true);
    }

    public HttpAccessor(URL url, Map<String, String> headers, boolean needHeadRequest) {
        this.url = url;
        this.headerInfoEx = headers;
        this.needHeadRequest = needHeadRequest;
    }

    public void open() throws ExoPlaybackException {
        try {
            int responseCode = -1;
            if (needHeadRequest) {
                // Request HEAD
                connection = requestConnection(REQUEST_METHOD_HEAD, null);

                responseCode = connection.getResponseCode();
                if (!isValidResponseCode(responseCode)) {
                    MLog.w(TAG, "Invalid HEAD response code = " + responseCode + ".");
                }
            }

            // Request GET
            connection = requestConnection(REQUEST_METHOD_GET, null);
            responseCode = connection.getResponseCode();
            if (!isValidResponseCode(responseCode)) {
                if (connection != null) {
                    connection.disconnect();
                    connection = null;
                }
                MLog.e(TAG, "Server died. response code = " + responseCode);
                String errorMsg = "";
                if (responseCode >= 500) {
                    errorMsg = ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_SERVER_DIED, "Server died.");
                } else {
                    errorMsg = ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, "Server died.");
                }
                throw new ExoPlaybackException(errorMsg);
            }

            inputStream = connection.getInputStream();

            // Call updateSourceInfo
            updateSourceInfo(connection);
        } catch (IOException e) {
            // EXOPLAYER_ERROR_IO
            throw new ExoPlaybackException(
                    ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, e.getMessage()));
        }
    }

    /**
     * @see DataAccessor#read(byte[], int, int)
     */
    public int read(byte[] buffer, int offset, int size) throws ExoPlaybackException {
        if (inputStream == null) {
            // EXOPLAYER_ERROR_IO
            throw new ExoPlaybackException(
                    ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, "File not open."));
        }

        try {
            int readSize = inputStream.read(buffer, offset, size);
            return readSize;
        } catch (IOException e) {
            // EXOPLAYER_ERROR_IO
            throw new ExoPlaybackException(
                    ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, e.getMessage()));
        }
    }

    /**
     * @see DataAccessor#clone()
     */
    public void close() throws ExoPlaybackException {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        } catch (IOException e) {
            // EXOPLAYER_ERROR_IO
            throw new ExoPlaybackException(
                    ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, e.getMessage()));
        }
    }

    /**
     * @see DataAccessor#byteSeek(long)
     */
    public void byteSeek(long positionUs) throws ExoPlaybackException {
        try {
            // Stream and Connection close
            close();

            Map<String, String> addRequest = new HashMap<String, String>();
            addRequest.put("Range", "bytes=" + positionUs + "-");

            // Request GET
            connection = requestConnection(REQUEST_METHOD_GET, addRequest);
            int responseCode = connection.getResponseCode();
            if (!isValidResponseCode(responseCode)) {
                MLog.e(TAG, "Server died. response code = " + responseCode);
                String errorMsg = "";
                if (responseCode >= 500) {
                    errorMsg = ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_SERVER_DIED, "Server died.");
                } else {
                    errorMsg = ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, "Server died.");
                }
                throw new ExoPlaybackException(errorMsg);
            }

            inputStream = connection.getInputStream();

            if (contentLength < 0) {
                updateContentLength(connection);
            }
        } catch (IOException e) {
            // EXOPLAYER_ERROR_IO
            throw new ExoPlaybackException(
                    ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, e.getMessage()));
        }
    }

    /**
     * @see DataAccessor#timeSeek(long)
     */
    public void timeSeek(long positionUs) throws ExoPlaybackException {
        // This function is not supported.
        MLog.e(TAG, "This function is not supported.");
    }

    /**
     * @see DataAccessor#isOpen()
     */
    public boolean isOpen() {
        if (connection != null) {
            return true;
        } else {
            return false;
        }
    }

    /*-------------*/
  /* Private API */
  /*-------------*/
    private HttpURLConnection requestConnection(
            String requestMethod, Map<String, String> addRequest) throws ExoPlaybackException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) this.url.openConnection();

            conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLIS);
            conn.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
            conn.setRequestMethod(requestMethod);
            conn.setInstanceFollowRedirects(true);

            if (headerInfoEx != null) {
                for (String key : headerInfoEx.keySet()) {
                    conn.setRequestProperty(key, headerInfoEx.get(key));
                }
            }

            if (addRequest != null) {
                for (String key : addRequest.keySet()) {
                    conn.setRequestProperty(key, addRequest.get(key));
                }
            }

            conn.connect();

        } catch (IOException e) {
            // EXOPLAYER_ERROR_IO
            throw new ExoPlaybackException(
                    ExoPlaybackException.createErrorMessage(
                            ExoPlaybackException.EXOPLAYER_ERROR_IO, e.getMessage()));
        }

        MLog.d(TAG, "Response header.");
        Map<String, List<String>> headers = conn.getHeaderFields();
        for (String key : headers.keySet()) {
            List<String> valueList = headers.get(key);
            StringBuilder values = new StringBuilder();
            for (String value : valueList) {
                values.append(value + " ");
            }
            MLog.d(TAG, key + " : " + values.toString());
        }

        return conn;
    }

    private void updateSourceInfo(HttpURLConnection connection) {
        // Set sourceInfo
        String keyValue = "";
        keyValue = connection.getHeaderField("Server");
        if (keyValue != null) {
            sourceInfo.put(DataAccessor.SOURCE_INFO_SERVER, keyValue);
        } else {
            sourceInfo.put(DataAccessor.SOURCE_INFO_SERVER, "unkown");
        }

        keyValue = connection.getHeaderField("Content-Type");
        if (keyValue != null) {
            sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_TYPE, keyValue);
        } else {
            sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_TYPE, "unkown");
        }

        keyValue = connection.getHeaderField("Accept-Ranges");
        if (keyValue != null) {
            sourceInfo.put(DataAccessor.SOURCE_INFO_ACCEPT_RANGES, keyValue);
        } else {
            sourceInfo.put(DataAccessor.SOURCE_INFO_ACCEPT_RANGES, "unkown");
        }
        sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_FEATURES, "unkown");

        if ("bytes".equals(keyValue)) {
            sourceInfo.put(DataAccessor.SOURCE_INFO_BYTE_SEEK_SUPPORT, "true");
        } else {
            sourceInfo.put(DataAccessor.SOURCE_INFO_BYTE_SEEK_SUPPORT, "false");
        }
        sourceInfo.put(DataAccessor.SOURCE_INFO_TIME_SEEK_SUPPORT, "false");
        sourceInfo.put(DataAccessor.SOURCE_INFO_STALL_INFO, "false");
        sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_FORMAT, "BOX");

        updateContentLength(connection);

        for (String key : sourceInfo.keySet()) {
            MLog.i(TAG, key + ":" + sourceInfo.get(key));
        }
    }

    private void updateContentLength(HttpURLConnection connection) {
        sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_LENGTH, "-1");
        contentLength = -1;

        String contentLengthHeader = connection.getHeaderField("Content-Length");
        if (!TextUtils.isEmpty(contentLengthHeader)) {
            try {
                contentLength = Long.parseLong(contentLengthHeader);
            } catch (NumberFormatException e) {
                MLog.e(TAG, "Unexpected Content-Length [" + contentLengthHeader + "]");
            }
        }

        String contentRangeHeader = this.connection.getHeaderField("Content-Range");
        if (!TextUtils.isEmpty(contentRangeHeader)) {
            try {
                Matcher matcher = RESPONSE_HEADER_CONTENT_RANGE.matcher(contentRangeHeader);
                if (matcher.find()) {
                    long contentLengthFromRange =
                            Long.parseLong(matcher.group(2)) - Long.parseLong(matcher.group(1)) + 1;
                    if (contentLengthFromRange < 0) {
                        contentLength = contentLengthFromRange;
                    } else if (contentLength != contentLengthFromRange) {
                        contentLength = Math.max(contentLength, contentLengthFromRange);
                    }
                }
            } catch (NumberFormatException e) {
                MLog.e(TAG, "Unexpected Content-Range [" + contentRangeHeader + "]");
            }
        }
        sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_LENGTH, Long.toString(contentLength));
        sourceInfo.put(DataAccessor.SOURCE_INFO_CONTENT_DURATION, "-1");
    }

    private boolean isValidResponseCode(int code) {
        switch (code) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:
                return true;
            default:
                return false;
        }
    }
}
