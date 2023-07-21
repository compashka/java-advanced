package info.kgeorgiy.ja.pleshanov.walk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashVisitor extends SimpleFileVisitor<Path> {
    private final HashWriter hasher;

    private final WalkVariant walkVariant;

    public HashVisitor(final HashWriter hashWriter, final WalkVariant walkVariant) {
        this.walkVariant = walkVariant;
        this.hasher = hashWriter;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        hasher.writeHash(file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException e) {
        System.err.println(e.getMessage());
        hasher.writeNullHash(file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
        if (walkVariant == WalkVariant.RecursiveWalk) {
            return FileVisitResult.CONTINUE;
        }

        hasher.writeNullHash(dir.toString());
        return FileVisitResult.TERMINATE;
    }
}
