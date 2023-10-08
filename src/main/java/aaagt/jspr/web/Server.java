package aaagt.jspr.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int serverPort;
    private final int threadPoolSize;
    private final Map<HandlerInfo, Handler> handlers = new HashMap<>();

    private Server(int serverPort) {
        this.serverPort = serverPort;

        this.threadPoolSize = Settings.TREAD_POOL_SIZE;
    }

    public void addHandler(String method, String route, Handler handler) {
        handlers.put(new HandlerInfo(method, route), handler);
    }

    public void start() {
        try (
                var serverSocket = new ServerSocket(serverPort);
                var executorService = Executors.newFixedThreadPool(threadPoolSize)
        ) {
            while (true) {
                waitConnection(serverSocket, executorService);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitConnection(ServerSocket serverSocket, ExecutorService executorService) throws IOException {
        final var socket = serverSocket.accept();
        System.out.println("Accepting connection");
        final var socketHandlerTask = new SocketHandler(socket, handlers);
        executorService.submit(socketHandlerTask);
    }

    public static class ServerBuilder {

        private int serverPort = 8080;

        public ServerBuilder setServerPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public Server build() {
            return new Server(serverPort);
        }
    }

}
