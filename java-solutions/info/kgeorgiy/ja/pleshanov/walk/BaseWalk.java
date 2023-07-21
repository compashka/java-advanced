package info.kgeorgiy.ja.pleshanov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BaseWalk {
    private final WalkVariant walkVariant;

    public BaseWalk(WalkVariant walkVariant) {
        this.walkVariant = walkVariant;
    }

    public void walk(String[] args) {
        if (!validateArgs(args)) {
            return;
        }

        Path inputFile = getValidPath(args[0], "inputFile");
        Path outputFile = getValidPath(args[1], "outputFile");

        if (inputFile == null || outputFile == null) {
            return;
        }

        try (BufferedReader inputFileReader = Files.newBufferedReader(inputFile)) {
            try (BufferedWriter outputFileWriter = Files.newBufferedWriter(outputFile)) {
                MessageDigest messageDigest;
                try {
                    messageDigest = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("SHA-256 is not available");
                    return;
                }
                HashWriter hashWriter = new HashWriter(outputFileWriter, messageDigest);
                String lineFile;
                while ((lineFile = inputFileReader.readLine()) != null) {
                    try {
                        Files.walkFileTree(Path.of(lineFile), new HashVisitor(hashWriter, walkVariant));
                    } catch (IOException | InvalidPathException e) {
                        System.err.println("Error in traversing file tree " + e.getMessage());
                        hashWriter.writeNullHash(lineFile);
                    }
                }
            } catch (IOException e) {
                System.err.println("Invalid OutputFile: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Invalid InputFile: " + e.getMessage());
        }
    }

    private boolean validateArgs(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong format of input. Example: java " + walkVariant + " <input file> <output file>");
            return false;
        }
        return true;
    }


    private Path getValidPath(String p, String file) {
        try {
            Path path = Path.of(p);
            if (file.equals("outputFile") && path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            return path;
        } catch (InvalidPathException | IOException e) {
            System.err.println(e.getMessage() + file);
            return null;
        }
    }
}
