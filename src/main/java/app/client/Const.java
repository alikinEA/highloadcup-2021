package app.client;

public class Const {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final byte[] EMPTY_ARRAY = "[]".getBytes();
    public static final byte QUOTE = (byte)'"';
    public static final byte BRACKET_O = (byte)'[';
    public static final byte BRACKET_C = (byte)']';

    public static final int HTTP_OK = 200;
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int RATE_LIMIT = 429;
}
