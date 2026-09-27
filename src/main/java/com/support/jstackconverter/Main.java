package com.support.jstackconverter;

import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {

        JstackParser parser = new JstackParser();
        Thread thread = parser.parse("/home/jelliedsquash/Downloads/test_unit.txt");
        System.out.print(thread.toString());

//        JStackParser parser = new JStackParser();
//        JStack jstack = parser.parse(inputFile);
//
//        JsonFormatter formatter = new JsonFormatter();
//        formatter.write(jstack, outputFile);
    }
}
