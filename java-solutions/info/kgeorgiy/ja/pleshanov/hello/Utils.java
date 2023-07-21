package info.kgeorgiy.ja.pleshanov.hello;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static final String[] ARGUMENTS = {"port", "threads", "requests"};

    protected static void validateArgs(String[] args, int numberArgs) {
        if (args == null || args.length != numberArgs || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments!");
        }
    }

    protected static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(30, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static boolean validateResponse(final String responseBody, final String prefix,
                                           final int thread, final int req) {

        final String included = "Hello, " + prefix;

        try {
            var params = responseBody.substring(included.length()).split("_");
            return params.length == 2 &&
                    Integer.parseInt(params[0]) == thread && Integer.parseInt(params[1]) == req;
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            return false;
        }
    }

    public static void select(Selector selector) {
        try {
            selector.select(1000);
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void isNumber(int i, String[] args) {
        try {
            Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            System.err.println("Argument " + ARGUMENTS[i] + "must be a number");
        }
    }
}
