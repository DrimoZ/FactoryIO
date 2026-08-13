package com.drimoz.factoryio.core.belts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * Pente d'un convoyeur, et surtout <b>où il débouche</b>.
 *
 * <h2>Ce que la pente change, et ce qu'elle ne change pas</h2>
 *
 * <p>Elle ne change <b>rien</b> au transport : {@link BeltLane} compte des cases, et une case
 * en pente est une case. Ce qu'elle change, c'est la position du bloc aval — et c'est tout le
 * sujet, parce qu'une connexion mal résolue coupe la ligne sans rien casser de visible.
 *
 * <h2>La règle : la sortie fait autorité</h2>
 *
 * <p>Chaque convoyeur sait où il débouche : {@link #exit(BlockPos, Direction)}. Un convoyeur
 * ne cherche donc pas « qui est derrière moi » à partir de sa propre forme — il demande aux
 * trois candidats possibles <b>lequel débouche sur lui</b>.
 *
 * <p>Ce sens est le seul qui marche. Une montée dépose à {@code +1} ; le convoyeur qui reçoit
 * a son entrée à son propre niveau, donc s'il déduisait son amont de sa seule forme il
 * chercherait un bloc qui n'est pas là. C'est exactement la logique des rails vanilla, qui
 * regardent aussi un cran au-dessus et un cran en dessous.
 *
 * <h2>Une rampe est forcément droite</h2>
 *
 * <p>Contrainte du format, pas choix de conception : un élément de modèle de bloc n'admet
 * <b>qu'une seule rotation, sur un seul axe</b>. Les modèles de virage ont déjà consommé la
 * leur sur Y. Une pente en demanderait une sur X — impossible de cumuler. Un virage reste
 * donc plat, et {@link #allowsCurve()} le dit une fois pour toutes.
 */
public enum BeltSlope implements StringRepresentable {

    /** À plat : le convoyeur débouche sur son voisin, au même niveau. */
    FLAT("flat", 0),

    /** En montée : le convoyeur débouche un bloc plus haut. */
    UP("up", 1),

    /** En descente : le convoyeur débouche un bloc plus bas. */
    DOWN("down", -1);

    private final String name;
    private final int rise;

    BeltSlope(String name, int rise) {
        this.name = name;
        this.rise = rise;
    }

    // Interface

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** Dénivelé entre l'entrée et la sortie, en blocs. */
    public int rise() {
        return this.rise;
    }

    /** @return {@code true} si cette pente peut se combiner à un virage */
    public boolean allowsCurve() {
        return this == FLAT;
    }

    /**
     * Position du bloc que ce convoyeur alimente.
     *
     * <p>Une montée dépose sur la diagonale <b>avant et au-dessus</b> : c'est la géométrie
     * d'un rail vanilla ascendant, dont le sommet touche le coin bas du bloc suivant.
     */
    public BlockPos exit(BlockPos pos, Direction facing) {
        return pos.relative(facing).above(this.rise);
    }

    /**
     * Position d'où viendrait un convoyeur de cette pente qui alimenterait {@code pos}.
     *
     * <p>L'inverse de {@link #exit} : utile pour tester un candidat sans avoir à le lire.
     */
    public BlockPos entryFrom(BlockPos pos, Direction facing) {
        return pos.relative(facing.getOpposite()).below(this.rise);
    }

    // Interface (Statique)

    /**
     * Les trois positions susceptibles d'alimenter {@code pos}.
     *
     * <p>Elles se déduisent de {@link #exit} en l'inversant : un convoyeur de pente
     * {@code r} débouchant sur {@code pos} se trouve en {@code pos − facing − r}.
     *
     * <ul>
     *   <li>même niveau — un convoyeur plat ;</li>
     *   <li>un cran <b>plus bas</b> — une montée qui arrive ;</li>
     *   <li>un cran <b>plus haut</b> — une descente qui arrive.</li>
     * </ul>
     *
     * <p>L'appelant doit ensuite vérifier que le candidat <b>débouche réellement</b> sur
     * {@code pos} : un convoyeur perpendiculaire occupe l'une de ces positions sans rien y
     * déverser. D'où {@link #feeds}.
     */
    public static List<BlockPos> upstreamCandidates(BlockPos pos, Direction facing) {
        BlockPos behind = pos.relative(facing.getOpposite());

        return List.of(behind, behind.below(), behind.above());
    }

    /**
     * Ce convoyeur alimente-t-il {@code target} ?
     *
     * <p>La seule question qui compte pour établir une connexion, et elle ne dépend que de
     * l'amont : sa position, son orientation et sa pente.
     */
    public static boolean feeds(BlockPos from, Direction facing, BeltSlope slope, BlockPos target) {
        return slope.exit(from, facing).equals(target);
    }

    public static BeltSlope byName(String name) {
        for (BeltSlope slope : values()) {
            if (slope.name.equals(name)) return slope;
        }

        return FLAT;
    }
}
