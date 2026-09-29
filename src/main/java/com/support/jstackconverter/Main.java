package com.support.jstackconverter;

import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {

        JstackParser parser = new JstackParser();
        JstackDump threads = parser.parseFile("C:/Users/nickr/Documents/jstack.txt");
        System.out.print(threads);
        JsonFormatter formatter = new JsonFormatter();
        formatter.write(threads, "C:/Users/nickr/Documents/output.json");

//        JStackParser parser = new JStackParser();
//        JStack jstack = parser.parse(inputFile);
//
//        JsonFormatter formatter = new JsonFormatter();
//        formatter.write(jstack, outputFile);
    }
}
