package com.drimoz.factoryio.core.belts;

/**
 * Le contenu transporté par un bloc de convoyeur : deux voies et leur horloge.
 *
 * <h2>Le rôle de cette classe</h2>
 *
 * <p>C'est la couture que [`08`](../../../../../../../docs/08-DESIGN-BELTS.md) §2 réclame :
 * tout le transport passe par ici, et rien d'autre ne connaît la structure interne. Le design
 * B — lignes continues à la Factorio — se substituerait derrière la même surface, sans
 * toucher au bloc, au rendu ni à l'inserter.
 *
 * <p>Elle reste <b>pure</b> : ni bloc, ni monde, ni {@code ItemStack}. C'est ce qui permet de
 * vérifier en JUnit les deux choses qui font un convoyeur — l'avancement d'un cran par pas et
 * la compression — plutôt que de les constater en jeu.
 *
 * <h2>L'horloge</h2>
 *
 * <p>Une cadence en fraction de pas par tick ({@link #FULL_SPEED} = une case par tick), et une
 * phase qui l'accumule. C'est ce qui donne sa vitesse au convoyeur, et c'est aussi ce que le
 * rendu interpole : sans elle, les items sauteraient de case en case.
 *
 * <p>La cadence est <b>fractionnaire</b> (FIO-189) : 15 items/s font un pas tous les 2,67 ticks,
 * en alternant des intervalles de 2 et 3. Le plafond reste une case par tick : un item ne
 * franchit jamais deux cases dans le même tick, c'est ce qui garde le mouvement indépendant de
 * l'ordre de tick ({@link BeltLane#advance}).
 *
 * <h2>Les deux voies sont indépendantes</h2>
 *
 * <p>Chacune avance pour son compte et interroge l'aval séparément. Une voie bouchée ne bloque
 * donc pas l'autre — comportement de Factorio, et la seule règle qui rende les séparateurs
 * intelligibles plus tard.
 */
public final class BeltTransport<T> {

    /** Deux voies : 0 à gauche, 1 à droite, vues depuis la sortie. */
    public static final int LANES = 2;

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    /**
     * Cadence d'une case par tick, la plus haute.
     *
     * <p>720 720 est le plus petit multiple commun de 1 à 16 : une cadence d'un pas tous les
     * {@code n} ticks, pour tout {@code n} jusqu'à 16, y est exacte, sans dérive.
     */
    public static final int FULL_SPEED = 720_720;

    private final BeltLane<T>[] lanes;

    /**
     * Fraction de case par tick, sur {@link #FULL_SPEED}. Non final : un changement de
     * vitesse doit s'appliquer aux blocs déjà posés.
     */
    private int rate;

    /** Cadence accumulée depuis le dernier pas, de 0 à {@link #FULL_SPEED} exclu. */
    private long phase;

    /** Instant du dernier tick, pour dater les glissements que le rendu interpole. */
    private long clock;

    public BeltTransport(int rate) {
        this(rate, BeltLane.DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public BeltTransport(int rate, int slotsPerLane) {
        this.rate = checkRate(rate);
        this.lanes = new BeltLane[LANES];

        for (int lane = 0; lane < LANES; lane++) {
            this.lanes[lane] = new BeltLane<>(slotsPerLane);
        }
    }

    /** La cadence d'un pas tous les {@code ticksPerSlot} ticks. */
    public static int everyTicks(int ticksPerSlot) {
        if (ticksPerSlot < 1) throw new IllegalArgumentException("Un pas dure au moins un tick : " + ticksPerSlot);

        return Math.max(1, Math.round((float) FULL_SPEED / ticksPerSlot));
    }

    private static int checkRate(int rate) {
        if (rate < 1 || rate > FULL_SPEED) {
            throw new IllegalArgumentException("Cadence hors de ]0, une case par tick] : " + rate);
        }

        return rate;
    }

    // Interface (Lecture)

    public int rate() {
        return this.rate;
    }

    /**
     * Change la cadence d'un convoyeur déjà en service.
     *
     * <p>La phase reste sous {@link #FULL_SPEED} quelle que soit la cadence : aucun pas n'est
     * dû d'avance, et le rendu ne peut pas faire déborder un item de son bloc.
     */
    public void setRate(int rate) {
        this.rate = checkRate(rate);
    }

    /** Ticks entiers écoulés depuis le dernier pas. */
    public int subTick() {
        return (int) (this.phase / this.rate);
    }

    public BeltLane<T> lane(int lane) {
        return this.lanes[lane];
    }

    public boolean isEmpty() {
        for (BeltLane<T> lane : this.lanes) {
            if (!lane.isEmpty()) return false;
        }

        return true;
    }

    public int count() {
        int count = 0;

        for (BeltLane<T> lane : this.lanes) {
            count += lane.count();
        }

        return count;
    }

    // Interface (Tick)

    /**
     * Fait avancer le convoyeur d'un tick.
     *
     * <p>Le pas n'a lieu que lorsque la phase atteint une case ; entre-temps seule la phase
     * progresse, ce qui suffit au rendu.
     *
     * @return {@code true} si quelque chose a bougé, de quoi décider d'une mise en sommeil
     */
    public boolean tick(BeltSink<T> downstream) {
        return tick(downstream, BeltLane.NO_STAMP);
    }

    /**
     * Fait avancer le convoyeur d'un tick, en datant le pas.
     *
     * @param stamp pas courant — le temps du monde. Il empêche un item qui vient d'entrer
     *              d'avancer une seconde fois dans le même tick, ce qui, le long d'une ligne,
     *              lui ferait traverser plusieurs blocs d'un coup. Voir {@link BeltLane#advance}
     */
    public boolean tick(BeltSink<T> downstream, long stamp) {
        // Daté, le pas suit le temps du monde : tous les convoyeurs d'une même vitesse le font
        // au même tick, côté serveur comme côté client. Chacun gardait auparavant sa propre
        // phase, née de l'arrivée de son premier item ; deux blocs voisins avançaient alors à
        // des instants différents, et un item passait deux ticks sur l'un, quatre sur l'autre —
        // une saccade à chaque frontière.
        //
        // Un pas a lieu au tick où le compte des cases parcourues depuis l'origine du monde
        // change d'entier : une fonction du temps seul, donc la même pour tous les convoyeurs
        // de même cadence, et pour le client comme le serveur.
        if (stamp == BeltLane.NO_STAMP) {
            this.clock++;
            this.phase += this.rate;
            if (this.phase < FULL_SPEED) return false;
            this.phase -= FULL_SPEED;
        } else {
            this.clock = stamp;
            long before = Math.floorDiv(stamp * this.rate, FULL_SPEED);
            long reached = (stamp + 1) * this.rate;
            this.phase = Math.floorMod(reached, (long) FULL_SPEED);
            if (Math.floorDiv(reached, FULL_SPEED) == before) return false;
        }

        boolean moved = false;
        long now = this.clock;

        // Chaque voie interroge l'aval pour son propre compte : une voie bouchée ne doit pas
        // arrêter l'autre.
        for (int lane = 0; lane < LANES; lane++) {
            int index = lane;

            moved |= this.lanes[lane].advance(item -> downstream.accept(index, item), stamp, now);
        }

        return moved;
    }

    /** Instant du dernier tick — le temps du monde, ou un compteur hors du monde. */
    public long clock() {
        return this.clock;
    }

    /**
     * Un convoyeur vide n'a rien à faire.
     *
     * <p>Le sous-tick est remis à zéro : un convoyeur qui se rendort puis reçoit un item ne
     * doit pas le faire avancer d'un demi-pas à l'instant de son arrivée.
     */
    public boolean canSleep() {
        if (!isEmpty()) return false;

        this.phase = 0;

        return true;
    }

    // Interface (Dépôt)

    /** Dépose sur la case d'entrée d'une voie. */
    public boolean offer(int lane, T item) {
        return this.lanes[lane].offer(item);
    }

    /** Dépose sur la case d'entrée en datant l'arrivée — voir {@link BeltLane#advance}. */
    public boolean offer(int lane, T item, long stamp) {
        return this.lanes[lane].offer(item, stamp);
    }

    /**
     * Dépose sur une case précise.
     *
     * <p>C'est ce dont un inserter a besoin : il ne dépose pas en bout de bande mais à
     * l'endroit où sa pince plonge.
     */
    public boolean offerAt(int lane, int slot, T item) {
        return this.lanes[lane].offerAt(slot, item);
    }

    /** Dépose sur une case précise en datant l'arrivée — voir {@link BeltLane#advance}. */
    public boolean offerAt(int lane, int slot, T item, long stamp) {
        return this.lanes[lane].offerAt(slot, item, stamp);
    }

    /**
     * Vide les deux voies et remet l'horloge à zéro.
     *
     * <p>Nécessaire à la synchronisation : un paquet décrit l'état complet du convoyeur, et
     * doit <b>écraser</b> celui du client. Sans cela, une case déjà occupée refuserait le
     * dépôt et le client conserverait indéfiniment un item que le serveur n'a plus.
     */
    public void clear() {
        for (BeltLane<T> lane : this.lanes) {
            lane.clear();
        }

        this.phase = 0;
    }

    // Interface (Rendu)

    /**
     * Position d'un item le long du bloc, de 0 à 1 — un pas de retard sur la simulation, voir
     * {@link BeltLane#progressOf}.
     *
     * @param partialTick fraction de tick écoulée, pour un rendu fluide entre deux ticks
     */
    public float progress(int lane, int slot, float partialTick) {
        return this.lanes[lane].progressOf(slot, this.clock, partialTick, this.rate);
    }

    /** Position de l'item du tampon d'une voie, légèrement en deçà de l'entrée. */
    public float stagedProgress(int lane, float partialTick) {
        return this.lanes[lane].stagedProgress(this.clock, partialTick, this.rate);
    }

    // Interface (Persistance)

    /**
     * Restaure le sous-tick lu en NBT.
     *
     * <p>Borné : une sauvegarde écrite avec une autre cadence — un datapack qui a changé la
     * vitesse entre deux sessions — donnerait sinon un pas dû d'avance.
     */
    public void restoreSubTick(int subTick) {
        this.phase = Math.max(0L, Math.min((long) subTick * this.rate, FULL_SPEED - 1L));
    }
}
