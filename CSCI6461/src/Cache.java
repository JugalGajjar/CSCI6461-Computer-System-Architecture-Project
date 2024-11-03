import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

class CacheLine {
    private int tag;
    private int[] data;
    private boolean valid;
    private boolean dirty;

    public CacheLine(int lineSize) {
        this.data = new int[lineSize];
        this.valid = false;
        this.dirty = false;
    }

    // Getters and setters
    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
        this.valid = true;
    }

    public int[] getData() {
        return data;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void clear() {
        valid = false;
        dirty = false;
        tag = 0;
        data = new int[data.length];
    }

    public String toString() {
        if (valid) {
            return "Tag: " + tag + ", Data: " + java.util.Arrays.toString(data);
        }
        return "Invalid Cache Line";
    }
}

class Cache {
    private CacheLine[] cacheLines;
    private int lineSize;
    private Queue<Integer> fifoQueue;
    private int cacheSize;
    private BufferedWriter traceWriter; // For tracing

    public Cache(int cacheSize, int lineSize, String traceFile) {
        this.cacheSize = cacheSize;
        this.lineSize = lineSize;
        this.cacheLines = new CacheLine[cacheSize];
        this.fifoQueue = new LinkedList<>();

        // Initialize cache lines
        for (int i = 0; i < cacheSize; i++) {
            cacheLines[i] = new CacheLine(lineSize);
        }

        // Initialize trace writer
        try {
            traceWriter = new BufferedWriter(new FileWriter(traceFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to access data in the cache
    public int[] accessCache(int address) {
        int index = address % cacheSize;
        CacheLine line = cacheLines[index];

        // Check for a cache hit
        if (line.isValid() && line.getTag() == (address / lineSize)) {
            logTrace("Cache Hit! Address: " + address);
            return line.getData();
        } else {
            logTrace("Cache Miss! Address: " + address);
            return fetchFromMemory(address, line);
        }
    }

    private int[] fetchFromMemory(int address, CacheLine line) {
        // If FIFO queue is full, evict the oldest cache line
        if (fifoQueue.size() >= cacheSize) {
            evictCacheLine();
        }

        line.setTag(address / lineSize);
        line.setDirty(false);
        fifoQueue.offer(line.getTag());

        int[] dataFromMemory = new int[lineSize]; // Dummy data
        for (int i = 0; i < lineSize; i++) {
            dataFromMemory[i] = address + i;
        }
        System.arraycopy(dataFromMemory, 0, line.getData(), 0, lineSize);
        return line.getData();
    }

    private void evictCacheLine() {
        int oldestTag = fifoQueue.poll();
        for (CacheLine line : cacheLines) {
            if (line.isValid() && line.getTag() == oldestTag) {
                line.clear();
                logTrace("Evicted Cache Line with Tag: " + oldestTag);
                break;
            }
        }
    }

    public void addItemToCache(int address, int[] data) {
        int index = address % cacheSize;
        CacheLine line = cacheLines[index];

        if (line.isValid()) {
            evictCacheLine();
        }

        line.setTag(address / lineSize);
        line.setDirty(true);
        System.arraycopy(data, 0, line.getData(), 0, Math.min(data.length, lineSize));
        fifoQueue.offer(line.getTag());
        logTrace("Added Item to Cache at Address: " + address);
    }

    public void removeItemFromCache(int address) {
        int index = address % cacheSize;
        CacheLine line = cacheLines[index];

        if (line.isValid() && line.getTag() == (address / lineSize)) {
            line.clear();
            fifoQueue.remove(line.getTag());
            logTrace("Removed item from cache: Address " + address);
        } else {
            logTrace("Address " + address + " not found in cache.");
        }
    }

    public void clearCache() {
        for (CacheLine line : cacheLines) {
            line.clear();
        }
        fifoQueue.clear();
        logTrace("Cache cleared.");
    }

    // Method to print cache contents
    public void printCacheContents() {
        System.out.println("Cache Contents:");
        for (int i = 0; i < cacheLines.length; i++) {
            System.out.println("Line " + i + ": " + cacheLines[i]);
        }
    }

    // Method to log trace information
    private void logTrace(String message) {
        try {
            traceWriter.write(message);
            traceWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to close the trace writer
    public void closeTrace() {
        try {
            if (traceWriter != null) {
                traceWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}