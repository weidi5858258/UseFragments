package com.weidi.usefragments.tool;

import java.util.HashMap;
import java.util.Map;

public abstract class DataAccessor {

    // sourceInfo key.
    public static final String SOURCE_INFO_SERVER = "Server";
    public static final String SOURCE_INFO_CONTENT_TYPE = "ContentType";
    public static final String SOURCE_INFO_ACCEPT_RANGES = "AcceptRanges";
    public static final String SOURCE_INFO_CONTENT_FEATURES = "ContentFeatures";
    public static final String SOURCE_INFO_BYTE_SEEK_SUPPORT = "ByteSeekSupport";
    public static final String SOURCE_INFO_TIME_SEEK_SUPPORT = "TimeSeekSupport";
    public static final String SOURCE_INFO_TRANSCODE = "TransCode";
    public static final String SOURCE_INFO_CONTENT_FORMAT = "ContentFormat";
    public static final String SOURCE_INFO_STALL_INFO = "StallInfo";
    public static final String SOURCE_INFO_CONTENT_LENGTH = "ContentLength";
    public static final String SOURCE_INFO_CONTENT_DURATION = "ContentDuration";
    public static final String SOURCE_INFO_MIME_FROM_SERVER = "MimeFromServer";
    public static final String SOURCE_INFO_PROTOCOL_FROM_SERVER = "ProtocolFromServer";
    public static final String SOURCE_INFO_RECORDING = "Recording";
    public static final String SOURCE_INFO_SEEKABLE_LIMIT = "SeekableLimit";
    public static final String SOURCE_INFO_REACHED_LIVE_POINT = "ReachedLivePoint";
    public static final String SOURCE_INFO_RECORDING_COMPLETED = "RecordingCompleted";

    // Content format type.
    public static final String CONTENT_FORMAT_TYPE_OTHER = "OTHER";
    public static final String CONTENT_FORMAT_TYPE_BOX = "BOX";

    // Source info.
    protected Map<String, String> sourceInfo = new HashMap<>();
    // Content length.
    final static long INVALID_CONTENT_LENGTH = -1;
    protected long contentLength = INVALID_CONTENT_LENGTH;

    /**
     * Content open process.
     *
     * @throws ExoPlaybackException
     */
    public abstract void open() throws ExoPlaybackException;

    /**
     * Read data for content.
     *
     * @param buffer the buffer to read the data into.
     * @param offset the offset within buffer to read the data into.
     * @param size   the number of bytes to read.
     * @return the number of bytes read, or -1 if there was an error.
     * @throws ExoPlaybackException
     */
    public abstract int read(byte[] buffer, int offset, int size) throws ExoPlaybackException;

    /**
     * Content close process.
     *
     * @throws ExoPlaybackException
     */
    public abstract void close() throws ExoPlaybackException;

    /**
     * Byte seek.
     *
     * @param positionUs Seek position.
     * @throws ExoPlaybackException
     */
    public abstract void byteSeek(long positionUs) throws ExoPlaybackException;

    /**
     * Time seek.
     *
     * @param positionUs Seek position.
     * @throws ExoPlaybackException
     */
    public abstract void timeSeek(long positionUs) throws ExoPlaybackException;

    /**
     * Get for open process completion status.
     *
     * @return true:open, false: not open
     */
    public abstract boolean isOpen();

    /**
     * Get byte seek support state.
     *
     * @return Byte seek support state.
     */
    public boolean isByteSeek() {
        return "true".equals(
                sourceInfo.get(SOURCE_INFO_BYTE_SEEK_SUPPORT));
    }

    /**
     * Get time seek support state.
     *
     * @return Time seek support state.
     */
    public boolean isTimeSeek() {
        return "true".equals(
                sourceInfo.get(SOURCE_INFO_TIME_SEEK_SUPPORT));
    }

    /**
     * Get content format type.
     *
     * @return Content format type.
     */
    public String getFormatType() {
        return sourceInfo.get(SOURCE_INFO_CONTENT_FORMAT);
    }

    /**
     * Get source info.
     *
     * @return Source info.
     */
    public Map<String, String> getSourceInfo() {
        return sourceInfo;
    }

    /**
     * Get content length.
     *
     * @return Content length.
     */
    public long getSize() {
        return contentLength;
    }

    /**
     * Get Content Duration.
     *
     * @return Content Duration.
     */
    public long getDuration() {
        return -1;
    }

    public boolean forceDisconnect() {
        // Do nothing.
        return true;
    }
}
