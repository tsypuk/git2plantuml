package io.github.tsypuk.writer;

import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JekyllFileWriter implements ResultsWriter {
    Path path;

    @SneakyThrows
    public JekyllFileWriter() {
        path = Paths.get("/tmp/jekyll.plantuml");
        Files.deleteIfExists(path);
        Files.createFile(path);
    }

    @Override
    public void startSection(String metadata) {
        writeOutput("++++");
        writeOutput("<center>");
        writeOutput("++++");
        writeOutput(metadata);
        writeOutput("....");
    }

    @Override
    public void endSection() {
        writeOutput("....");
        writeOutput("++++");
        writeOutput("</center>");
        writeOutput("++++");
    }

    @Override
    public void writeOutput(String text) {
        try {
            Files.write(path, text.getBytes(), StandardOpenOption.APPEND);
            Files.write(path, "\n".getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
