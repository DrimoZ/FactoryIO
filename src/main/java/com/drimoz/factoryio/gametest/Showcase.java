package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.multiblock.MultiblockBlock;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static com.drimoz.factoryio.gametest.GuiPreview.command;
import static com.drimoz.factoryio.gametest.GuiPreview.shot;
import static com.drimoz.factoryio.gametest.GuiPreview.step;

/**
 * Captures de la page CurseForge et du wiki : des lignes de production construites par le
 * datapack {@code docs/showcase/datapack}, photographiées sans interface.
 *
 * <pre>
 *   FACTORIO_GUI_PREVIEW=showcase ./gradlew runClient -Pshowcase
 * </pre>
 * Le monde « Showcase » est créé une fois, à graine fixe ; le chantier est posé dans la plaine
 * la plus proche du spawn et reconstruit à chaque lancement. Les images vont dans
 * {@code run/screenshots/showcase/}. Avec {@code -Pshowcase}, Oculus lit le pack de shaders
 * posé dans {@code run/shaderpacks/}.
 */
final class Showcase {

    static final String MODE = "showcase";

    private static final String WORLD = "Showcase";
    private static final long SEED = 20261001L;

    /** Le chantier : x et z depuis le coin, y depuis le sol. */
    private static final int SIZE_X = 72, SIZE_Z = 60, HEIGHT = 16;

    /** Largeur de la pente qui raccorde le chantier au terrain naturel. */
    private static final int FEATHER = 24;

    private static volatile BlockPos site;
    private static int clock;

    private Showcase() {}

    static void open(Minecraft minecraft, TitleScreen title) {
        if (Files.isDirectory(minecraft.gameDirectory.toPath().resolve("saves").resolve(WORLD))) {
            minecraft.createWorldOpenFlows().loadLevel(title, WORLD);
            return;
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, rules, WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(SEED, false, false),
                WorldPresets::createNormalWorldDimensions);
    }

    /** Les machines du chantier ne manquent jamais d'énergie : aucune source visible à l'image. */
    static void tick(Minecraft minecraft) {
        if (site == null || ++clock % 20 != 0 || minecraft.getSingleplayerServer() == null) return;
        var server = minecraft.getSingleplayerServer();
        server.execute(() -> recharge(server.overworld()));
    }

    // Scénario

    static void script() {
        step(60, mc -> command(mc, "gamemode spectator"));
        step(5, mc -> mc.getSingleplayerServer().execute(() -> findSite(mc.getSingleplayerServer().overworld())));
        step(40, mc -> command(mc, "tp @s " + at(SIZE_X / 2, 20, SIZE_Z / 2) + " 0 60"));
        step(100, mc -> command(mc, "forceload add " + site.getX() + " " + site.getZ() + " "
                + (site.getX() + SIZE_X) + " " + (site.getZ() + SIZE_Z)));
        step(5, mc -> command(mc, "forceload add " + (site.getX() - FEATHER) + " " + (site.getZ() - FEATHER) + " "
                + (site.getX() + SIZE_X + FEATHER) + " " + (site.getZ() + SIZE_Z + FEATHER)));
        step(100, mc -> mc.getSingleplayerServer().execute(() -> blend(mc.getSingleplayerServer().overworld())));
        step(20, Showcase::installDatapack);
        step(10, mc -> command(mc, "reload"));
        step(60, mc -> command(mc, "execute positioned " + at(0, 0, 0) + " run function showcase:build"));
        step(20, mc -> mc.getSingleplayerServer().execute(() -> settle(mc.getSingleplayerServer().overworld())));
        step(5, mc -> command(mc, "time set 2500"));
        step(5, mc -> command(mc, "weather clear"));
        step(5, mc -> {
            mc.options.hideGui = true;
            mc.options.fov().set(70);
        });

        // Laisser les lignes se remplir avant la première image.
        // Des plans serrés, à hauteur de machine : ce sont les blocs qu'on montre, pas le décor.
        view(900, "inserter_swing", 31.4, 1.2, 38.6, 30, 0.5, 41);
        view("inserters_row", 7.8, 1.0, 38.9, 34, 0.3, 41.3);
        view("filter_inserter", 27.3, 1.2, 38.6, 26, 0.4, 41);
        view("belt_lanes", 10.5, 1.4, 15.6, 14, 0.1, 14);
        view("belt_curve", 37.6, 1.6, 6.4, 40, 0.1, 9);
        view("ramp_hall", 37.2, 2.2, 11.4, 40, 0.8, 14.5);
        view("ramp_outdoor", 55.2, 3.5, 21.6, 58, 0.9, 16);
        view("belt_tiers", 50, 1.5, 21, 56, 0.1, 16);
        view("crafter_feed", 17.2, 3.1, 15.3, 14, 0.8, 12);
        view("crafter_mk3", 32.3, 3.8, 15.6, 35, 1, 11);
        view("crafter_row", 11.5, 4.2, 16, 30, 0.6, 21);

        // Les écrans, en créatif : un spectateur n'ouvre rien. À moins de trois blocs, sans quoi
        // le serveur refuse l'interaction.
        step(40, mc -> {
            mc.options.hideGui = false;
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
            ShowcaseJei.hideOverlay();
            command(mc, "gamemode creative");
        });
        gui("gui_inserter", 26, 2.6, 43, site -> site.offset(26, 0, 41));
        step(5, mc -> GuiPreview.clickGui(mc, -11, 4 + 11));
        step(20, mc -> shot("gui_inserter_info"));
        step(5, mc -> mc.setScreen(null));
        gui("gui_crafter", 34, 2.6, 13, site -> site.offset(35, 0, 11));
        step(5, GuiPreview::clickRecipeSocket);
        step(20, mc -> shot("gui_picker"));
        step(5, mc -> mc.setScreen(null));

        // Les items : l'onglet créatif du mod, puis une page de recettes de JEI.
        step(10, Showcase::openCreativeTab);
        step(5, mc -> GuiPreview.hover(mc, 0.0, 0.0));
        step(20, mc -> shot("gui_creative"));
        step(5, mc -> mc.setScreen(null));
        step(10, mc -> ShowcaseJei.showCrafterRecipes());
        step(5, mc -> GuiPreview.hover(mc, 0.0, 0.0));
        step(20, mc -> shot("gui_jei"));
        step(5, mc -> mc.setScreen(null));

        step(20, net.minecraft.client.Minecraft::stop);
    }

    private static String at(int dx, int dy, int dz) {
        return (site.getX() + dx) + " " + (site.getY() + dy) + " " + (site.getZ() + dz);
    }

    private static void view(String name, double ex, double ey, double ez, double tx, double ty, double tz) {
        view(5, name, ex, ey, ez, tx, ty, tz);
    }

    /** Une image : l'œil et le point visé, en coordonnées du chantier (centres de blocs). */
    private static void view(int delay, String name, double ex, double ey, double ez, double tx, double ty, double tz) {
        step(delay, mc -> camera(mc, ex, ey, ez, tx, ty, tz));
        step(60, mc -> shot(name));
    }

    /** Ouvre l'écran d'un bloc, regardé de près, et le photographie. */
    private static void gui(String name, double ex, double ey, double ez, java.util.function.Function<BlockPos, BlockPos> block) {
        step(5, mc -> {
            BlockPos target = block.apply(site);
            camera(mc, ex, ey, ez, target.getX() - site.getX(), 0, target.getZ() - site.getZ());
        });
        step(20, mc -> GuiPreview.use(mc, block.apply(site)));
        step(5, mc -> {
            GuiPreview.hover(mc, 0.0, 0.0);
            mc.gui.getChat().clearMessages(false);
        });
        step(30, mc -> shot(name));
    }

    /** L'inventaire créatif, ouvert sur l'onglet du mod. */
    private static void openCreativeTab(Minecraft minecraft) {
        var screen = new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(minecraft.player,
                minecraft.player.connection.enabledFeatures(), minecraft.options.operatorItemsTab().get());
        minecraft.setScreen(screen);
        try {
            var select = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class
                    .getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, com.drimoz.factoryio.shared.ModCreativeTab.MOD_TAB.get());
        } catch (ReflectiveOperationException e) {
            FactoryIO.LOGGER.error("Onglet créatif inaccessible", e);
        }
    }

    static void camera(Minecraft minecraft, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        // tp place les pieds ; l'œil est 1,62 bloc plus haut.
        command(minecraft, "tp @s " + (site.getX() + ex + 0.5) + " " + (site.getY() + ey - 1.62) + " "
                + (site.getZ() + ez + 0.5) + " " + yaw + " " + pitch);
    }

    /**
     * Raccorde le chantier au terrain naturel : à plat dessus, puis une pente en douceur sur
     * {@link #FEATHER} blocs. Les arbres coupés par la pente disparaissent en entier.
     */
    private static void blend(ServerLevel level) {
        int padY = site.getY();
        var dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        var grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = FEATHER + 8;

        for (int x = site.getX() - reach; x <= site.getX() + SIZE_X + reach; x++) {
            for (int z = site.getZ() - reach; z <= site.getZ() + SIZE_Z + reach; z++) {
                int ox = Math.max(0, Math.max(site.getX() - x, x - site.getX() - SIZE_X));
                int oz = Math.max(0, Math.max(site.getZ() - z, z - site.getZ() - SIZE_Z));
                double d = Math.sqrt(ox * ox + oz * oz);
                int natural = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int top = Math.max(natural, padY) + 32;

                if (d >= FEATHER) {
                    // Au-delà de la pente, seulement ôter les arbres qu'elle a coupés.
                    for (int y = natural; y <= top; y++) {
                        BlockState s = level.getBlockState(pos.set(x, y, z));
                        if (s.is(net.minecraft.tags.BlockTags.LEAVES) || s.is(net.minecraft.tags.BlockTags.LOGS)) {
                            level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                        }
                    }
                    continue;
                }
                double t = d / FEATHER;
                t = t * t * (3 - 2 * t);
                int ground = (int) Math.round(padY + (natural - padY) * t);

                for (int y = ground; y <= top; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                }
                for (int y = Math.min(natural, ground) - 3; y < ground - 1; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isSolidRender(level, pos)) level.setBlock(pos, dirt, Block.UPDATE_CLIENTS);
                }
                level.setBlock(pos.set(x, ground - 1, z), grass, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void findSite(ServerLevel level) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                biome -> biome.is(Biomes.PLAINS), BlockPos.ZERO, 6400, 32, 64);
        BlockPos centre = found == null ? BlockPos.ZERO : found.getFirst();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ());
        site = new BlockPos(centre.getX() - SIZE_X / 2, y, centre.getZ() - SIZE_Z / 2);
        FactoryIO.LOGGER.info("Chantier de la vitrine : {}", site);
    }

    /** Le datapack vit dans le dépôt, pour que les scènes se rejouent à l'identique. */
    private static void installDatapack(Minecraft minecraft) {
        Path source = minecraft.gameDirectory.toPath().resolve("../docs/showcase/datapack").normalize();
        Path target = minecraft.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                .resolve("showcase");
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path copy = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(copy);
                else Files.copy(path, copy, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Datapack de la vitrine introuvable : " + source, e);
        }
    }

    /**
     * {@code setblock} ne calcule ni la forme d'un convoyeur ni les parties d'un multibloc :
     * on rejoue ici ce que ferait une pose à la main.
     */
    private static void settle(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(site, site.offset(SIZE_X, HEIGHT, SIZE_Z))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getBlock() instanceof MultiblockBlock multiblock) {
                multiblock.placeParts(level, pos.immutable(), state.getValue(BlockStateProperties.HORIZONTAL_FACING));
                continue;
            }
            BlockState settled = Block.updateFromNeighbourShapes(state, level, pos);
            if (settled != state) level.setBlock(pos, settled, Block.UPDATE_CLIENTS);
        }
    }

    private static void recharge(ServerLevel level) {
        for (int cx = site.getX() >> 4; cx <= (site.getX() + SIZE_X) >> 4; cx++) {
            for (int cz = site.getZ() >> 4; cz <= (site.getZ() + SIZE_Z) >> 4; cz++) {
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    CompoundTag tag = be.saveWithoutMetadata();
                    String key = tag.contains("inserterEnergyLevel") ? "inserterEnergyLevel"
                            : tag.contains("energy") && tag.contains("recipe") ? "energy" : null;
                    if (key == null) continue;
                    tag.putInt(key, Integer.MAX_VALUE);
                    be.load(tag);
                }
            }
        }
    }
}
