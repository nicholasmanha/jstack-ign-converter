package com.support.jstackconverter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JstackParser {

    public JstackParser() {
    }

    public Thread parse(String path) throws IOException {
        String line;

        String name = null;
        int id = 0;
        ThreadState state = null;
        boolean daemon = false;
        String system = "test";
        String scope = "test";
        float cpuUsage = 0;

        Map<String, String> waitingFor = new HashMap<>();
        ArrayList<String> stacktrace = new ArrayList<>();
        ArrayList<Map<String, String>> lockedMonitors = new ArrayList<>();

        // Locked monitors that are waiting to be associated
        // with the next stack frame.
        ArrayList<Map<String, String>> pendingLocks = new ArrayList<>();

        // "platform-scheduled-executor-1" #27 ...
        Pattern headerPattern = Pattern.compile(
                "^\"([^\"]+)\"\\s+#(\\d+)"
        );

        // java.lang.Thread.State: WAITING (parking)
        Pattern statePattern = Pattern.compile(
                "^\\s*java\\.lang\\.Thread\\.State:\\s+(\\w+)"
        );

        // - parking to wait for <0x000000008113c7b8> (a java....ConditionObject)
        Pattern waitingPattern = Pattern.compile(
                "^\\s*-\\s+parking to wait for\\s+<([^>]+)>\\s+\\(([^)]+)\\)"
        );

        // - locked <0x00000000817d1ef0> (a sun.nio.ch.Util$2)
        Pattern lockedPattern = Pattern.compile(
                "^\\s*-\\s+locked\\s+<([^>]+)>\\s+\\(([^)]+)\\)"
        );

        // at java.util.concurrent.locks.LockSupport.park(...)
        Pattern stackPattern = Pattern.compile(
                "^\\s*at\\s+(.+)$"
        );

        try (BufferedReader bfro = new BufferedReader(new FileReader(path))) {
            while ((line = bfro.readLine()) != null) {

                // Thread name and ID
                Matcher matcher = headerPattern.matcher(line);

                if (matcher.find()) {
                    name = matcher.group(1);
                    id = Integer.parseInt(matcher.group(2));

                    // Daemon if "daemon" appears after the thread ID
                    daemon = line.matches(
                            "^\"[^\"]+\"\\s+#\\d+\\s+daemon\\s+.*"
                    );
                }

                // Thread state
                matcher = statePattern.matcher(line);

                if (matcher.find()) {
                    state = ThreadState.valueOf(matcher.group(1));
                }

                // Waiting for lock
                matcher = waitingPattern.matcher(line);

                if (matcher.find()) {
                    String address = matcher.group(1);
                    String type = matcher.group(2);

                    address = address.replaceFirst("^0x", "");

                    waitingFor.put(
                            "lock",
                            type + "@" + address
                    );
                }

                // Locked monitor
                matcher = lockedPattern.matcher(line);

                if (matcher.find()) {
                    String address = matcher.group(1);
                    String type = matcher.group(2);

                    address = address.replaceFirst("^0x", "");

                    Map<String, String> monitor = new HashMap<>();
                    monitor.put("lock", type + "@" + address);

                    pendingLocks.add(monitor);

                    continue;
                }

                // Stack trace
                matcher = stackPattern.matcher(line);

                if (matcher.find()) {
                    String frame = matcher.group(1);

                    stacktrace.add(frame);

                    // Locked monitors are associated with the
                    // stack frame immediately following them.
                    for (Map<String, String> monitor : pendingLocks) {
                        monitor.put("frame", frame);
                        lockedMonitors.add(monitor);
                    }

                    pendingLocks.clear();
                }
            }
        }

        if (waitingFor.isEmpty()) {
            waitingFor = null;
        }

        if (lockedMonitors.isEmpty()) {
            lockedMonitors = null;
        }

        return new Thread.Builder()
                .name(name)
                .id(id)
                .state(state)
                .daemon(daemon)
                .system(system)
                .scope(scope)
                .cpuUsage(cpuUsage)
                .waitingFor(waitingFor)
                .stacktrace(stacktrace)
                .lockedMonitors(lockedMonitors)
                .build();
    }
}