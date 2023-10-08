package aaagt.jspr.web;

import java.util.List;

public record Request(
        String method,
        String route,
        List<String> headers,
        String body) {}
