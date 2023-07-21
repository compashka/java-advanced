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


/**
 * HelloUDPNonblockingClient class.
 *
 * @author Pleshanov Pavel
 */
public class HelloUDPNonblockingClient extends AbstractUDPClient {
    public static void main(final String[] args) {
        main(new HelloUDPNonblockingClient(), args);
    }

    /**
     * Runs a client that sends requests to the specified host and port using UDP protocol.
     *
     * @param host    The host address of the server.
     * @param port    The port number on which the server is listening.
     * @param prefix  The prefix to include in the request message.
     * @param threads The number of threads to use for sending requests.
     */
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress socketAddress = new InetSocketAddress(host, port);

        try (final Selector selector = Selector.open()) {
            for (int i = 1; i <= threads; ++i) {
                try {
                    final DatagramChannel datagramChannel = DatagramChannel.open();
                    datagramChannel.connect(socketAddress)
                            .configureBlocking(false)
                            .register(selector, SelectionKey.OP_WRITE, new Attachment(i, 1));
                } catch (final IOException e) {
                    System.err.println("Error occurred while opening DatagramChannel");
                }
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                Utils.select(selector);
                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                if (!selectedKeys.isEmpty()) {
                    for (final Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext(); ) {
                        final SelectionKey selectionKey = iterator.next();
                        final DatagramChannel datagramChannel = (DatagramChannel) selectionKey.channel();
                        if (selectionKey.isWritable()) {
                            send(datagramChannel, selectionKey, prefix);
                        } else if (selectionKey.isReadable() &&
                                receive(datagramChannel, selectionKey, prefix, requests)) {
                            try {
                                datagramChannel.close();
                            } catch (final IOException e) {
                                System.err.println("Error occurred while closing DatagramChannel");
                            }
                        }
                        iterator.remove();
                    }
                } else {
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                }
            }
        } catch (final IOException e) {
            System.err.println("Error occurred while opening Selector");
        }

    }

    private boolean receive(final DatagramChannel datagramChannel, final SelectionKey selectionKey, final String prefix,
                            final int totalRequests) {
        final Attachment attachment = (Attachment) selectionKey.attachment();

        int currRequest = attachment.getRequest();
        final int currThread = attachment.getThread();
        final ByteBuffer buffer = ByteBuffer.allocate(500);
        try {
            datagramChannel.receive(buffer);
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }

        final String responseBody = UTF_8.decode(buffer.flip()).toString();
        if (Utils.validateResponse(responseBody, prefix, currThread, currRequest)) {
            System.out.println(responseBody);
            attachment.putRequest(++currRequest);
        }
        selectionKey.interestOps(SelectionKey.OP_WRITE);
        return currRequest == (totalRequests + 1);
    }

    private void send(final DatagramChannel datagramChannel, final SelectionKey selectionKey, final String prefix) {
        final Attachment attachment = (Attachment) selectionKey.attachment();
        final String requestBody = getRequestBody(prefix, attachment.getThread(), attachment.getRequest());
        try {
            datagramChannel.write(ByteBuffer.wrap(requestBody.getBytes(UTF_8)));
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    private static class Attachment {
        private final int thread;
        private int request;

        public Attachment(final int thread, final int request) {
            this.thread = thread;
            this.request = request;
        }

        public int getThread() {
            return thread;
        }

        public int getRequest() {
            return request;
        }

        public void putRequest(final int request) {
            this.request = request;
        }
    }
}
