import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestingRateLimiter {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        firstCallOnNewKeyIsAllowed();
        allowsExactlyLimitAndNoMore();
        keysHaveIndependentBudgets();
        entriesExpireAfterWindowSlides();
        rejectedRequestsDoNotOccupySlots();
        concurrentCallsNeverExceedLimit();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ---- Point 1: basic contract ----

    static void firstCallOnNewKeyIsAllowed() {
        RateLimiter rl = new RateLimiter(1, 60);
        check("first call on new key is allowed", rl.allow("pod-a", 0));
    }

    static void allowsExactlyLimitAndNoMore() {
        RateLimiter rl = new RateLimiter(2, 60);
        check("1st allowed",  rl.allow("k", 0));
        check("2nd allowed",  rl.allow("k", 10));
        check("3rd rejected", !rl.allow("k", 20));   // catches <= vs <
    }

    // ---- Point 2: per-key isolation ----

    static void keysHaveIndependentBudgets() {
        RateLimiter rl = new RateLimiter(1, 60);
        check("pod-a first allowed",   rl.allow("pod-a", 0));
        check("pod-a second rejected", !rl.allow("pod-a", 0));
        check("pod-b unaffected",      rl.allow("pod-b", 0));
        check("pod-b second rejected", !rl.allow("pod-b", 0));
    }

    // ---- Point 5: sliding window, no sleeping ----

    static void entriesExpireAfterWindowSlides() {
        RateLimiter rl = new RateLimiter(2, 60);
        check("t=0  allowed",  rl.allow("k", 0));
        check("t=10 allowed",  rl.allow("k", 10));
        check("t=20 rejected", !rl.allow("k", 20));
        // At t=61, cutoff=1, entry at 0 is evicted, entry at 10 survives.
        check("t=61 allowed after window slides", rl.allow("k", 61));
    }

    static void rejectedRequestsDoNotOccupySlots() {
        RateLimiter rl = new RateLimiter(2, 60);
        check("t=0  allowed",  rl.allow("k", 0));
        check("t=10 allowed",  rl.allow("k", 10));
        check("t=20 rejected", !rl.allow("k", 20));
        // If the rejected request had been added to the log, this would fail.
        check("rejected request didn't consume a slot", rl.allow("k", 61));
    }

    // ---- Point 4: thread safety ----

    static void concurrentCallsNeverExceedLimit() throws Exception {
        RateLimiter rl = new RateLimiter(50, 60);
        int threads = 20, perThread = 10;   // 200 attempts, budget 50
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger accepted = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();           // all threads start together
                    for (int i = 0; i < perThread; i++) {
                        if (rl.allow("hot", 0)) accepted.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        check("exactly 50 accepted under contention (was " + accepted.get() + ")",
              accepted.get() == 50);
    }
}