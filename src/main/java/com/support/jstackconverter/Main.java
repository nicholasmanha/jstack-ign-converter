package com.support.jstackconverter;

import java.io.IOException;
import java.util.ArrayList;


public class Main {
    public static void main(String[] args) throws IOException {

        JstackParser parser = new JstackParser();
        ArrayList<Thread> threads = parser.parseFile("C:/Users/nickr/Documents/jstack.txt");
        for(Thread thread : threads){
            System.out.print(thread.toString());
        }


//        JStackParser parser = new JStackParser();
//        JStack jstack = parser.parse(inputFile);
//
//        JsonFormatter formatter = new JsonFormatter();
//        formatter.write(jstack, outputFile);
    }
}
