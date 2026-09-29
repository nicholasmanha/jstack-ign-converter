package com.support.jstackconverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final String USAGE =
            "Usage:\n" +
                    "  jstackconverter <input-file>\n" +
                    "  jstackconverter -r <input-folder> <output-folder>\n";

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && !args[0].equals("-r")) {
            convertSingle(Paths.get(args[0]).toAbsolutePath());
        } else if (args.length == 3 && args[0].equals("-r")) {
            convertFolder(Paths.get(args[1]).toAbsolutePath(), Paths.get(args[2]).toAbsolutePath());
        } else {
            System.err.print(USAGE);
            System.exit(1);
        }
    }

    private static void convertSingle(Path input) throws IOException {
        Path output = input.resolveSibling(withJsonExtension(input.getFileName().toString()));
        convert(input, output);
        System.out.println("Wrote " + output);
    }

    private static void convertFolder(Path inputDir, Path outputDir) throws IOException {
        if (!Files.isDirectory(inputDir)) {
            System.err.println("Input folder does not exist or is not a directory: " + inputDir);
            System.exit(1);
        }
        Files.createDirectories(outputDir);

        List<Path> files;
        try (Stream<Path> stream = Files.list(inputDir)) {
            files = stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }

        int success = 0;
        int failed = 0;
        for (Path file : files) {
            Path output = outputDir.resolve(withJsonExtension(file.getFileName().toString()));
            try {
                convert(file, output);
                System.out.println("Wrote " + output);
                success++;
            } catch (Exception e) {
                System.err.println("Failed to convert " + file + ": " + e.getMessage());
                failed++;
            }
        }

        System.out.printf("Done: %d converted, %d failed%n", success, failed);
        if (failed > 0) {
            System.exit(2);
        }
    }

    private static void convert(Path input, Path output) throws IOException {
        JstackParser parser = new JstackParser();
        JstackDump threads = parser.parseFile(input.toString());
        new JsonFormatter().write(threads, output.toString());
    }

    private static String withJsonExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = (dot > 0) ? fileName.substring(0, dot) : fileName;
        return base + ".json";
    }
}