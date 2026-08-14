package com.drimoz.factoryio.core.belts;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forme visible d'un convoyeur.
 *
 * <p>Ce qui se teste ici n'est pas l'apparence — aucun test ne dira qu'un virage est joli —
 * mais la <b>table</b> : chaque combinaison de voisins doit donner la valeur de
 * {@code connected} dont le modèle correspondant existe. Une valeur fausse affiche une bande
 * droite là où le joueur a construit un coude, et rien dans le journal ne le dit.
 */
class BeltShapeTest {

    // La table, valeur par valeur

    /**
     * Les huit valeurs de {@code connected}, telles que relevées sur les modèles.
     *
     * <p>Colonnes : entrée arrière, entrée gauche, entrée droite, sortie → {@code connected}.
     */
    @ParameterizedTest(name = "arrière={0} gauche={1} droite={2} sortie={3} → connected={4}")
    @CsvSource({
            // Bandes droites : aucune entrée, ou une entrée arrière.
            "false, false, false, false, 0",
            "true,  false, false, false, 3",
            "false, false, false, true,  2",
            "true,  false, false, true,  1",

            // Virages : l'unique entrée est latérale.
            "false, true,  false, false, 4",
            "false, true,  false, true,  5",
            "false, false, true,  false, 6",
            "false, false, true,  true,  7",

            // Un T reste droit : l'entrée arrière l'emporte sur la latérale.
            "true,  true,  false, true,  1",
            "true,  false, true,  true,  1",

            // Deux entrées latérales fusionnent, sans coude.
            "false, true,  true,  true,  1",
    })
    @DisplayName("Chaque combinaison de voisins donne la valeur attendue")
    void theTableIsExhaustive(boolean back, boolean left, boolean right, boolean output, int expected) {
        assertEquals(expected, BeltShape.connectedOf(back, left, right, output, true));
    }

    @Test
    @DisplayName("Les huit valeurs sont atteignables, et aucune n'est produite deux fois pour rien")
    void allEightValuesAreReachable() {
        Set<Integer> produced = new HashSet<>();

        for (boolean back : new boolean[] {false, true}) {
            for (boolean left : new boolean[] {false, true}) {
                for (boolean right : new boolean[] {false, true}) {
                    for (boolean output : new boolean[] {false, true}) {
                        produced.add(BeltShape.connectedOf(back, left, right, output, true));
                    }
                }
            }
        }

        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6, 7), produced,
                "chaque modèle du dépôt doit correspondre à une situation réelle");
    }

    @ParameterizedTest
    @CsvSource({"false, false, false", "true, true, true", "false, true, true"})
    @DisplayName("La valeur produite reste dans le domaine de la propriété")
    void connectedStaysInRange(boolean back, boolean left, boolean output) {
        int value = BeltShape.connectedOf(back, left, false, output, true);

        assertTrue(value >= 0 && value <= BeltShape.MAX_CONNECTED, "connected = " + value);
    }

    // La règle du virage

    /**
     * Le point de conception, et celui qu'une première rédaction du document avait inversé.
     *
     * <p>Le virage appartient à la bande qui <b>reçoit</b> par le côté, pas à celle qui entre.
     * Une bande n'a qu'un {@code facing}, sa sortie : dans un coude, c'est la tuile où la
     * direction change qui pointe vers la nouvelle direction et reçoit latéralement.
     */
    @Test
    @DisplayName("Un virage se déduit quand l'unique entrée est latérale")
    void aCurveIsDefinedByItsLoneLateralInput() {
        assertAll(
                () -> assertEquals(BeltShape.CURVE_LEFT, BeltShape.of(false, true, false, true)),
                () -> assertEquals(BeltShape.CURVE_RIGHT, BeltShape.of(false, false, true, true)),
                () -> assertEquals(BeltShape.STRAIGHT, BeltShape.of(true, true, false, true),
                        "avec une entrée arrière, c'est un T"),
                () -> assertEquals(BeltShape.STRAIGHT, BeltShape.of(false, true, true, true),
                        "deux entrées latérales fusionnent"));
    }

    /**
     * En pente, jamais de virage.
     *
     * <p>Contrainte du format et non choix de conception : un élément de modèle de bloc
     * n'admet qu'une seule rotation, sur un seul axe, et les modèles de virage ont déjà
     * consommé la leur sur Y.
     */
    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    @DisplayName("Une rampe reste droite, même avec une entrée latérale")
    void aRampNeverCurves(boolean left, boolean right) {
        assertEquals(BeltShape.STRAIGHT, BeltShape.of(false, left, right, false));
    }

    @Test
    @DisplayName("Un ascenseur interdit le virage, et la forme le reflète")
    void flowAndShapeAgree() {
        for (BeltFlow flow : BeltFlow.values()) {
            BeltShape shape = BeltShape.of(false, true, false, flow.allowsCurve());

            assertEquals(flow.isHorizontal() ? BeltShape.CURVE_LEFT : BeltShape.STRAIGHT, shape,
                    "sens " + flow.getSerializedName());
        }
    }

    // Les côtés

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("Gauche et droite sont opposées, et perpendiculaires à la sortie")
    void sidesAreConsistent(Direction facing) {
        Direction left = BeltShape.leftOf(facing);
        Direction right = BeltShape.rightOf(facing);

        assertAll(
                () -> assertEquals(left.getOpposite(), right, "gauche et droite sont opposées"),
                () -> assertTrue(left.getAxis() != facing.getAxis(), "perpendiculaire à la sortie"),
                () -> assertTrue(left.getAxis().isHorizontal(), "un côté reste horizontal"));
    }

    @Test
    @DisplayName("Vu depuis sa sortie, une bande orientée au nord a l'ouest à sa gauche")
    void leftOfNorthIsWest() {
        // Fixé explicitement : une inversion gauche/droite mettrait les virages du mauvais
        // côté sans qu'aucune propriété générale ne le relève.
        assertAll(
                () -> assertEquals(Direction.WEST, BeltShape.leftOf(Direction.NORTH)),
                () -> assertEquals(Direction.EAST, BeltShape.rightOf(Direction.NORTH)));
    }

    // La réciproque, dont dépend le rendu

    /**
     * Le rendu n'a que {@code connected} sous la main, et doit en retrouver la forme.
     *
     * <p>Se tromper ici ne casse rien de visible sur le bloc — le modèle, lui, vient du
     * blockstate — mais fait traverser les virages en ligne droite aux items.
     */
    @ParameterizedTest
    @EnumSource(BeltShape.class)
    @DisplayName("Chaque valeur produite se relit comme la forme qui l'a produite")
    void connectedRoundTrips(BeltShape shape) {
        for (boolean hasInput : new boolean[] {false, true}) {
            for (boolean hasOutput : new boolean[] {false, true}) {
                int connected = shape.connected(hasInput, hasOutput);

                // Un virage sans entrée retombe sur la bande droite : c'est cette forme-là,
                // et non le virage, que le rendu doit relire.
                BeltShape expected = shape.isCurve() && !hasInput ? BeltShape.STRAIGHT : shape;

                assertEquals(expected, BeltShape.of(connected),
                        shape + " avec entrée=" + hasInput + " sortie=" + hasOutput);
            }
        }
    }

    @Test
    @DisplayName("Une valeur hors table se relit comme une bande droite")
    void unknownConnectedReadsAsStraight() {
        assertEquals(BeltShape.STRAIGHT, BeltShape.of(BeltShape.MAX_CONNECTED + 1));
    }

    // Le trajet d'un item

    /**
     * Sur un virage, l'item n'entre pas dans la direction du bloc.
     *
     * <p>Recevoir par la gauche veut dire que l'amont pousse <b>de gauche à droite</b> : sa
     * direction de circulation est celle qui va du côté gauche vers le centre. L'inverser
     * ferait entrer les items par le mauvais bord.
     */
    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    @DisplayName("L'entrée d'un virage circule depuis le côté qui l'alimente")
    void curveEntryComesFromItsFeedingSide(Direction facing) {
        assertAll(
                () -> assertEquals(facing, BeltShape.STRAIGHT.entryTravel(facing),
                        "une bande droite entre comme elle sort"),
                () -> assertEquals(BeltShape.rightOf(facing), BeltShape.CURVE_LEFT.entryTravel(facing),
                        "recevoir par la gauche, c'est être poussé vers la droite"),
                () -> assertEquals(BeltShape.leftOf(facing), BeltShape.CURVE_RIGHT.entryTravel(facing),
                        "recevoir par la droite, c'est être poussé vers la gauche"),
                () -> assertTrue(BeltShape.CURVE_LEFT.entryTravel(facing).getAxis() != facing.getAxis(),
                        "un virage tourne : l'entrée est perpendiculaire à la sortie"));
    }

    // Robustesse

    @Test
    @DisplayName("Un virage sans entrée n'existe pas, et retombe sur la bande droite")
    void aCurveWithoutInputFallsBackToStraight() {
        // Impossible à produire par « of », mais un état posé par commande ne doit pas
        // casser le rendu.
        assertAll(
                () -> assertEquals(0, BeltShape.CURVE_LEFT.connected(false, false)),
                () -> assertEquals(2, BeltShape.CURVE_RIGHT.connected(false, true)));
    }
}
