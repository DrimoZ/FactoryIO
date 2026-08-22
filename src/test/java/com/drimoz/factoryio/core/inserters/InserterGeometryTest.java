package com.drimoz.factoryio.core.inserters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structure des géométries d'inserter (FIO-066).
 *
 * <h2>Pourquoi tester un fichier d'assets</h2>
 *
 * <p>Le bone {@code turret} n'est pas l'œuvre d'un modeleur mais celle d'un script
 * ({@code tools/restructure-geo.js}). Un ré-export depuis Blockbench le ferait disparaître
 * <b>en silence</b> : la géométrie se chargerait, l'inserter s'afficherait, et il ne
 * bougerait plus. C'est exactement le mode de panne de BUG-016, où une animation visait un
 * bone inexistant sans que rien ne le signale pendant des mois.
 *
 * <p>Ces cas ne vérifient donc pas une image — aucun test ne le peut — mais les invariants
 * de fichier dont l'animation dépend. Ils lisent les {@code .geo.json} depuis le classpath :
 * du parsing, donc du calcul pur, donc du JUnit et non un GameTest.
 */
class InserterGeometryTest {

    private static final String ROOT_BONE = "inserter";
    private static final String TURRET_BONE = "turret";

    /** Bagues du palier qui doivent suivre la tourelle. La troisième, « base », reste au sol. */
    private static final List<String> ROTATING_RINGS = List.of("bearing", "base_top");

    /** Bague fixe : elle porte la rotation, elle ne la subit pas. */
    private static final String FIXED_RING = "base";

    // Chargement

    private record Bone(String name, String parent, List<double[]> pivots, JsonArray cubes) {}

    private static Map<String, Bone> bonesOf(String geometry) {
        String path = "/assets/factor_io/geo/" + geometry + ".geo.json";

        try (InputStream in = InserterGeometryTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "géométrie introuvable sur le classpath : " + path);

            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

            JsonArray bones = root.getAsJsonArray("minecraft:geometry")
                    .get(0).getAsJsonObject().getAsJsonArray("bones");

            Map<String, Bone> byName = new HashMap<>();
            for (var element : bones) {
                JsonObject bone = element.getAsJsonObject();

                JsonArray pivot = bone.getAsJsonArray("pivot");
                List<double[]> pivots = new ArrayList<>();
                pivots.add(new double[]{
                        pivot.get(0).getAsDouble(), pivot.get(1).getAsDouble(), pivot.get(2).getAsDouble()});

                byName.put(bone.get("name").getAsString(), new Bone(
                        bone.get("name").getAsString(),
                        bone.has("parent") ? bone.get("parent").getAsString() : null,
                        pivots,
                        bone.has("cubes") ? bone.getAsJsonArray("cubes") : new JsonArray()));
            }

            return byName;
        } catch (Exception e) {
            throw new AssertionError("lecture impossible de " + path, e);
        }
    }

    private static double top(JsonObject cube) {
        return cube.getAsJsonArray("origin").get(1).getAsDouble()
                + cube.getAsJsonArray("size").get(1).getAsDouble();
    }

    // Les invariants

    @ParameterizedTest
    @ValueSource(strings = {"energy_inserter", "filter_inserter", "fuel_inserter"})
    @DisplayName("Le bone de tourelle existe et pend à la racine")
    void turretExists(String geometry) {
        Map<String, Bone> bones = bonesOf(geometry);
        Bone turret = bones.get(TURRET_BONE);

        assertNotNull(turret, "bone « " + TURRET_BONE + " » absent — un ré-export l'a effacé ?");
        assertEquals(ROOT_BONE, turret.parent(), "la tourelle doit pendre à « " + ROOT_BONE + " »");
        assertTrue(turret.cubes().size() > 0, "la tourelle ne porte aucun cube");
    }

    @ParameterizedTest
    @ValueSource(strings = {"energy_inserter", "filter_inserter", "fuel_inserter"})
    @DisplayName("Le pivot de la tourelle est sur l'axe du bloc")
    void turretPivotIsOnTheAxis(String geometry) {
        double[] pivot = bonesOf(geometry).get(TURRET_BONE).pivots().get(0);

        // C'est cette propriété qui rend la rotation insensible à la convention de signe de
        // GeckoLib sur les pivots de bone : l'opposé de zéro vaut zéro. La perdre
        // réintroduirait un doute que rien d'automatique ne pourrait plus lever.
        assertAll(
                () -> assertEquals(0.0, pivot[0], 1e-9, "pivot x"),
                () -> assertEquals(0.0, pivot[2], 1e-9, "pivot z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"energy_inserter", "filter_inserter", "fuel_inserter"})
    @DisplayName("Les deux bagues supérieures tournent, la bague fixe reste au sol")
    void ringsAreSplit(String geometry) {
        Map<String, Bone> bones = bonesOf(geometry);

        assertAll(
                () -> ROTATING_RINGS.forEach(ring -> assertEquals(
                        TURRET_BONE, bones.get(ring).parent(),
                        "« " + ring + " » doit tourner avec la tourelle")),

                // Sans bague fixe, le palier tourne d'un bloc et le mouvement perd sa
                // lisibilité : c'est le « en partie » du geste demandé.
                () -> assertEquals(ROOT_BONE, bones.get(FIXED_RING).parent(),
                        "« " + FIXED_RING + " » doit rester au sol"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"energy_inserter", "filter_inserter", "fuel_inserter"})
    @DisplayName("Aucune pièce du bras n'est restée dans la partie statique")
    void noArmPieceLeftBehind(String geometry) {
        // Le bras culmine à plus de 15 unités, la partie statique plafonne à 6 : un cube haut
        // resté dans la racine serait une pièce oubliée qui ne suivrait pas le mouvement.
        for (var element : bonesOf(geometry).get(ROOT_BONE).cubes()) {
            JsonObject cube = element.getAsJsonObject();

            assertTrue(top(cube) <= 6.0,
                    "cube haut (sommet " + top(cube) + ") resté dans « " + ROOT_BONE + " »");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"energy_inserter", "filter_inserter", "fuel_inserter"})
    @DisplayName("La partie statique garde ses pieds")
    void staticPartKeepsItsLegs(String geometry) {
        JsonArray statics = bonesOf(geometry).get(ROOT_BONE).cubes();

        // Trois pieds de trois cubes, plus six patins : le script ne doit pas avoir emporté
        // le socle avec la tourelle, sans quoi l'inserter tournerait en entier sur lui-même.
        assertTrue(statics.size() >= 15,
                "la partie statique ne compte que " + statics.size() + " cubes");
    }
}
