import java.io.BufferedOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface Handler {
    public void handle(Request request, BufferedOutputStream responseStream) throws IOException;
}