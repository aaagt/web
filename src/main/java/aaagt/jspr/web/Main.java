package aaagt.jspr.web;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        System.out.println("Initializing server");
        final var server = new Server.ServerBuilder()
                .setServerPort(Settings.PORT)
                .build();

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                try {
                    final var content = """
                            [
                                {"message": "Some messsage"},
                                {"message": "Another message"}
                            ]
                            """.getBytes();
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    responseStream.write(content);
                    responseStream.flush();

                    System.out.println("Requested messages");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        server.addHandler("POST", "/messages", (Request request, BufferedOutputStream responseStream) -> {
            try {
                final var content = request.body().getBytes();
                responseStream.write((
                        "HTTP/1.1 201 Created\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();

                System.out.printf("Posted message: %s\n", request.body());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Starting server");
        server.start();

    }
}
