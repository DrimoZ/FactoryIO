package com.drimoz.factoryio.content.crafter;

/**
 * L'effet des modules posés dans un crafter (FIO-127), aux valeurs de Factorio.
 *
 * <p>Chaque module ajoute ses bonus, et les bonus s'additionnent — vitesse, consommation,
 * productivité — avant d'être appliqués une seule fois :
 *
 * <table>
 *   <tr><th></th><th>vitesse</th><th>consommation</th><th>productivité</th></tr>
 *   <tr><td>vitesse 1 / 2 / 3</td><td>+20 / 30 / 50 %</td><td>+50 / 60 / 70 %</td><td></td></tr>
 *   <tr><td>productivité 1 / 2 / 3</td><td>−5 / 10 / 15 %</td><td>+40 / 60 / 80 %</td><td>+4 / 6 / 10 %</td></tr>
 *   <tr><td>efficacité 1 / 2 / 3</td><td></td><td>−30 / 40 / 50 %</td><td></td></tr>
 * </table>
 *
 * <p>Vitesse et consommation ne descendent jamais sous 20 % : c'est le plancher de Factorio,
 * qui empêche quatre modules d'efficacité de rendre une machine gratuite.
 *
 * <p>Les modules sont reconnus par les tags des inserters ({@code factor_io:upgrades/…}) :
 * {@code speed}, {@code efficiency}, et {@code capacity} — qui porte les modules de
 * productivité, et vaut productivité sur un crafter. Un même item sert donc aux deux machines.
 *
 * <p>Classe immuable, sans dépendance au jeu : testée en JUnit.
 */
public record CrafterModules(float speedMultiplier, float energyMultiplier, float productivity) {

    public static final CrafterModules NONE = new CrafterModules(1.0F, 1.0F, 0.0F);

    public enum Kind { SPEED, PRODUCTIVITY, EFFICIENCY }

    public static final int MAX_TIER = 3;
    private static final float FLOOR = 0.2F;

    private static final float[] SPEED_SPEED = { 0.20F, 0.30F, 0.50F };
    private static final float[] SPEED_ENERGY = { 0.50F, 0.60F, 0.70F };
    private static final float[] PRODUCTIVITY_BONUS = { 0.04F, 0.06F, 0.10F };
    private static final float[] PRODUCTIVITY_SPEED = { -0.05F, -0.10F, -0.15F };
    private static final float[] PRODUCTIVITY_ENERGY = { 0.40F, 0.60F, 0.80F };
    private static final float[] EFFICIENCY_ENERGY = { -0.30F, -0.40F, -0.50F };

    /**
     * @param counts nombre de modules posés, par nature ({@link Kind#ordinal}) puis par palier
     *               (indice 0 = palier 1)
     */
    public static CrafterModules of(int[][] counts) {
        float speed = 0.0F;
        float energy = 0.0F;
        float productivity = 0.0F;
        boolean any = false;

        for (int tier = 0; tier < MAX_TIER; tier++) {
            int s = counts[Kind.SPEED.ordinal()][tier];
            int p = counts[Kind.PRODUCTIVITY.ordinal()][tier];
            int e = counts[Kind.EFFICIENCY.ordinal()][tier];
            any |= s + p + e > 0;

            speed += s * SPEED_SPEED[tier] + p * PRODUCTIVITY_SPEED[tier];
            energy += s * SPEED_ENERGY[tier] + p * PRODUCTIVITY_ENERGY[tier] + e * EFFICIENCY_ENERGY[tier];
            productivity += p * PRODUCTIVITY_BONUS[tier];
        }

        if (!any) return NONE;
        return new CrafterModules(Math.max(FLOOR, 1.0F + speed), Math.max(FLOOR, 1.0F + energy), productivity);
    }

    /** Vitesse de fabrication effective. */
    public float speed(float base) {
        return base * this.speedMultiplier;
    }

    /** FE par tick de travail effectifs, jamais moins de 1 si la base en demande. */
    public int energyPerTick(int base) {
        if (base <= 0) return base;
        return Math.max(1, Math.round(base * this.energyMultiplier));
    }
}
