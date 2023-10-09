package aaagt.jspr.web.server.request;

import java.util.List;

public record RequestData(
        String method,
        String path,
        List<String> headers,
        String body) {}
