package aaagt.jspr.web.server.request;

public enum RequestErrors {
    NONE,
    NO_REQUESTLINE,
    NOT_ALL_ELEMENTS_IN_REQUESTLINE,
    REQUEST_METHOD_IS_NOT_ALLOWED,
    WRONG_URI_SINTAX,
    HEADERS_DELIMITER_NOT_FOUND
}
