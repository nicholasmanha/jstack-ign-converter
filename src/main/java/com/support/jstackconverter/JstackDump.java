package com.support.jstackconverter;

import java.util.ArrayList;

public class JstackDump {
    private final ArrayList<Thread> threads;

    public JstackDump(){
        this.threads = new ArrayList<>();
    }

    public JstackDump(ArrayList<Thread> threads){
        this.threads = threads;
    }

    public void addThread(Thread thread){
        threads.add(thread);
    }

    @Override
    public String toString(){
        StringBuilder output = new StringBuilder();
        for(Thread thread : threads){
            output.append(thread.toString());
        }
        return output.toString();
    }
}
