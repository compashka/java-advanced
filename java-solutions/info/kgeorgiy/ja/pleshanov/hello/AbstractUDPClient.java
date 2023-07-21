package info.kgeorgiy.ja.pleshanov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

/**
 * An abstract class implementing the {@link HelloClient} interface for UDP-based clients.
 *
 * @author Pleshanov Pavel
 */
public abstract class AbstractUDPClient implements HelloClient {
    protected static final Charset UTF_8 = StandardCharsets.UTF_8;

    protected static void main(HelloClient client, String[] args) {
        Utils.validateArgs(args, 5);
        IntStream.range(1, 5).filter(i -> i != 2).forEach(i -> Utils.isNumber(i, args));

        client.run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[3]));
    }

    protected static String getRequestBody(final String prefix, final int currThread, final int currRequest) {
        return String.format("%s%d_%d", prefix, currThread, currRequest);
    }
}
