package com.drimoz.factoryio.core.belts;

import java.util.Arrays;
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

    /**
     * Absence de marque temporelle : l'item n'arrive pas du pas en cours.
     *
     * <p>C'est la valeur à passer quand la question ne se pose pas — une relecture NBT, un test
     * qui n'a qu'une voie. Elle ne bloque jamais rien.
     */
    public static final long NO_STAMP = Long.MIN_VALUE;

    private final Object[] slots;

    /** Pas auquel chaque case a été remplie <b>de l'extérieur</b>. Voir {@link #advance}. */
    private final long[] arrived;

    /**
     * Instant où l'item de chaque case y a <b>glissé</b>, ou {@link #NO_STAMP} s'il y a été posé.
     *
     * <p>Le rendu dessine avec un pas de retard : un item qui vient de glisser va de la case
     * précédente à la sienne pendant le pas qui suit. On ne montre ainsi que ce qui s'est
     * réellement passé — aucune prédiction, donc aucun saut quand elle se tromperait. Voir
     * {@link #progressOf}.
     */
    private final long[] slidAt;

    /** Instant où l'item du tampon a quitté l'amont. */
    private long stagedAt = NO_STAMP;

    /** Cases occupées, tampon exclu. Tenu à jour : voir {@link #isEmpty}. */
    private int occupied;

    /**
     * Case tampon, à cheval sur la frontière avec le bloc amont.
     *
     * <p>Elle tient <b>un</b> item, et n'existe que le temps d'un pas : l'amont y dépose quand
     * la case d'entrée n'est pas encore libre, et {@link #advance} la vide dès qu'elle l'est —
     * c'est-à-dire à la fin du même pas dans la quasi-totalité des cas.
     *
     * <p>C'est elle qui rend le mouvement <b>indépendant de l'ordre de tick</b>, et c'est la
     * seule façon de faire tourner une boucle saturée. Voir {@link #advance}.
     */
    private Object staged;

    public BeltLane() {
        this(DEFAULT_CAPACITY);
    }

    public BeltLane(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Voie sans emplacement : " + capacity);

        this.slots = new Object[capacity];
        this.arrived = new long[capacity];
        this.slidAt = new long[capacity];

        Arrays.fill(this.arrived, NO_STAMP);
        Arrays.fill(this.slidAt, NO_STAMP);
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

    /**
     * Vide, tampon compris.
     *
     * <p>Tenu à jour plutôt que recalculé : c'est la question posée à <b>chaque convoyeur et
     * à chaque tick</b>, y compris — surtout — aux innombrables convoyeurs vides d'une usine
     * réelle, pour lesquels elle est la seule chose qui s'exécute. Un balayage de tableau y
     * coûtait plus que tout le reste.
     */
    public boolean isEmpty() {
        return this.occupied == 0 && this.staged == null;
    }

    /**
     * Toutes les cases occupées.
     *
     * <p>Ne compte pas le tampon : une voie pleine dont le tampon est libre peut encore
     * recevoir, et c'est justement le cas qui fait tourner une boucle.
     */
    public boolean isFull() {
        return this.occupied == this.slots.length;
    }

    public int count() {
        return this.occupied + (this.staged == null ? 0 : 1);
    }

    // Interface (Tampon)

    @SuppressWarnings("unchecked")
    public T staged() {
        return (T) this.staged;
    }

    public boolean isStaged() {
        return this.staged != null;
    }

    /**
     * Dépose dans le tampon.
     *
     * @return {@code false} s'il est déjà pris — l'appelant garde son item
     */
    public boolean stage(T item) {
        return stage(item, NO_STAMP);
    }

    /**
     * Dépose dans le tampon un item qui arrive de l'amont.
     *
     * @param at instant de son départ — il glisse vers l'entrée à partir de là
     */
    public boolean stage(T item, long at) {
        if (item == null) throw new IllegalArgumentException("Un item nul n'occupe rien");
        if (this.staged != null) return false;

        this.staged = item;
        this.stagedAt = at;

        return true;
    }

    // Interface (Écriture)

    /**
     * Dépose un item sur la case d'entrée.
     *
     * @return {@code false} si elle est déjà occupée — l'appelant garde son item
     */
    public boolean offer(T item) {
        return offerAt(entrySlot(), item, NO_STAMP);
    }

    /** Dépose sur la case d'entrée, en marquant le pas d'arrivée (voir {@link #advance}). */
    public boolean offer(T item, long stamp) {
        return offerAt(entrySlot(), item, stamp);
    }

    /**
     * Reçoit l'item de tête du bloc amont, sur la case d'entrée.
     *
     * <p>À la différence d'un dépôt, l'item <b>glisse</b> : il franchit la frontière pendant le
     * pas qui suit, au lieu d'apparaître à l'entrée.
     */
    public boolean receive(T item, long stamp) {
        if (!offerAt(entrySlot(), item, stamp)) return false;

        this.slidAt[entrySlot()] = stamp;

        return true;
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
        return offerAt(slot, item, NO_STAMP);
    }

    /**
     * Dépose sur une case précise, en marquant le pas d'arrivée.
     *
     * @param stamp pas courant, ou {@link #NO_STAMP} — voir {@link #advance}
     */
    public boolean offerAt(int slot, T item, long stamp) {
        if (item == null) throw new IllegalArgumentException("Un item nul n'occupe rien");
        if (this.slots[slot] != null) return false;

        this.slots[slot] = item;
        this.arrived[slot] = stamp;
        this.slidAt[slot] = NO_STAMP;
        this.occupied++;

        return true;
    }

    /** Retire l'item d'une case, ou {@code null} si elle est vide. */
    @SuppressWarnings("unchecked")
    public T take(int slot) {
        T item = (T) this.slots[slot];
        if (item != null) this.occupied--;

        this.slots[slot] = null;
        this.arrived[slot] = NO_STAMP;
        this.slidAt[slot] = NO_STAMP;

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
        return advance(sink, NO_STAMP);
    }

    /**
     * Fait avancer la voie d'un cran, sans faire avancer deux fois ce qui vient d'arriver.
     *
     * <h3>Pourquoi une marque temporelle</h3>
     *
     * <p>Le parcours descendant protège la voie contre elle-même, mais pas contre ses voisines.
     * Deux blocs A → B qui franchissent un pas au <b>même tick</b> ne donnent pas le même
     * résultat selon l'ordre : si A passe en premier, son item entre dans la case d'entrée de
     * B, puis B avance et le fait progresser une seconde fois. Répété le long d'une ligne, un
     * item la traverse entière en un tick — exactement le défaut que le parcours descendant
     * évite à l'intérieur d'une voie.
     *
     * <p>Et l'ordre de tick n'a rien d'un détail théorique : les block entities sont tickées
     * dans leur ordre de création, donc dans l'ordre de pose. Un joueur qui pose sa ligne en
     * marchant le long produit précisément l'ordre défavorable.
     *
     * <p>La marque suffit : une case remplie de l'extérieur pendant le pas courant est passée.
     * Les deux ordres donnent alors la même vitesse.
     *
     * <h3>Et le tampon, qui débloque les boucles</h3>
     *
     * <p>La datation ne suffit pas. Tant qu'un transfert exige que la case d'entrée de l'aval
     * soit libre <b>à l'instant précis</b> où l'amont tique, une <b>boucle fermée saturée</b>
     * est un blocage définitif : chaque bloc attend que le suivant se libère, et le suivant
     * attend le précédent. Aucun ordre de tick n'en sort — le circuit s'arrête pour de bon,
     * alors qu'il devrait tourner indéfiniment.
     *
     * <p>Le tampon casse cette circularité. L'amont y dépose quand l'entrée est encore prise,
     * et cette méthode le vide <b>après</b> le décalage, donc une fois l'entrée libérée. Les
     * deux ordres de tick donnent alors le même résultat, boucles comprises, sans qu'aucune
     * structure de niveau ne soit nécessaire.
     *
     * <p>Vider le tampon <b>après</b> le décalage n'est pas un détail : c'est ce qui garantit
     * qu'un item entré pendant ce pas n'y avance pas une seconde fois.
     *
     * <p>L'amont ne dépose dans le tampon que s'il a établi que l'aval bougera réellement —
     * sans quoi un convoyeur bouché avalerait les items dans un trou. C'est à l'appelant de le
     * vérifier ; ici, on ne fait qu'offrir la case.
     *
     * @param stamp pas courant — le temps du monde suffit ; {@link #NO_STAMP} ne bloque rien
     */
    public boolean advance(Predicate<T> sink, long stamp) {
        return advance(sink, stamp, stamp);
    }

    /**
     * Comme {@link #advance(Predicate, long)}, en datant les glissements pour le rendu.
     *
     * @param now instant du pas, noté sur chaque item qui glisse — distinct de {@code stamp},
     *            qui peut valoir {@link #NO_STAMP} quand l'ordre de tick est hors de propos
     */
    public boolean advance(Predicate<T> sink, long stamp, long now) {
        boolean moved = false;

        int exit = exitSlot();

        if (this.slots[exit] != null && !justArrived(exit, stamp) && sink.test(get(exit))) {
            this.slots[exit] = null;
            this.arrived[exit] = NO_STAMP;
            this.slidAt[exit] = NO_STAMP;
            this.occupied--;
            moved = true;
        }

        // De l'avant-dernière case vers l'entrée : chaque item avance d'au plus une case par
        // tick, et une file compressée avance d'un bloc en une seule passe.
        for (int slot = exit - 1; slot >= 0; slot--) {
            if (this.slots[slot] == null || this.slots[slot + 1] != null) continue;
            if (justArrived(slot, stamp)) continue;

            this.slots[slot + 1] = this.slots[slot];
            this.arrived[slot + 1] = this.arrived[slot];
            this.slidAt[slot + 1] = now;
            this.slots[slot] = null;
            this.arrived[slot] = NO_STAMP;
            this.slidAt[slot] = NO_STAMP;
            moved = true;
        }

        // Le tampon en dernier, une fois l'entrée libérée par le décalage. L'item garde
        // l'instant de son départ de l'amont : il poursuit le glissement commencé au lieu de le
        // reprendre à zéro.
        int entry = entrySlot();

        if (this.staged != null && this.slots[entry] == null) {
            this.slots[entry] = this.staged;
            this.arrived[entry] = stamp;
            this.slidAt[entry] = this.stagedAt;
            this.staged = null;
            this.stagedAt = NO_STAMP;
            this.occupied++;
            moved = true;
        }

        return moved;
    }

    /** Cette case a-t-elle été remplie de l'extérieur pendant le pas courant ? */
    private boolean justArrived(int slot, long stamp) {
        return stamp != NO_STAMP && this.arrived[slot] == stamp;
    }

    /** Vide la voie — la relecture d'un paquet de synchronisation écrase l'état précédent. */
    public void clear() {
        Arrays.fill(this.slots, null);
        Arrays.fill(this.arrived, NO_STAMP);
        Arrays.fill(this.slidAt, NO_STAMP);

        this.staged = null;
        this.stagedAt = NO_STAMP;
        this.occupied = 0;
    }

    // Interface (Rendu)

    /**
     * Position d'un item le long du bloc, de 0 à 1 — en deçà de 0 pour celui qui franchit
     * encore la frontière amont.
     *
     * <h3>Un pas de retard, pas de prédiction</h3>
     *
     * <p>Un item au repos est dessiné sur sa case. Un item qui vient de glisser va de la case
     * précédente à la sienne pendant le pas suivant. On ne dessine donc que ce qui s'est
     * <b>réellement</b> passé.
     *
     * <p>La version précédente devinait : un item glissait si la case devant lui était
     * <i>déjà</i> libre. Sur une bande saturée qui avance — le régime normal d'une usine — elle
     * ne l'est jamais : chaque item restait figé puis sautait d'une case entière à chaque pas.
     * Deviner mieux aurait demandé d'interroger toute la ligne en aval à chaque image, et la
     * moindre erreur aurait produit un saut. Le retard, lui, est d'un pas au plus, invisible.
     *
     * @param clock        instant du dernier tick
     * @param partialTick  fraction écoulée depuis, de 0 à 1
     * @param ticksPerSlot durée d'un pas, en ticks
     */
    public float progressOf(int slot, long clock, float partialTick, int ticksPerSlot) {
        return (slot + slide(this.slidAt[slot], clock, partialTick, ticksPerSlot)) / capacity();
    }

    /** Position de l'item du tampon : il franchit la frontière amont, de -1/capacité à 0. */
    public float stagedProgress(long clock, float partialTick, int ticksPerSlot) {
        return (entrySlot() + slide(this.stagedAt, clock, partialTick, ticksPerSlot)) / capacity();
    }

    /**
     * Retard sur la case, de -1 (le glissement commence) à 0 (arrivé).
     *
     * <p>La soustraction se fait en {@code long} : le temps du monde se compte en millions de
     * ticks, et un {@code float} n'y distingue plus deux ticks voisins, encore moins une
     * fraction d'image.
     */
    private static float slide(long at, long clock, float partialTick, int ticksPerSlot) {
        if (at == NO_STAMP) return 0f;

        float elapsed = ((clock - at) + partialTick) / ticksPerSlot;

        return elapsed >= 1f ? 0f : Math.max(elapsed, 0f) - 1f;
    }

    /**
     * Copie des instants de glissement, à rendre à {@link #keepMotion} après une relecture.
     *
     * <p>Une resynchronisation réécrit la voie entière ; sans cela, chaque paquet remettrait au
     * repos tous les items du bloc — un recul visible à chaque dépôt d'un inserter.
     */
    public long[] motion() {
        return this.slidAt.clone();
    }

    /** Rend les instants relevés par {@link #motion}, pour les cases occupées des deux côtés. */
    public void keepMotion(long[] previous) {
        for (int slot = 0; slot < this.slots.length && slot < previous.length; slot++) {
            if (this.slots[slot] != null) this.slidAt[slot] = previous[slot];
        }
    }
}
