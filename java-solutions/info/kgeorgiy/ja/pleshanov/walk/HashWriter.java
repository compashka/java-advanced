package info.kgeorgiy.ja.pleshanov.walk;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class HashWriter {
    private final Writer writer;
    private final MessageDigest messageDigest;
    private static final byte[] buff = new byte[1024];
    private static final BigInteger NULL_HASH = BigInteger.ZERO;

    public HashWriter(final Writer writer, final MessageDigest messageDigest) {
        this.writer = writer;
        this.messageDigest = messageDigest;
    }

    public void writeNullHash(String path) {
        writeHash(NULL_HASH, path);
    }

    public void writeHash(String path) {
        writeHash(calculateHash(Path.of(path)), path);
    }

    private BigInteger calculateHash(Path path) {
        messageDigest.reset();

        int count;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(path))) {
            while ((count = bufferedInputStream.read(buff)) > 0) {
                messageDigest.update(buff, 0, count);
            }
        } catch (IOException e) {
            System.err.println("Error in reading from inputFile " + e.getMessage());
            return NULL_HASH;
        }

        return new BigInteger(1, messageDigest.digest());
    }

    private void writeHash(BigInteger hash, String path) {
        try {
            writer.write(String.format("%064x %s%n", hash, path));
        } catch (IOException e) {
            System.err.println("Error in writing to outputFile" + e.getMessage());
        }
    }
}
