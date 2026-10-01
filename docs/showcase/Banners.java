import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * Bandeaux de la page CurseForge, dans la police de Minecraft, sur les captures de la vitrine.
 *
 * <pre>
 *   java docs/showcase/Banners.java run/screenshots/showcase docs/showcase/out
 * </pre>
 *
 * La police vient du jar client que ForgeGradle a déjà téléchargé : elle n'est pas versionnée.
 * L'éditeur de description de CurseForge refuse toute image de plus de 850 px de large.
 */
public class Banners {

    static final int W = 850;
    static final Color INK = new Color(14, 12, 10);
    static final Color ACCENT = new Color(240, 170, 60);
    static final Color SUB = new Color(255, 205, 130);

    static BufferedImage font;
    static final int[] widths = new int[256];

    public static void main(String[] args) throws Exception {
        File shots = new File(args[0]), out = new File(args[1]);
        out.mkdirs();
        loadFont();

        banner(shots, out);
        header(shots, out, "header_inserters", "inserters_row", 0.5, "Inserters", "Seven of them, from coal to stack filter");
        header(shots, out, "header_belts", "belt_tiers", 0.45, "Transport belts", "Two lanes, three tiers, and ramps");
        header(shots, out, "header_crafters", "crafter_row", 0.55, "Crafters", "Factorio's assembling machine, in three tiers");
        header(shots, out, "header_modules", "crafter_mk3", 0.5, "Modules", "Speed, productivity, efficiency - in both machines");
        header(shots, out, "header_data", "ramp_hall", 0.5, "Data-driven", "New machines from a JSON file, any number by datapack");
        header(shots, out, "header_missing", "belt_curve", 0.5, "Not in this build", "Said plainly, because finding out in-game is worse");

        itemSheet(out, "items_components", "Components",
                "iron_plate", "copper_plate", "steel_plate", "iron_gear_wheel", "copper_cable",
                "electronic_circuit", "advanced_circuit", "processing_unit");
        itemSheet(out, "items_modules", "Modules and tools",
                "speed_module", "speed_module_2", "speed_module_3",
                "productivity_module", "productivity_module_2", "productivity_module_3",
                "efficiency_module", "efficiency_module_2", "efficiency_module_3",
                "advanced_redstone_module", "configurator");

        // Captures pour la page et le wiki : 850 de large pour CurseForge, en JPEG.
        for (File f : shots.listFiles((d, n) -> n.endsWith(".png"))) {
            BufferedImage src = ImageIO.read(f);
            String name = f.getName().replace(".png", "");
            boolean gui = name.startsWith("gui_");
            BufferedImage img = gui ? cropGui(src) : src;
            jpeg(scale(img, Math.min(W, img.getWidth()) * 1.0 / img.getWidth()), new File(out, name + "_850.jpg"));
            if (!gui) jpeg(scale(img, 1280.0 / img.getWidth()), new File(out, name + ".jpg"));
            else ImageIO.write(img, "png", new File(out, name + ".png"));
        }
    }

    /** Bannière de tête : le titre en grand, sur la plus belle image. */
    static void banner(File shots, File out) throws Exception {
        int h = 300;
        BufferedImage b = cover(ImageIO.read(new File(shots, "crafter_row.png")), W, h, 0.45);
        Graphics2D g = b.createGraphics();
        g.setPaint(new GradientPaint(0, 0, alpha(INK, 235), W * 0.7f, 0, alpha(INK, 0)));
        g.fillRect(0, 0, W, h);
        g.setPaint(new GradientPaint(0, h * 0.55f, alpha(INK, 0), 0, h, alpha(INK, 150)));
        g.fillRect(0, 0, W, h);
        int x = 34;
        text(g, "Factor'I/O", x, 58, 7, Color.WHITE);
        text(g, "Factorio's machines, in Minecraft.", x + 2, 140, 2, new Color(236, 236, 240));
        text(g, "Inserters - transport belts - crafters", x + 2, 176, 2, SUB);
        text(g, "Forge 1.20.1", x + 2, 230, 2, alpha(new Color(220, 220, 220), 220));
        g.dispose();
        ImageIO.write(b, "png", new File(out, "banner.png"));
    }

    /** Bandeau de section : une tranche floutée de capture derrière un titre. */
    static void header(File shots, File out, String name, String shot, double fy, String title, String subtitle) throws Exception {
        int h = 100;
        BufferedImage img = blur(cover(ImageIO.read(new File(shots, shot + ".png")), W, h, fy));
        Graphics2D g = img.createGraphics();
        g.setColor(alpha(INK, 150));
        g.fillRect(0, 0, W, h);
        g.setPaint(new GradientPaint(0, 0, alpha(INK, 210), W * 0.75f, 0, alpha(INK, 30)));
        g.fillRect(0, 0, W, h);
        g.setColor(ACCENT);
        g.fillRect(0, 0, 6, h);
        text(g, title, 28, 20, 4, Color.WHITE);
        text(g, subtitle, 30, 64, 2, SUB);
        g.dispose();
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }

    /** Les textures d'items du dépôt, agrandies sans lissage, avec leur nom anglais. */
    static void itemSheet(File out, String name, String title, String... ids) throws Exception {
        String lang = java.nio.file.Files.readString(Path.of("src/main/resources/assets/factor_io/lang/en_us.json"));
        int cols = 3, cell = W / cols, icon = 96, rowH = icon + 52, top = 64;
        int rows = (ids.length + cols - 1) / cols;
        BufferedImage img = new BufferedImage(W, top + rows * rowH + 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(40, 36, 32), 0, img.getHeight(), new Color(22, 20, 18)));
        g.fillRect(0, 0, W, img.getHeight());
        g.setColor(ACCENT);
        g.fillRect(0, 0, 6, img.getHeight());
        text(g, title, 28, 18, 3, Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < ids.length; i++) {
            int cx = (i % cols) * cell, cy = top + (i / cols) * rowH;
            BufferedImage tex = ImageIO.read(new File("src/main/resources/assets/factor_io/textures/item/" + ids[i] + ".png"));
            g.setColor(new Color(255, 255, 255, 14));
            g.fillRect(cx + (cell - icon) / 2 - 8, cy - 4, icon + 16, icon + 8);
            g.drawImage(tex.getSubimage(0, 0, 16, 16), cx + (cell - icon) / 2, cy, icon, icon, null);
            String label = label(lang, ids[i]);
            int scale = width(label) * 2 > cell - 12 ? 1 : 2;
            text(g, label, cx + (cell - width(label) * scale) / 2, cy + icon + 14, scale, SUB);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }

    static String label(String lang, String id) {
        var m = java.util.regex.Pattern.compile("\"item\\.factor_io\\." + id + "\"\\s*:\\s*\"([^\"]*)\"").matcher(lang);
        if (m.find()) return m.group(1);
        // Les modules de palier 2 et 3 partagent la clé du palier 1, avec un chiffre.
        String base = id.replaceAll("_[23]$", "");
        var b = java.util.regex.Pattern.compile("\"item\\.factor_io\\." + base + "\"\\s*:\\s*\"([^\"]*)\"").matcher(lang);
        return (b.find() ? b.group(1) : id) + (id.equals(base) ? "" : " " + id.substring(id.length() - 1));
    }

    static int width(String s) {
        int w = 0;
        for (char ch : s.toCharArray()) w += widths[ch] + 1;
        return w;
    }

    // Police de Minecraft : cellules de 8, un pixel d'interlettre, ombre au quart de la couleur.

    static void loadFont() throws Exception {
        Path jar = Path.of(System.getProperty("user.home"),
                ".gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar");
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            font = ImageIO.read(zip.getInputStream(zip.getEntry("assets/minecraft/textures/font/ascii.png")));
        }
        for (int c = 0; c < 256; c++) {
            int gx = (c % 16) * 8, gy = (c / 16) * 8, w = 0;
            for (int x = 7; x >= 0 && w == 0; x--)
                for (int y = 0; y < 8; y++) if ((font.getRGB(gx + x, gy + y) >>> 24) > 0) { w = x + 1; break; }
            widths[c] = c == ' ' ? 4 : w;
        }
    }

    static void text(Graphics2D g, String s, int x, int y, int scale, Color c) {
        glyphs(g, s, x + scale, y + scale, scale, new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, c.getAlpha()));
        glyphs(g, s, x, y, scale, c);
    }

    static void glyphs(Graphics2D g, String s, int x, int y, int scale, Color c) {
        g.setColor(c);
        for (char ch : s.toCharArray()) {
            int gx = (ch % 16) * 8, gy = (ch / 16) * 8;
            for (int yy = 0; yy < 8; yy++)
                for (int xx = 0; xx < 8; xx++)
                    if ((font.getRGB(gx + xx, gy + yy) >>> 24) > 0) g.fillRect(x + xx * scale, y + yy * scale, scale, scale);
            x += (widths[ch] + 1) * scale;
        }
    }

    // Images

    static BufferedImage cover(BufferedImage src, int w, int h, double fy) {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        double s = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
        int sw = (int) (src.getWidth() * s), sh = (int) (src.getHeight() * s);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, (w - sw) / 2, (int) (-(sh - h) * fy), sw, sh, null);
        g.dispose();
        return o;
    }

    static BufferedImage scale(BufferedImage src, double f) {
        int w = (int) Math.round(src.getWidth() * f), h = (int) Math.round(src.getHeight() * f);
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return o;
    }

    static BufferedImage blur(BufferedImage src) {
        int r = 5, n = (2 * r + 1) * (2 * r + 1);
        float[] k = new float[n];
        java.util.Arrays.fill(k, 1f / n);
        BufferedImage padded = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        new ConvolveOp(new Kernel(2 * r + 1, 2 * r + 1, k), ConvolveOp.EDGE_NO_OP, null).filter(src, padded);
        return padded;
    }

    /** L'écran ouvert : la plus grande zone qui n'est pas le monde assombri derrière. */
    static BufferedImage cropGui(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int x1 = w, y1 = h, x2 = 0, y2 = 0;
        for (int y = 0; y < h; y += 2)
            for (int x = 0; x < w; x += 2) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 255, gr = (rgb >> 8) & 255, b = rgb & 255;
                // Le fond de GUI vanilla est un gris clair (#C6C6C6) ; les onglets sont vifs.
                boolean panel = Math.abs(r - 198) < 6 && Math.abs(gr - 198) < 6 && Math.abs(b - 198) < 6;
                if (panel) { x1 = Math.min(x1, x); y1 = Math.min(y1, y); x2 = Math.max(x2, x); y2 = Math.max(y2, y); }
            }
        if (x2 <= x1) return src;
        int pad = 260; // les onglets débordent largement de la fenêtre
        x1 = Math.max(0, x1 - pad); x2 = Math.min(w, x2 + pad);
        y1 = Math.max(0, y1 - 40); y2 = Math.min(h, y2 + 40);
        return src.getSubimage(x1, y1, x2 - x1, y2 - y1);
    }

    static void jpeg(BufferedImage img, File file) throws Exception {
        var writer = ImageIO.getImageWritersByFormatName("jpg").next();
        var param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.88f);
        try (var stream = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(stream);
            writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
        }
        writer.dispose();
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
