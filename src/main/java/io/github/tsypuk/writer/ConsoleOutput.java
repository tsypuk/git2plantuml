package io.github.tsypuk.writer;

import java.util.UUID;

public class ConsoleOutput implements ResultsWriter {
    @Override
    public void startSection(String metadata) {
        System.out.println("++++");
        System.out.println("<center>");
        System.out.println("++++");
        System.out.println(metadata);
        System.out.println("....");
    }

    @Override
    public void endSection() {
        System.out.println("....");
        System.out.println("++++");
        System.out.println("</center>");
        System.out.println("++++");
    }

    @Override
    public void writeOutput(String text) {
        System.out.println(text);
    }
}
