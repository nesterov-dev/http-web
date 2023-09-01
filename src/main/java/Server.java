import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class Server {
    private int socketInt;
    private final List validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private String path;
    private ExecutorService executor;
    private HashMap<String, Map<String, Handler>> handlers;

    public Server(int socketInt, int poolSizeThreads) {
        this.socketInt = socketInt;
        executor = Executors.newFixedThreadPool(poolSizeThreads);
    }

    public void proceedConnection(Socket socket) throws IOException {
        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final var requestLine = in.readLine();
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            // just close socket
            socket.close();
            return;
        }

        path = parts[1];
        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } else {
            defaultHandler(out, path);
        }
        if (request == null || !handlers.containsKey(request.getMethod())) {
            responseWithoutContent(out, "404", "Not found");
        }

        Map<String, Handler> handlerMap = handlers.get(request.getMethod());
        String requestPath = request.getPath();
        if (handlerMap.containsKey(requestPath)) {
            Handler handler = handlerMap.get(requestPath);
            handler.handle(request, out);

        }

        public void defaultHandler(BufferedOutputStream out, String path) throws IOException {
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
        }


        public void start() {
            try (final var serverSocket = new ServerSocket(socketInt)) {
                while (!serverSocket.isClosed()) {
                    try (final var socket = serverSocket.accept()) {
                        executor.execute(() -> {
                            try {
                                proceedConnection(socket);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (IOException e) {
                        System.out.println("Произошла ошибка типа IOException, это значит что произошло какое-либо исключение ввода-вывода");
                    } finally {
                        executor.shutdown();
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void addHandler(String method, String path, Handler handler) {
            if (!handlers.containsKey(method)) {
                handlers.put(method, new HashMap<>());
            }
            handlers.get(method).put(path, handler);
        }

        protected void responseWithoutContent(BufferedOutputStream out, String responseCode, String responseStatus) throws IOException {
            out.write((
                    "HTTP/1.1 " + responseCode + " " + responseStatus + "\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

    }