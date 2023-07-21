package info.kgeorgiy.ja.pleshanov.hello;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * HelloUDPClient class.
 *
 * @author Pleshanov Pavel
 */
public class HelloUDPClient extends AbstractUDPClient {

    public static void main(String[] args) {
        main(new HelloUDPClient(), args);
    }

    /**
     * Runs a client that sends requests to the specified host and port using UDP protocol.
     *
     * @param host     The host address of the server.
     * @param port     The port number on which the server is listening.
     * @param prefix   The prefix to include in the request message.
     * @param threads  The number of threads to use for sending requests.
     * @param requests The number of requests to send per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        final SocketAddress socketAddress = new InetSocketAddress(host, port);

        for (int i = 1; i <= threads; ++i) {
            final int currThread = i;
            executorService.submit(() -> {
                try (final DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(500);
                    final int socketBufferSize = socket.getReceiveBufferSize();
                    final DatagramPacket response = new DatagramPacket(
                            new byte[socketBufferSize],
                            socketBufferSize
                    );

                    for (int req = 1; req <= requests; ++req) {
                        final String requestBody = getRequestBody(prefix, currThread, req);
                        final DatagramPacket request = new DatagramPacket(
                                requestBody.getBytes(UTF_8),
                                requestBody.length(),
                                socketAddress
                        );
                        while (true) {
                            try {
                                socket.send(request);
                                socket.receive(response);
                                final String responseBody = new String(response.getData(), response.getOffset(),
                                        response.getLength(), UTF_8);
                                if (Utils.validateResponse(responseBody, prefix, currThread, req)) {
                                    System.out.println(responseBody);
                                    break;
                                }
                            } catch (SocketTimeoutException e) {
                                System.out.println("Timeout error: " + e.getMessage());
                            } catch (IOException e) {
                                System.out.println("I/O error while sending request" + e.getMessage());
                            }
                        }

                    }
                } catch (final SocketException e) {
                    System.err.println(e.getMessage());
                }
            });
        }
        Utils.shutdownAndAwaitTermination(executorService);
    }
}
