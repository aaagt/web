package aaagt.jspr.web.server;

import aaagt.jspr.web.server.request.RequestErrors;
import aaagt.jspr.web.server.request.RequestReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Хэндлер для обработки конкретных подключений
 */
public class SocketHandler implements Runnable {

    final Socket socket;
    final Map<HandlerInfo, Handler> handlers;
    final List<String> validPaths;

    public SocketHandler(Socket socket, Map<HandlerInfo, Handler> handlers) {
        this.socket = socket;
        this.handlers = handlers;

        this.validPaths = Settings.VALID_PATHS;
    }

    private static void response(BufferedOutputStream out, String headers, String body) throws IOException {
        out.write(headers.getBytes());
        if (body != null) {
            out.write(body.getBytes());
        }
        out.flush();
    }

    private static void errorResponse(BufferedOutputStream out, String statusCode, String statusMessage) throws IOException {
        final var headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        response(out, headers, null);
    }

    /**
     * Старт обработки запроса
     */
    @Override
    public void run() {
        try (
                var in = new BufferedInputStream(socket.getInputStream());
                var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var request = RequestReader.read(in);
            if (request.error() != RequestErrors.NONE) {
                errorResponse(out, "400", "Bad Request");
                return;
            }

            final var requestData = request.data();
            System.out.printf("Recieved request %s\n", requestData);

            // Если есть кастомные хэндлеры, то выполнить их
            final var handlerInfo = new HandlerInfo(requestData.method(), requestData.path());
            if (handlers.containsKey(handlerInfo)) {
                handlers.get(handlerInfo).handle(requestData, out);
                return;
            }

            final var path = requestData.path();
            if (!validPaths.contains(path)) {
                errorResponse(out, "404", "Not Found");
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var body = template.replace("{time}", LocalDateTime.now().toString());
                final var headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + body.getBytes().length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
                response(out, headers, body);
                return;
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeSocket();
        }
    }

    /**
     * Закрыть сокет
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
