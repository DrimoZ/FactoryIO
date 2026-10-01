package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Captures automatiques des écrans du mod, pour les regarder sans jouer.
 *
 * <p>Outil de développement, absent du jar comme tout {@code gametest/}. Il ne fait rien sauf
 * si la variable d'environnement {@code FACTORIO_GUI_PREVIEW=1} est posée :
 * <pre>
 *   FACTORIO_GUI_PREVIEW=1 ./gradlew runClient
 * </pre>
 * Le client charge une <b>copie</b> du monde « Test FactorIO » (jamais l'original), pose la
 * démo {@code demo:crafter} loin de tout, ouvre chaque écran, enregistre
 * {@code run/screenshots/preview/*.png} et se ferme.
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID, value = Dist.CLIENT)
public final class GuiPreview {

    private static final boolean ENABLED = "1".equals(System.getenv("FACTORIO_GUI_PREVIEW"));
    private static final String SOURCE_WORLD = "Test FactorIO";
    private static final String WORLD = "GuiPreview";
    private static final BlockPos ORIGIN = new BlockPos(1000, 120, 1000);

    private record Step(int delay, Consumer<Minecraft> action) {}

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static boolean started;
    private static int wait;
    private static String pendingShot;

    private GuiPreview() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();

        if (!started && minecraft.screen instanceof TitleScreen title) {
            started = true;
            minecraft.options.pauseOnLostFocus = false;
            copyWorld(minecraft);
            script();
            minecraft.createWorldOpenFlows().loadLevel(title, WORLD);
            return;
        }
        if (!started || minecraft.player == null || minecraft.getSingleplayerServer() == null) return;
        if (pendingShot != null || STEPS.isEmpty()) return;
        if (wait-- > 0) return;

        Step step = STEPS.poll();
        step.action().accept(minecraft);
        wait = STEPS.isEmpty() ? 0 : STEPS.peek().delay();
    }

    /** La capture se prend après le rendu complet de l'image, pas au milieu d'un tick. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || pendingShot == null) return;
        Minecraft minecraft = Minecraft.getInstance();

        File file = new File(minecraft.gameDirectory, "screenshots/preview/" + pendingShot + ".png");
        file.getParentFile().mkdirs();
        try (NativeImage image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(file);
            FactoryIO.LOGGER.info("Aperçu d'écran : {}", file);
        } catch (IOException e) {
            FactoryIO.LOGGER.error("Capture impossible : {}", file, e);
        }
        pendingShot = null;
    }

    // Scénario

    private static void script() {
        step(40, mc -> command(mc, "gamemode creative"));
        step(5, mc -> command(mc, "tp @s " + ORIGIN.getX() + " " + ORIGIN.getY() + " " + ORIGIN.getZ() + " 0 30"));
        step(60, mc -> command(mc, "fill ~-2 ~-1 ~-2 ~10 ~-1 ~20 minecraft:smooth_stone"));
        step(5, mc -> command(mc, "function demo:crafter"));
        step(5, mc -> command(mc, "time set noon"));

        // Crafter Mk1, puis son sélecteur de recettes.
        step(80, mc -> use(mc, ORIGIN.offset(3, 0, 3)));
        step(5, mc -> hover(mc, 0.0, 0.0));
        step(30, mc -> shot("crafter_mk1"));
        step(5, mc -> hoverGui(mc, 25 + 13, 20 + 13));
        step(5, mc -> shot("crafter_mk1_recipe_tooltip"));
        step(5, mc -> clickRecipeSocket(mc));
        step(5, mc -> hover(mc, 0.0, 0.0));
        step(10, mc -> shot("crafter_picker"));
        step(5, mc -> hoverGui(mc, 7 + 18 * 4 + 8, 36 + 8));
        step(10, mc -> shot("crafter_picker_tooltip"));
        step(5, mc -> mc.setScreen(null));

        // L'inserter d'entrée de la première ligne.
        step(10, mc -> use(mc, ORIGIN.offset(1, 0, 3)));
        step(20, mc -> shot("inserter"));
        step(5, mc -> mc.setScreen(null));

        // Crafter Mk3, plus loin.
        step(10, mc -> command(mc, "tp @s " + ORIGIN.getX() + " " + ORIGIN.getY() + " " + (ORIGIN.getZ() + 12) + " 0 30"));
        step(40, mc -> use(mc, ORIGIN.offset(3, 0, 15)));
        step(5, mc -> hover(mc, 0.0, 0.0));
        step(30, mc -> shot("crafter_mk3"));
        step(5, mc -> mc.setScreen(null));

        step(10, Minecraft::stop);
    }

    private static void step(int delay, Consumer<Minecraft> action) {
        STEPS.add(new Step(delay, action));
    }

    private static void shot(String name) {
        pendingShot = name;
    }

    /** Exécutée par le serveur intégré, avec tous les droits : le monde n'a pas forcément les commandes. */
    private static void command(Minecraft minecraft, String command) {
        var server = minecraft.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(minecraft.player.getUUID());
            if (player != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
            }
        });
    }

    private static void use(Minecraft minecraft, BlockPos pos) {
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    /** Clique le grand slot de recette de l'écran de crafter, s'il est ouvert. */
    private static void clickRecipeSocket(Minecraft minecraft) {
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;
        try {
            Class<?> menu = Class.forName("com.drimoz.factoryio.content.crafter.CrafterMenu");
            int x = menu.getField("RECIPE_X").getInt(null) + 13;
            int y = menu.getField("RECIPE_Y").getInt(null) + 13;
            screen.mouseClicked(screen.getGuiLeft() + x, screen.getGuiTop() + y, 0);
        } catch (ReflectiveOperationException e) {
            FactoryIO.LOGGER.error("Slot de recette introuvable", e);
        }
    }

    /** Place le curseur sur un point de l'écran ouvert, en coordonnées de sa fenêtre. */
    private static void hoverGui(Minecraft minecraft, int x, int y) {
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;
        double scale = minecraft.getWindow().getGuiScale();
        hover(minecraft, (screen.getGuiLeft() + x) * scale / minecraft.getWindow().getScreenWidth(),
                (screen.getGuiTop() + y) * scale / minecraft.getWindow().getScreenHeight());
    }

    /** Place le curseur, en fraction de la fenêtre : les infobulles suivent la vraie souris. */
    private static void hover(Minecraft minecraft, double fx, double fy) {
        try {
            MouseHandler mouse = minecraft.mouseHandler;
            Field x = MouseHandler.class.getDeclaredField("xpos");
            Field y = MouseHandler.class.getDeclaredField("ypos");
            x.setAccessible(true);
            y.setAccessible(true);
            x.setDouble(mouse, minecraft.getWindow().getScreenWidth() * fx);
            y.setDouble(mouse, minecraft.getWindow().getScreenHeight() * fy);
        } catch (ReflectiveOperationException e) {
            FactoryIO.LOGGER.error("Curseur inaccessible", e);
        }
    }

    private static void copyWorld(Minecraft minecraft) {
        Path saves = minecraft.gameDirectory.toPath().resolve("saves");
        Path source = saves.resolve(SOURCE_WORLD);
        Path target = saves.resolve(WORLD);
        try {
            if (Files.exists(target)) {
                try (Stream<Path> walk = Files.walk(target)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
                }
            }
            try (Stream<Path> walk = Files.walk(source)) {
                for (Path path : (Iterable<Path>) walk::iterator) {
                    if (path.getFileName().toString().equals("session.lock")) continue;
                    Path copy = target.resolve(source.relativize(path).toString());
                    if (Files.isDirectory(path)) Files.createDirectories(copy);
                    else Files.copy(path, copy);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Copie du monde d'aperçu impossible", e);
        }
    }
}
