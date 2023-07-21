package info.kgeorgiy.ja.pleshanov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.Executors;


/**
 * HelloUDPServer class.
 *
 * @author Pleshanov Pavel
 */
public class HelloUDPServer extends AbstractUDPServer {
    private DatagramSocket socket;


    public static void main(String[] args) {
        main(new HelloUDPServer(), args);
    }

    /**
     * Starts a server on the specified port with the given number of threads.
     *
     * @param port    The port number on which the server should listen.
     * @param threads The number of threads to use for handling incoming requests.
     */
    @Override
    public void start(int port, int threads) {
        executorService = Executors.newFixedThreadPool(threads);
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            System.err.println(e.getMessage());
        }

        for (int i = 0; i < threads; ++i) {
            executorService.submit(() -> {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        final int socketBufferSize = socket.getReceiveBufferSize();
                        final DatagramPacket request = new DatagramPacket(
                                new byte[socketBufferSize],
                                socketBufferSize
                        );
                        socket.receive(request);
                        final String requestBody = new String(
                                request.getData(), request.getOffset(),
                                request.getLength(), UTF_8
                        );
                        final String responseBody = getResponseBody(requestBody);

                        final DatagramPacket response = new DatagramPacket(
                                responseBody.getBytes(UTF_8),
                                responseBody.length(),
                                request.getSocketAddress()
                        );
                        socket.send(response);
                    } catch (SocketException e) {
                        System.err.println(e.getMessage());
                    } catch (IOException e) {
                        System.err.println("I/O error while sending response" + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Closes the server, releasing any resources used.
     */
    @Override
    public void close() {
        socket.close();
        Utils.shutdownAndAwaitTermination(executorService);
    }
}
