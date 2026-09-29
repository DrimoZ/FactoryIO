package com.drimoz.factoryio.client.screen;

/**
 * Débit réellement mesuré, pendant que l'écran est ouvert (FIO-170).
 *
 * <p>Le serveur ne tient qu'un compteur d'items livrés ; la moyenne se fait ici, sur les dix
 * dernières secondes. Rien de périodique ne part du serveur pour autant : le compteur voyage par
 * le {@code ContainerData} du menu, qui n'envoie une valeur que quand elle change, et seulement
 * au joueur qui regarde.
 *
 * <p>Le compteur arrive tronqué à 16 bits. Les différences restent justes modulo 65 536, ce qui
 * suffit largement : aucun inserter n'en livre autant en dix secondes.
 */
public final class ThroughputMeter {

    /** Un échantillon par seconde. */
    static final int PERIOD_TICKS = 20;

    /** Dix secondes de fenêtre, soit onze bornes. */
    static final int SAMPLES = 11;

    private final long[] times = new long[SAMPLES];
    private final int[] counts = new int[SAMPLES];
    private int size;
    private int newest = -1;

    /** À appeler à chaque tick ; ne garde qu'un échantillon par seconde. */
    public void sample(long gameTime, int delivered) {
        if (this.size > 0 && gameTime - this.times[this.newest] < PERIOD_TICKS) return;

        this.newest = (this.newest + 1) % SAMPLES;
        this.times[this.newest] = gameTime;
        this.counts[this.newest] = delivered & 0xFFFF;
        if (this.size < SAMPLES) this.size++;
    }

    /** @return {@code true} tant qu'il n'y a pas encore une seconde de mesure */
    public boolean isMeasuring() {
        return this.size < 2;
    }

    /** @return items par seconde sur la fenêtre disponible, 0 tant que la mesure n'a pas commencé */
    public double itemsPerSecond() {
        if (isMeasuring()) return 0;

        int oldest = (this.newest - this.size + 1 + SAMPLES) % SAMPLES;

        long ticks = this.times[this.newest] - this.times[oldest];
        if (ticks <= 0) return 0;

        int items = (this.counts[this.newest] - this.counts[oldest]) & 0xFFFF;
        return items * 20.0 / ticks;
    }
}
