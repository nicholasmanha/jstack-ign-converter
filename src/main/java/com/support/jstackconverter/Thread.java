package com.support.jstackconverter;

import java.util.ArrayList;
import java.util.Map;

public class Thread {
    private final String name;
    private final int id;
    private final ThreadState state;
    private final boolean daemon;
    private final String system;
    private final String scope;
    private final float cpuUsage;
    private final Map<String, String> waitingFor;
    private final ArrayList<String> stacktrace;
    private final Map<String, String> lockedMonitors;

    private Thread(Builder builder) {
        this.name = builder.name;
        this.id = builder.id;
        this.state = builder.state;
        this.daemon = builder.daemon;
        this.system = builder.system;
        this.scope = builder.scope;
        this.cpuUsage = builder.cpuUsage;
        this.waitingFor = builder.waitingFor;
        this.stacktrace = builder.stacktrace;
        this.lockedMonitors = builder.lockedMonitors;
    }

    @Override
    public String toString() {
        return "Thread{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", state=" + state +
                ", daemon=" + daemon +
                ", system='" + system + '\'' +
                ", scope='" + scope + '\'' +
                ", cpuUsage=" + cpuUsage +
                ", waitingFor=" + waitingFor +
                ", stacktrace=" + stacktrace +
                ", lockedMonitors=" + lockedMonitors +
                '}';
    }

    public static class Builder {
        private String name;
        private int id;
        private ThreadState state;
        private boolean daemon;
        private String system;
        private String scope;
        private float cpuUsage;
        private Map<String, String> waitingFor;
        private ArrayList<String> stacktrace;
        private Map<String, String> lockedMonitors;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder state(ThreadState state) {
            this.state = state;
            return this;
        }

        public Builder daemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder cpuUsage(float cpuUsage) {
            this.cpuUsage = cpuUsage;
            return this;
        }

        public Builder waitingFor(Map<String, String> waitingFor) {
            this.waitingFor = waitingFor;
            return this;
        }

        public Builder stacktrace(ArrayList<String> stacktrace) {
            this.stacktrace = stacktrace;
            return this;
        }

        public Builder lockedMonitors(Map<String, String> lockedMonitors) {
            this.lockedMonitors = lockedMonitors;
            return this;
        }

        public Thread build() {
            return new Thread(this);
        }
    }
}