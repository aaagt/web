package aaagt.jspr.web.server.request;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
) {

    /**
     * Для получения значения параметра переданного из формы в x-www-form-urlencoded формате
     *
     * @param name Имя Необходимого параметра
     * @return Значение параметра
     */
    public Optional<String> getPostParam(String name) {
        final var rawParams = URLEncodedUtils.parse(body(), StandardCharsets.UTF_8);
        return rawParams.stream()
                .filter(param -> param.getName().equals(name))
                .map(NameValuePair::getValue)
                .findFirst();
    }

    /**
     * Получить параметры из формы переданные в x-www-form-urlencoded формате
     *
     * @return Параметры
     */
    public Map<String, List<String>> getPostParams() {
        final var rawParams = URLEncodedUtils.parse(body(), StandardCharsets.UTF_8);
        return rawParams.stream()
                .collect(Collectors.groupingBy(NameValuePair::getName,
                        Collectors.mapping(NameValuePair::getValue, Collectors.toList())));
    }
}
