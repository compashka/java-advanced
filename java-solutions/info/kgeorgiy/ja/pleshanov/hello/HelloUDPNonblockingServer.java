package info.kgeorgiy.ja.pleshanov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;


/**
 * HelloUDPNonblockingServer class.
 *
 * @author Pleshanov Pavel
 */
public class HelloUDPNonblockingServer extends AbstractUDPServer {
    private Selector selector;
    private DatagramChannel datagramChannel;

    public static void main(final String[] args) {
        main(new HelloUDPNonblockingServer(), args);
    }

    /**
     * Starts a server on the specified port with the given number of threads.
     *
     * @param port    The port number on which the server should listen.
     * @param threads The number of threads to use for handling incoming requests.
     */
    @Override
    public void start(final int port, final int threads) {
        executorService = Executors.newSingleThreadExecutor();
        try {
            selector = Selector.open();
        } catch (final IOException e) {
            System.err.println("Error occurred while opening selector");
        }
        try {
            final SocketAddress socketAddress = new InetSocketAddress(port);
            datagramChannel = DatagramChannel.open();
            datagramChannel.bind(socketAddress)
                    .configureBlocking(false)
                    .register(selector, SelectionKey.OP_READ);
            executorService.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    Utils.select(selector);
                    final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    for (final Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext(); ) {
                        final SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isReadable()) {
                            final DatagramChannel datagramChannel = (DatagramChannel) selectionKey.channel();
                            final ByteBuffer byteBuffer = ByteBuffer.allocate(500);
                            try {
                                SocketAddress socketRecAddress = datagramChannel.receive(byteBuffer);
                                final String responseBody = getResponseBody(
                                        UTF_8.decode(byteBuffer.flip()).toString());
                                try {
                                    datagramChannel.send(ByteBuffer.wrap(
                                            responseBody.getBytes(UTF_8)), socketRecAddress);
                                } catch (final IOException e) {
                                    System.err.println("Error occurred while sending response");
                                }
                            } catch (final IOException e) {
                                System.err.println("Error occurred while receiving byteBuffer");
                            }
                        }
                        iterator.remove();
                    }
                }
            });
        } catch (final IOException e) {
            System.err.println("Error occurred while opening DatagramChannel");
        }
    }

    /**
     * Closes the server, releasing any resources used.
     */
    @Override
    public void close() {
        try {
            selector.close();
            datagramChannel.close();
        } catch (final IOException e) {
            System.err.println("Error occurred while closing selector or datagramChannel");
        }
        Utils.shutdownAndAwaitTermination(executorService);
    }
}
