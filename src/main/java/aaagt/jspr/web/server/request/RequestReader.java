package aaagt.jspr.web.server.request;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestReader {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final List<String> ALLOWED_METHODS = List.of(GET, POST);

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Читать HTTP запрос
     *
     * @param in чиатель запроса
     * @return Прочитанный запрос
     * @throws IOException
     */
    public static Request read(BufferedInputStream in) throws IOException {
        // лимит на request line + заголовки
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return new Request(null, RequestErrors.NO_REQUESTLINE);
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return new Request(null, RequestErrors.NOT_ALL_ELEMENTS_IN_REQUESTLINE);
        }

        final var method = requestLine[0];
        if (!ALLOWED_METHODS.contains(method)) {
            return new Request(null, RequestErrors.REQUEST_METHOD_IS_NOT_ALLOWED);
        }
        System.out.println(method);

        // Парсим строку запроса
        final URI uri;
        try {
            uri = new URI(requestLine[1]);
        } catch (URISyntaxException e) {
            return new Request(null, RequestErrors.WRONG_URI_SINTAX);
        }

        // Забираем путь запроса
        final var path = uri.getPath();
        System.out.println(path);

        // Забираем параметры запроса
        final var rawParams = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        final var param = rawParams.stream()
                .peek(p -> System.out.println(p.getName() + " - " + p.getValue()))
                .collect(Collectors.groupingBy(NameValuePair::getName,
                        Collectors.mapping(NameValuePair::getValue, Collectors.toList())));

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return new Request(null, RequestErrors.HEADERS_DELIMITER_NOT_FOUND);
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        final var body = readBody(in, method, headersDelimiter, headers);
        System.out.println(body);

        final var requestData = new RequestData(method, path, param, headers, body);
        return new Request(requestData, RequestErrors.NONE);
    }

    private static String readBody(BufferedInputStream in, String method, byte[] headersDelimiter, List<String> headers) throws IOException {
        // для GET тела нет
        if (method.equals(GET)) {
            return null;
        }

        // Пропускаем байты до разделителя заголовка от тела
        in.skip(headersDelimiter.length);

        // вычитываем Content-Length, чтобы прочитать body
        final var contentLength = extractHeader(headers, "Content-Length");
        if (contentLength.isEmpty()) {
            return null;
        }

        // Парсим считанную длину
        final var length = Integer.parseInt(contentLength.get());

        // Читаем тело
        final var bodyBytes = in.readNBytes(length);

        return new String(bodyBytes);
    }
}
