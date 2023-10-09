package aaagt.jspr.web.server;

import java.util.List;

public class Settings {

    public static final List<String> VALID_PATHS = List.of(
            "/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html",
            "/styles.css",
            "/app.js",
            "/links.html",
            "/forms.html",
            "/classic.html",
            "/events.html",
            "/events.js");

    public static final int PORT = 9999;

    public static final int TREAD_POOL_SIZE = 64;
}
