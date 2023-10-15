package aaagt.jspr.web;

import aaagt.jspr.web.server.Handler;
import aaagt.jspr.web.server.Server;
import aaagt.jspr.web.server.Settings;
import aaagt.jspr.web.server.request.RequestData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {

        System.out.println("Initializing server");
        final var server = new Server.ServerBuilder()
                .setServerPort(Settings.PORT)
                .build();

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(RequestData request, BufferedOutputStream responseStream) {
                try {
                    final var gson = new Gson();
                    final Type typeObject = new TypeToken<HashMap>() {}.getType();
                    final var params = gson.toJson(request.queryParams(), typeObject);

                    final var content = String.format("""
                            {
                                "messages":[
                                    {"message": "Some messsage"},
                                    {"message": "Another message"}
                                ],
                                "query_params": %s
                            }
                            """, params).getBytes();
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
        server.addHandler("POST", "/messages", (RequestData request, BufferedOutputStream responseStream) -> {
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

        // Получить данные из формы в формате x-www-form-urlencoded
        server.addHandler("POST", "/forms.html", (RequestData request, BufferedOutputStream responseStream) -> {
            try {
                final var params = request.getPostParams();
                System.out.println(params);

                final var content = """
                        {
                            "message": "Logged in",
                            "login": "%s"
                        }
                        """.formatted(params.get("login").getFirst()).getBytes();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();

                System.out.printf("logged in: %s\n", request.body());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Starting server");
        server.start();

    }
}
