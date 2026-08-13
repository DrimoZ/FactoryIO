package com.drimoz.factoryio.core.belts;

import java.util.function.Predicate;

/**
 * Une voie de convoyeur : quelques emplacements en file, et la règle qui les fait avancer
 * (design A de [`08`](../../../../../../../docs/08-DESIGN-BELTS.md)).
 *
 * <h2>Ce que cette classe est, et ce qu'elle n'est pas</h2>
 *
 * <p>Elle ne connaît ni {@code ItemStack}, ni bloc, ni monde — d'où le paramètre de type. Tout
 * le transport se décrit avec « une case est occupée ou non », et rien de ce qui suit ne
 * dépend de <i>ce que</i> l'on transporte. C'est ce qui la rend testable en JUnit, là où un
 * convoyeur complet relèverait du GameTest.
 *
 * <p>C'est aussi une couverture : la règle d'avancement et la compression sont exactement les
 * deux choses qu'un convoyeur doit réussir, et les deux qu'une refonte casse en silence.
 *
 * <h2>Le sens de parcours n'est pas un détail</h2>
 *
 * <p>L'avancement se fait <b>de la sortie vers l'entrée</b>. Libérer d'abord la case de tête,
 * puis remonter, permet à toute une file compressée d'avancer d'un cran en une seule passe.
 * Parcourir dans l'autre sens ferait avancer le premier item, puis le suivant dans la case
 * qu'il vient de quitter, et ainsi de suite : toute la file traverserait la voie en un tick.
 *
 * <h2>La compression n'est pas codée</h2>
 *
 * <p>Il n'y a nulle part de « si bloqué, alors compresser ». Quand la tête ne passe pas, elle
 * reste ; la case suivante ne peut donc plus avancer, et ainsi de suite. Le bouchon remonte
 * de lui-même. Une compression écrite à part serait une seconde description du même
 * phénomène, donc une occasion de diverger.
 */
public final class BeltLane<T> {

    /** Emplacements par voie et par bloc. Quatre donne huit items par bloc, comme Factorio. */
    public static final int DEFAULT_CAPACITY = 4;

    private final Object[] slots;

    public BeltLane() {
        this(DEFAULT_CAPACITY);
    }

    public BeltLane(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Voie sans emplacement : " + capacity);

        this.slots = new Object[capacity];
    }

    // Interface (Lecture)

    public int capacity() {
        return this.slots.length;
    }

    /** Index de la case de <b>sortie</b> : celle qui passe au bloc suivant. */
    public int exitSlot() {
        return this.slots.length - 1;
    }

    /** Index de la case d'<b>entrée</b> : celle qu'alimente le bloc précédent. */
    public int entrySlot() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    public T get(int slot) {
        return (T) this.slots[slot];
    }

    public boolean isOccupied(int slot) {
        return this.slots[slot] != null;
    }

    public boolean isEmpty() {
        for (Object slot : this.slots) {
            if (slot != null) return false;
        }

        return true;
    }

    public boolean isFull() {
        for (Object slot : this.slots) {
            if (slot == null) return false;
        }

        return true;
    }

    public int count() {
        int count = 0;

        for (Object slot : this.slots) {
            if (slot != null) count++;
        }

        return count;
    }

    // Interface (Écriture)

    /**
     * Dépose un item sur la case d'entrée.
     *
     * @return {@code false} si elle est déjà occupée — l'appelant garde son item
     */
    public boolean offer(T item) {
        return offerAt(entrySlot(), item);
    }

    /**
     * Dépose un item sur une case précise.
     *
     * <p>Utile à l'inserter, qui ne dépose pas en bout de bande mais à l'endroit où il
     * plonge.
     *
     * @return {@code false} si la case est occupée
     */
    public boolean offerAt(int slot, T item) {
        if (item == null) throw new IllegalArgumentException("Un item nul n'occupe rien");
        if (this.slots[slot] != null) return false;

        this.slots[slot] = item;

        return true;
    }

    /** Retire l'item d'une case, ou {@code null} si elle est vide. */
    @SuppressWarnings("unchecked")
    public T take(int slot) {
        T item = (T) this.slots[slot];
        this.slots[slot] = null;

        return item;
    }

    /**
     * Fait avancer la voie d'un cran.
     *
     * <p>D'abord la sortie, ensuite le décalage — dans cet ordre, sinon la case de tête reste
     * occupée pendant la passe et bloque tout ce qui la suit d'un tick inutile.
     *
     * @param sink reçoit l'item de tête ; rend {@code true} s'il l'a pris. Un {@code sink} qui
     *             refuse toujours décrit un mur, et produit la compression.
     * @return {@code true} si quelque chose a bougé — de quoi décider d'une mise en sommeil
     */
    public boolean advance(Predicate<T> sink) {
        boolean moved = false;

        int exit = exitSlot();

        if (this.slots[exit] != null && sink.test(get(exit))) {
            this.slots[exit] = null;
            moved = true;
        }

        // De l'avant-dernière case vers l'entrée : chaque item avance d'au plus une case par
        // tick, et une file compressée avance d'un bloc en une seule passe.
        for (int slot = exit - 1; slot >= 0; slot--) {
            if (this.slots[slot] == null || this.slots[slot + 1] != null) continue;

            this.slots[slot + 1] = this.slots[slot];
            this.slots[slot] = null;
            moved = true;
        }

        return moved;
    }

    // Interface (Rendu)

    /**
     * Position d'un item le long du bloc, de 0 à 1.
     *
     * <p><b>Un item bloqué ne glisse pas.</b> Sans cette garde, un item arrêté continuerait
     * d'avancer visuellement au fil du sous-tick puis reviendrait en arrière d'un coup au
     * moment du pas — un tremblement sur toute une file compressée, c'est-à-dire sur le cas le
     * plus fréquent d'une usine.
     *
     * <p>Ce qui décide n'est pas l'état de la voie seule : la case de tête ne peut glisser que
     * si l'aval la prendra. D'où {@code exitOpen}, que seul le bloc connaît.
     *
     * @param subTick       sous-tick écoulé, de 0 à {@code ticksPerSlot}
     * @param ticksPerSlot  durée d'un pas, en ticks
     * @param exitOpen      {@code true} si l'aval accepterait l'item de tête
     */
    public float progressOf(int slot, float subTick, int ticksPerSlot, boolean exitOpen) {
        float base = (float) slot / capacity();

        if (!canCreep(slot, exitOpen)) return base;

        float step = Math.min(subTick / ticksPerSlot, 1f) / capacity();

        return base + step;
    }

    /** @return {@code true} si cet item a devant lui la place d'avancer */
    public boolean canCreep(int slot, boolean exitOpen) {
        if (this.slots[slot] == null) return false;

        return slot == exitSlot() ? exitOpen : this.slots[slot + 1] == null;
    }
}
