package io.github.tsypuk.writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JekyllFileWriter implements ResultsWriter {
    Path path;

    public JekyllFileWriter() {
        this.path = Paths.get("/tmp/jekyll.plantuml");
    }

    @Override
    public void startSection(String metadata) {

    }

    @Override
    public void endSection() {

    }

    @Override
    public void writeOutput(String text) {
        try {
            Files.write(path, text.getBytes(), StandardOpenOption.APPEND);
            System.out.println("Successfully written bytes to the file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
