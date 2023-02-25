package io.github.tsypuk.writer;

public interface ResultsWriter {

    void startSection(String metadata);

    void endSection();

    void writeOutput(String text);
}
