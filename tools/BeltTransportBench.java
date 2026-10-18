import com.drimoz.factoryio.core.belts.BeltLane;
import com.drimoz.factoryio.core.belts.BeltTransport;

/**
 * Microbenchmark du transport seul (10-BENCHMARKS, FIO-189) : 2 000 convoyeurs chaînés,
 * ticks datés comme en jeu, 20 000 ticks de chauffe puis 20 000 mesurés.
 *
 * <p>Arguments : la cadence ({@code BeltTransport.FULL_SPEED} × items/s ÷ 40) et le régime,
 * {@code moving}, {@code compressed} ou {@code empty}.
 */
public class BeltTransportBench {
    static final int N = 2000, WARMUP = 20_000, TICKS = 20_000;
    static BeltTransport<String>[] belts;
    static long stamp = 12_345_678L;
    static long delivered;
    static boolean deadEnd;

    @SuppressWarnings("unchecked")
    public static void main(String[] a) {
        int arg = Integer.parseInt(a[0]);
        String regime = a[1];
        belts = new BeltTransport[N];
        for (int i = 0; i < N; i++) belts[i] = new BeltTransport<>(arg);
        deadEnd = regime.equals("compressed");
        if (!regime.equals("empty")) {
            for (BeltTransport<String> b : belts)
                for (int l = 0; l < 2; l++)
                    for (int s = 0; s < BeltLane.DEFAULT_CAPACITY; s++) b.offerAt(l, s, "x");
        }
        run(WARMUP, regime);
        delivered = 0;
        long t0 = System.nanoTime();
        run(TICKS, regime);
        long t1 = System.nanoTime();
        System.out.printf("%s %.4f ms/tick, %.3f items/s%n", regime, (t1 - t0) / 1e6 / TICKS, delivered * 20.0 / TICKS);
    }

    static void run(int ticks, String regime) {
        boolean feed = regime.equals("moving");
        for (int t = 0; t < ticks; t++) {
            stamp++;
            final long now = stamp;
            // De l'aval vers l'amont : un ordre fixe, le même pour les deux versions.
            for (int i = N - 1; i >= 0; i--) {
                final int next = i + 1;
                belts[i].tick((lane, item) -> {
                    if (next >= N) { if (deadEnd) return false; delivered++; return true; }
                    return belts[next].lane(lane).offer(item, now);
                }, now);
            }
            if (feed) for (int l = 0; l < 2; l++) belts[0].lane(l).offer("x", now);
        }
    }
}
