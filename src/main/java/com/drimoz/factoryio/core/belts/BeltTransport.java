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
 * <p>Un compteur de sous-ticks, et un pas tous les {@code ticksPerSlot}. C'est ce qui donne sa
 * vitesse au convoyeur, et c'est aussi ce que le rendu interpole : sans lui, les items
 * sauteraient de case en case.
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

    private final BeltLane<T>[] lanes;
    private final int ticksPerSlot;

    /** Sous-ticks écoulés depuis le dernier pas, de 0 à {@code ticksPerSlot}. */
    private int subTick;

    @SuppressWarnings("unchecked")
    public BeltTransport(int ticksPerSlot) {
        this(ticksPerSlot, BeltLane.DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public BeltTransport(int ticksPerSlot, int slotsPerLane) {
        if (ticksPerSlot < 1) {
            throw new IllegalArgumentException("Un pas dure au moins un tick : " + ticksPerSlot);
        }

        this.ticksPerSlot = ticksPerSlot;
        this.lanes = new BeltLane[LANES];

        for (int lane = 0; lane < LANES; lane++) {
            this.lanes[lane] = new BeltLane<>(slotsPerLane);
        }
    }

    // Interface (Lecture)

    public int ticksPerSlot() {
        return this.ticksPerSlot;
    }

    public int subTick() {
        return this.subTick;
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
     * <p>Le pas n'a lieu qu'une fois tous les {@code ticksPerSlot} ; entre-temps seul le
     * sous-tick progresse, ce qui suffit au rendu.
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
        this.subTick++;

        if (this.subTick < this.ticksPerSlot) return false;

        this.subTick = 0;

        boolean moved = false;

        // Chaque voie interroge l'aval pour son propre compte : une voie bouchée ne doit pas
        // arrêter l'autre.
        for (int lane = 0; lane < LANES; lane++) {
            int index = lane;

            moved |= this.lanes[lane].advance(item -> downstream.accept(index, item), stamp);
        }

        return moved;
    }

    /**
     * Un convoyeur vide n'a rien à faire.
     *
     * <p>Le sous-tick est remis à zéro : un convoyeur qui se rendort puis reçoit un item ne
     * doit pas le faire avancer d'un demi-pas à l'instant de son arrivée.
     */
    public boolean canSleep() {
        if (!isEmpty()) return false;

        this.subTick = 0;

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

        this.subTick = 0;
    }

    // Interface (Rendu)

    /**
     * Position d'un item le long du bloc, de 0 à 1.
     *
     * @param exitOpen {@code true} si l'aval accepterait la tête de cette voie — un item
     *                 bloqué ne doit pas glisser, faute de quoi toute une file compressée
     *                 tremblerait
     * @param partialTick fraction de tick écoulée, pour un rendu fluide entre deux ticks
     */
    public float progress(int lane, int slot, float partialTick, boolean exitOpen) {
        return this.lanes[lane].progressOf(slot, this.subTick + partialTick, this.ticksPerSlot, exitOpen);
    }

    // Interface (Persistance)

    /**
     * Restaure le sous-tick lu en NBT.
     *
     * <p>Borné : une sauvegarde écrite avec un autre {@code ticksPerSlot} — un datapack qui a
     * changé la vitesse entre deux sessions — donnerait sinon une progression au-delà de 1, et
     * un item qui déborde de son bloc au premier rendu.
     */
    public void restoreSubTick(int subTick) {
        this.subTick = Math.max(0, Math.min(subTick, this.ticksPerSlot - 1));
    }
}
