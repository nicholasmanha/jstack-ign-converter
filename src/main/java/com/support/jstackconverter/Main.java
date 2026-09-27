package com.support.jstackconverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, String> waitingFor = new LinkedHashMap<>();

        waitingFor.put("Thread-1", "Monitor-123");
        waitingFor.put("Thread-2", "Monitor-456");

        ArrayList<String> stacktrace = new ArrayList<>();
        stacktrace.add("test");

        Map<String, String> lockedMonitors = new LinkedHashMap<>();
        lockedMonitors.put("Thread-1", "Monitor-123");
        lockedMonitors.put("Thread-2", "Monitor-456");
        Thread thread = new Thread.Builder()
                .name("main")
                .id(123)
                .state(ThreadState.RUNNABLE)
                .daemon(false)
                .system("system")
                .scope("scope")
                .cpuUsage(2.5f)
                .waitingFor(waitingFor)
                .stacktrace(stacktrace)
                .lockedMonitors(lockedMonitors)
                .build();
        System.out.println(thread.toString());
    }
}
