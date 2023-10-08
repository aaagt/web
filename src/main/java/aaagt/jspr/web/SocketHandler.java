package aaagt.jspr.web;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /**
     * Распарсить HTTP запрос
     *
     * @param in чиатель запроса
     * @return Распарсенный запрос
     * @throws IOException
     */
    private Request parseRequest(BufferedReader in) throws IOException {
        final var firstRequestLine = in.readLine();
        final var parts = firstRequestLine.split(" ");
        final var method = parts[0];
        final var route = parts[1];
        var contentLength = 0;
        System.out.printf("%s %s\n", method, route);

        List<String> headers = new ArrayList<>();
        String body = null;
        String line = null;
        while (true) {
            line = in.readLine();
            if (line == null || line.isEmpty()) {
                System.out.println("headers have been read");
                break;
            }
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(" ")[1]);
            }
            System.out.printf("header: %s\n", line);
            headers.add(line);
        }
        System.out.printf("all headers: %s\n", headers);

        if (line != null) {
            char[] charBuffer = new char[contentLength];
            in.read(charBuffer);
            body = new String(charBuffer);
            System.out.printf("body: %s\n", body);
        } else {
            System.out.println("Request without body");
            body = "";
        }

        return new Request(method, route, headers, body);
    }

    /**
     * Старт обработки запроса
     */
    @Override
    public void run() {
        try (
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var request = parseRequest(in);
            System.out.printf("Recieved request %s\n", request);

            // Если есть кастомные хэндлеры, то выполнить их
            final var handlerInfo = new HandlerInfo(request.method(), request.route());
            if (handlers.containsKey(handlerInfo)) {
                handlers.get(handlerInfo).handle(request, out);
                return;
            }

            final var path = request.route();
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
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
