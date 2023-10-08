package aaagt.jspr.web;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int serverPort;
    private final int threadPoolSize;
    private final Map<HandlerInfo, Handler> handlers = new HashMap<>();

    private Server(int serverPort, int threadPoolSize) {
        this.serverPort = serverPort;
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * Добавление кастомных хэндлеров
     *
     * @param method  метод запроса (например GET)
     * @param route   путь запроса
     * @param handler обработчик
     */
    public void addHandler(String method, String route, Handler handler) {
        handlers.put(new HandlerInfo(method, route), handler);
    }

    /**
     * Запуск сервера
     */
    public void start() {
        try (
                var serverSocket = new ServerSocket(serverPort);
                var executorService = Executors.newFixedThreadPool(threadPoolSize)
        ) {
            while (true) {
                final var socket = serverSocket.accept();
                proccessConnection(socket, executorService);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обработка конкретного подключения
     *
     * @param socket          подключение
     * @param executorService сервис запуска потоков обработки
     * @throws IOException
     */
    private void proccessConnection(Socket socket, ExecutorService executorService) throws IOException {
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
            return new Server(serverPort, Settings.TREAD_POOL_SIZE);
        }
    }

}
