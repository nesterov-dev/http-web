import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        int poolSizeThreads = 64;
        int port = 9999;
        Server server = new Server(port, poolSizeThreads);

        // код инициализации сервера (из вашего предыдущего ДЗ)

        // добавление хендлеров (обработчиков)
        Handler handler = new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream out) {
                try {
                    if (request.getPath().equals("/classic.html")) {
                        final var template = Files.readString(request.getFilePath());
                        final var content = template.replace(
                                "{time}",
                                LocalDateTime.now().toString()
                        ).getBytes();
                        out.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + request.getMimeType() + "\r\n" +
                                        "Content-Length: " + content.length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.write(content);
                        out.flush();
                        return;
                    }

                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + request.getMimeType() + "\r\n" +
                                    "Content-Length: " + request.getLenght() + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(request.getFilePath(), out);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        server.addHandler("GET", "/index.html", handler);
        server.addHandler("GET", "/classic.html", handler);

        server.start();
    }
}