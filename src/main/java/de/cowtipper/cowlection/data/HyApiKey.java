package de.cowtipper.cowlection.data;

@SuppressWarnings("unused")
public class HyApiKey {
    private boolean success;
    private String cause;

    private Record record;

    /**
     * No-args constructor for GSON
     */
    private HyApiKey() {
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCause() {
        return cause;
    }

    public Record getRecord() {
        return record;
    }

    public class Record {
        private int queriesInPastMin;
        private int limit;
        private long totalQueries;

        public int getQueriesInPastMin() {
            return queriesInPastMin;
        }

        public int getLimit() {
            return limit;
        }

        public long getTotalQueries() {
            return totalQueries;
        }
    }
}
