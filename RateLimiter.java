import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; 

public class RateLimiter {
    private final int limit;
    private final int windowSeconds;
    private final Map<String, List<Integer>> logs = new HashMap<>();

    public RateLimiter(int limit, int windowSeconds) {
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    public synchronized boolean allow(String key, int timestamp) {
        List<Integer> log = logs.computeIfAbsent(key, k -> new ArrayList<>());

        int cutoff = timestamp - windowSeconds;
        while (log.isEmpty() == false && log.get(0) < cutoff) log.remove(0);

        if (log.size() < limit){
            log.add(timestamp);
            return true;
        }
        else return false;
    }

    /* simple, no multithreading version of the algorithm:
    private final ArrayList<Integer> log = new ArrayList<>(); 

    boolean allow(int timestamp) {
        int cutoff = timestamp - windowSeconds;
        while(log.isEmpty() == false && log.get(0) < cutoff) log.remove(0);

        if(log.size() < limit){
            log.add(timestamp);
            return true;
        } 
        else return false;
    }
    */
}