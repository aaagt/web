package aaagt.jspr.web.server.request;

import java.util.List;
import java.util.Map;

public record RequestData(

        /**
         * Метод запроса
         * Например GET, POST, PUT
         */
        String method,

        /**
         * Путь запроса
         * Например в строке http://localhost:9999/messages?ppp=dsdf&dffgss=123 это будет /messages
         */
        String path,

        /**
         * Параметры запроса
         * Например в строке http://localhost:9999/messages?p1=dsdf&p2=123
         * это будет:
         * p1 - dsdf
         * p2 - 123
         * Одинаковые параметры группируются в лист
         */
        Map<String, List<String>> queryParams,

        /**
         * Заголовки запроса
         */
        List<String> headers,

        /**
         * Тело запроса
         */
        String body
) {}
