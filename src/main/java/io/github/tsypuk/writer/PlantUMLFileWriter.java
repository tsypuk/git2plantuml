package io.github.tsypuk.writer;

import io.github.tsypuk.config.GitConfig;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class PlantUMLFileWriter implements ResultsWriter {
    Path path;

    @SneakyThrows
    public PlantUMLFileWriter(GitConfig config) {
        path = Paths.get(config.getResultFile());
        Files.deleteIfExists(path);
        Files.createFile(path);
    }

    @Override
    public void startSection(String metadata) {
        writeOutput("@startuml");
    }

    @Override
    public void endSection() {
        writeOutput("@enduml");
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
