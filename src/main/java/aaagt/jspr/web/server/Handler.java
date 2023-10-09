package aaagt.jspr.web.server;

import aaagt.jspr.web.server.request.RequestData;

import java.io.BufferedOutputStream;

@FunctionalInterface
public interface Handler {
    void handle(RequestData request, BufferedOutputStream responseStream);
}
