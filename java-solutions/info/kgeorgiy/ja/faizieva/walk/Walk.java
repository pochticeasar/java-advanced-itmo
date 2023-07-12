package info.kgeorgiy.ja.faizieva.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import static java.nio.file.Paths.get;


public class Walk {
    private static final int bufSize = 2048;
    private static final String repeat = "0".repeat(64);

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String calculateHash(Path filename) {
        File file = filename.toFile();
        byte[] bufAnswer;
        byte[] buf;
        // :NOTE: exceptions
        try (FileInputStream fileInput = new FileInputStream(file)) {
            try (BufferedInputStream fileReader = new BufferedInputStream(fileInput)) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                while (true) {
                    // :NOTE: allocate
                    buf = new byte[bufSize];
                    int actualSize = fileReader.read(buf, 0, bufSize);
                    if (actualSize > 0) {
                        // :NOTE: if
                        messageDigest.update(buf, 0, actualSize);
                    } else {
                        bufAnswer = messageDigest.digest();
                        return bytesToHex(bufAnswer);
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                System.err.println("No alg found" + e.getMessage());
                return repeat;
            }

        } catch (FileNotFoundException e) {
            System.err.println("File not found" + e.getMessage());
            // :NOTE: constant
            return repeat;

        } catch (IOException e) {
            System.err.println("Error while reading" + e.getMessage());
            return repeat;
        }
    }

    private static void inputOutput(String in, String out) {
        try (BufferedReader inputFile = Files.newBufferedReader(Paths.get(in), StandardCharsets.UTF_8)) {
            try {
                Path parent = Paths.get(out).getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            } catch (InvalidPathException e) {
                System.err.println("Error while reading" + e.getMessage());
            }

            try (BufferedWriter outputFile = Files.newBufferedWriter(Paths.get(out), StandardCharsets.UTF_8)) {
                String filename;
                while ((filename = inputFile.readLine()) != null) {
                    Path path;
                    try {
                        path = get(filename);
                        outputFile.write(String.format("%s %s", calculateHash(path), path));
                        outputFile.newLine();
                    } catch (InvalidPathException e) {
                        outputFile.write(String.format("%064x %s", 0, filename));
                        outputFile.newLine();
                    } catch (FileNotFoundException e) {
                        System.err.println("No such file" + e.getMessage());
                    }
                }
            } catch (IOException e) {
                // :NOTE: messages
                System.err.println("Error while reading or writing" + e.getMessage());
            }
        } catch (InvalidPathException | IOException e) {
            System.err.println("Error while reading" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Incorrect input");
        } else {
            inputOutput(args[0], args[1]);
        }
    }
}
