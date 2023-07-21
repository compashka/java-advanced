package info.kgeorgiy.ja.pleshanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;


/**
 * An abstract class implementing the {@link HelloServer} interface for UDP-based servers.
 *
 * @author Pleshanov Pavel
 */
public abstract class AbstractUDPServer implements HelloServer {
    protected ExecutorService executorService;
    protected static final Charset UTF_8 = StandardCharsets.UTF_8;

    protected static void main(HelloServer server, String[] args) {
        Utils.validateArgs(args, 2);
        IntStream.range(0, 2).forEach(i -> Utils.isNumber(i, args));

        server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    }

    protected static String getResponseBody(final String requestBody) {
        return "Hello, " + requestBody;
    }
}
