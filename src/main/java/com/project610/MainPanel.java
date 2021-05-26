package com.project610;

import com.project610.structs.JList2;
import com.project610.ui.DownloadPanel;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.project610.Utils.prefSize;

public class MainPanel extends JPanel {

    // This isn't great, but it works for now
    private final int SHUFFLE_NORMAL = 0;
    private final int SHUFFLE_HUE_IGNORE = 100;
    private final int SHUFFLE_SATURATION_IGNORE = 200;
    private final int SHUFFLE_SATURATION_ONLY_DOWN = 201;
    private final int SHUFFLE_SATURATION_ONLY_UP = 202;
    private final int SHUFFLE_BRIGHTNESS_IGNORE = 300;
    private final int SHUFFLE_BRIGHTNESS_ONLY_DOWN = 301;
    private final int SHUFFLE_BRIGHTNESS_ONLY_UP = 302;

    private JTextField installDirBox;
    JList2<String> spriteList;
    JList2<String> variantList;
    private JCheckBox freshStartBox;
    private JCheckBox shuffleColorsBox;
    private JCheckBox chaosShuffleBox;
    private JLabel imageView;
    private JTextArea console;
    private JScrollPane consoleScroll;
    private JDialog downloadDialog;

    private String graphicsPath = "data" + File.separator + "graphics" + File.separator + "00";
    private String spritesDirName = "sprites";
    private Path spritesPath;
    String extension = ".png";
    final String THUMBNAIL_NAME = "thumbnail";
    private float fontSize = 11f;
    public Random rand = new Random();

    ImageGenerator imageGenerator = new ImageGenerator(this);

    private boolean debug = false;
    private boolean inIDE = true;

    private int LOG_LEVEL = 6; // 3 err, 4 warn, *6 info*, 7 debug

    public TreeMap<String,Sprite> sprites = new TreeMap<>();
    public HashMap<Color, Float[]> adjustments = new HashMap<>(); // Remember to reset this when changing between variants/spritesheets!

    private JFrame parent;
    private HashSet<Component> blockables;

    MainPanel(String[] args, JFrame parent) {
        this.parent = parent;
        try {
            if (MainPanel.class.getClassLoader().getResource("MainPanel.class").toURI().getScheme().equalsIgnoreCase("jar")) {
                inIDE = false;
            }
        } catch (Exception ex) {
            // Meh
        }

        init();

        // TODO: Load base directory from stored settings
        // TODO: Store settings
        if (installDirBox.getText().trim().isEmpty()) {
            installDirBox.setText(Utils.guessInstallLocation(this));
        }

    }

    private void init() {
        removeAll();
        blockables = new HashSet<>();

        setLayout(new BorderLayout(0, 0));

        if (debug) setBackground(new Color (0.3f, 0.3f, 0.3f));

        JPanel topPane = new JPanel();
        if (debug) topPane.setBackground(new Color(1f, 0f, 0f));
        topPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(prefSize(topPane, 700, 30), BorderLayout.PAGE_START);

        topPane.add(prefSize(new JLabel("La-Mulana install directory"), 155, 20));

        installDirBox = new JTextField();
        blockables.add(installDirBox);
        topPane.add(prefSize(installDirBox, 350, 20));

        /////////// This is really kinda bad, and I should probably not do it over just fixing the root of the Manjaro giant-text issue
        topPane.add(new JLabel("Text size:"));

        JButton textSizeDownButton = new JButton("-");
        textSizeDownButton.setMargin(new Insets(0,0,0,0));
        textSizeDownButton.addActionListener(e -> changeFont(this, --fontSize));
        topPane.add(prefSize(textSizeDownButton, 20, 20));

        JButton textSizeUpButton = new JButton("+");
        textSizeUpButton.setMargin(new Insets(0,0,0,0));
        textSizeUpButton.addActionListener(e -> changeFont(this, ++fontSize));
        topPane.add(prefSize(textSizeUpButton, 20, 20));



        JPanel midPane = new JPanel(new BorderLayout(0, 0));
        if (debug) midPane.setBackground(new Color(1f, 1f, 0f));
        midPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));

        midPane.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(prefSize(midPane, 700, 300), BorderLayout.CENTER);

        JPanel spriteListPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (debug) spriteListPane.setBackground(new Color(0.3f, 0.3f, 0.6f));
        spriteListPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.lightGray));
        midPane.add(prefSize(spriteListPane, 170, 300), BorderLayout.LINE_START);

        spriteListPane.add(prefSize(new JLabel("   Sprite"), 170, 20));
        spriteList = new JList2<>();
        blockables.add(spriteList);
        spriteList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        spriteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spriteList.setVisibleRowCount(12);
        spriteList.setPrototypeCellValue("Lick my balls");
        spriteListPane.add(prefSize(spriteList, 159, 266));

        spriteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) return;

            variantList.clear();
            variantList.addAll(sprites.get(spriteList.getSelectedValue()).variants.keySet());
        });

        JPanel variantListPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (debug) variantListPane.setBackground(new Color(0.4f, 0.4f, 0.8f));
        variantListPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.lightGray));
        midPane.add(prefSize(variantListPane, 170, 300), BorderLayout.CENTER);

        variantListPane.add(prefSize(new JLabel("   Variant"), 170, 20));
        variantList = new JList2<>();
        blockables.add(variantList);
        variantList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        variantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variantListPane.add(prefSize(variantList, 159, 266));

        variantList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) return;

            Sprite currentSprite = sprites.get(spriteList.getSelectedValue());
            Variant currentVariant = currentSprite.variants.get(variantList.getSelectedValue());

            newAdjustments();

            try {
                // Ideally, a thumbnail will be provided to give an idea of what's being set
                String spritesheetName = THUMBNAIL_NAME;
                BufferedImage thumbnail;
                try {
                    thumbnail = currentVariant.spritesheetImages.get(THUMBNAIL_NAME);
                    thumbnail.getRGB(0,0);
                }
                // If there's no thumbnail, fall back on whatever first spritesheet we can find
                // TODO: Prioritize a non-thumbnail image, depending on context
                catch (Exception ex) {
                    try {
                        if (currentSprite.label.equalsIgnoreCase("lemeza")) {
                            spritesheetName = "00prof";
                        }
                        else if (currentSprite.label.equalsIgnoreCase("tiamat")) {
                            spritesheetName = "b07";
                        }
                        else {
                            // Surely this is bad practice
                            throw new Exception();
                        }
                    } catch (Exception ex2) {
                        for (String key : currentVariant.spritesheetImages.keySet()) {
                            spritesheetName = key;
                            break;
                        }
                    }
                }

                BufferedImage newImage = null;
                if (spritesheetName.equalsIgnoreCase(THUMBNAIL_NAME)) {
                        newImage = currentVariant.spritesheetImages.get(spritesheetName);
                }
                else {
                    newImage = generateImageForVariant2(currentVariant, spritesheetName);
                }

                // Scale image preview to fit imageView in previewPane
                AffineTransform transform = new AffineTransform();
                double scale = 2;
                scale = Math.min(scale, (double)(imageView.getWidth() - 10)/newImage.getWidth());
                scale = Math.min(scale, (double)(imageView.getHeight()- 20)/newImage.getHeight());
                transform.scale(scale, scale);
                AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
                BufferedImage scaledImage = new BufferedImage((int)(newImage.getWidth()*scale),(int)(newImage.getHeight()*scale), BufferedImage.TYPE_INT_ARGB);

                scaledImage = transformOp.filter(newImage, scaledImage);
                imageView.setIcon(new ImageIcon(scaledImage));

            } catch (Exception ex) {
                error("Failed to display image", ex);
            }
        });

        JPanel previewPane = new JPanel();
        if (debug) previewPane.setBackground(new Color(0.6f, 0.6f, 1.0f));
        previewPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        midPane.add(previewPane, BorderLayout.LINE_END);
        midPane.add(prefSize(previewPane, 350, 300), BorderLayout.LINE_END);

        freshStartBox = new JCheckBox("Fresh start", true);
        blockables.add(freshStartBox);
        freshStartBox.setToolTipText("Avoid overwriting previous changes, when possible");
        previewPane.add(prefSize(freshStartBox, 90, 20));

        JButton applyButton = new JButton("Apply");
        blockables.add(applyButton);
        previewPane.add(prefSize(applyButton, 70, 22));
        applyButton.addActionListener(e -> save());

        JButton refreshButton = new JButton("Refresh sprite files");
        blockables.add(refreshButton);
        refreshButton.addActionListener(e -> loadSprites()); // TODO: Make this work in-IDE again (Probably after changing packaged sprites to a download)
        previewPane.add(prefSize(refreshButton, 150, 22));

        shuffleColorsBox = new JCheckBox("Shuffle colours", false);
        blockables.add(shuffleColorsBox);
        shuffleColorsBox.setToolTipText("Will randomize colours if the variant has a colour mask (eg: `00prof-COLORMASK.png`)");
        previewPane.add(prefSize(shuffleColorsBox, 120, 22));

        chaosShuffleBox = new JCheckBox("Chaos shuffle", false);
        blockables.add(chaosShuffleBox);
        chaosShuffleBox.setToolTipText("Will NOT try to keep consistency between spritesheets in a variant. Also, try to ensure at least kind-of significant shuffling");
        previewPane.add(prefSize(chaosShuffleBox, 120, 22));



        /*JButton colorButton = new JButton("†");
        previewPane.add(prefSize(colorButton, 10, 10));
        colorButton.addActionListener(e -> {
            imageView.getIcon()
        });*/

        imageView = new JLabel();
        previewPane.add(prefSize(imageView, 300, 270));

        JPanel bottomPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (debug) bottomPane.setBackground(new Color(0f, 1f, 0f));
        add(prefSize(bottomPane, 700, 220), BorderLayout.PAGE_END);

        bottomPane.add(prefSize(new JLabel("   Console"), 80, 20));
        JButton clearConsoleButton = new JButton("x");
        bottomPane.add(clearConsoleButton);
        clearConsoleButton.addActionListener(e -> console.setText(""));

        console = new JTextArea();
        console.setLineWrap(true);
        console.setEditable(false);
        console.setFont(console.getFont().deriveFont(11f));
        consoleScroll = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (debug) consoleScroll.setBackground(new Color(1f,0f,1f));
        bottomPane.add(prefSize(consoleScroll, 680, 180));

        changeFont(this, fontSize);

        downloadDialog = new JDialog(parent, "Download sprites?");
        downloadDialog.setSize(400,95);
        downloadDialog.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 200);
        downloadDialog.setModal(true);

        DownloadPanel downloadPanel = new DownloadPanel(this);
        downloadDialog.add(downloadPanel);


        console.append(":)");
    }

    private void changeFont(Container c, float size) {
        for (Component c2 : c.getComponents()) {
            c2.setFont(c2.getFont().deriveFont(size));
            if (c2 instanceof Container) {
                changeFont((Container)c2, size);
            }
        }
    }

    // TODO: Thread-ify this bad boy, to stop UI from freezing
    public void downloadSprites() {
        final String ERASABLE_ZIP_PATH = "LaMulanaSpriteSwapper-main/";
        final String tempDir = "tmp";
        final String zipPath = tempDir + File.separator + "lmss-main.zip";

        ReadableByteChannel rbc = null;
        FileOutputStream zipFileOutputStream = null;
        FileInputStream zipFileInputStream = null;
        ZipInputStream zipInputStream = null;

        try {
            info("Downloading sprites from github: https://github.com/Virus610/LaMulanaSpriteSwapper");
            URL url = new URL("https://github.com/Virus610/LaMulanaSpriteSwapper/archive/refs/heads/main.zip");
            rbc = Channels.newChannel(url.openStream());

            // Download LMSS repo zip to temp folder
            Files.createDirectories(Paths.get("tmp"));
            File zipFile = new File(zipPath);
            zipFileOutputStream = new FileOutputStream(zipFile);
            zipFileOutputStream.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            // Read zip into memory, copy stuff out to sprites folder (Don't overwrite stuff, in case user has local edits)
            zipFileInputStream = new FileInputStream(zipPath);
            zipInputStream = new ZipInputStream(zipFileInputStream);
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().contains("/sprites/")) {
                    String outputPath = entry.getName().replace(ERASABLE_ZIP_PATH, "");
                    if (entry.isDirectory()) {
                        Files.createDirectories(Paths.get(outputPath));
                    } else {
                        try {
                            FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(zipPath), null);
                            Files.copy(fileSystem.getPath(entry.getName()), Paths.get(outputPath));
                        }
                        catch (FileAlreadyExistsException ex) {
                            // IDGAF right now
                        }
                        catch (Exception ex) {
                            error("Failed to extract sprite file from zip", ex);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            error("Failed to download sprites from github", ex);
        } finally {
            closeThing(rbc);
            closeThing(zipFileOutputStream);
            closeThing(zipFileInputStream);
            closeThing(zipInputStream);

            try {
                Files.deleteIfExists(Paths.get(zipPath));
            } catch (Exception ex) {
                error("Failed to clean up downloaded zip containing sprites.", ex);
            }
        }
    }

    private void closeThing(Closeable s) {
        try {
            if (null != s) s.close();
        } catch (Exception ex) {
            // Nyeh!
        }
    }

    // Load sprites from disk (`sprites` folder in same dir as app)
    // If sprites folder doesn't exist, offer to download it for the user
    //  * Don't do any internet stuff without asking
    public void loadSprites() {

        try {
            // Ignore this. Proof of concept for re-scaling the window if I want to eventually allow minimizing the console
//            Dimension dim = parent.getMinimumSize();
//            dim.setSize(dim.width*1.1, dim.height*1.3);
//            parent.setMinimumSize(dim);

            if (!Files.exists(Paths.get(spritesDirName))) {
                warn("No sprites folder located. Prompting to download from main repository.");
                downloadDialog.setVisible(true);
                downloadDialog.toFront();
                return;
            }

            info("Loading sprites from disk...");

            // If something is already selected, store the selected values for re-selection if/after reloading
            String selectedSprite = spriteList.getSelectedValue();
            String selectedVariant = variantList.getSelectedValue();
            sprites.clear();
            spriteList.clear();
            variantList.clear();

            spritesPath = Paths.get("sprites");
            Path[] spriteFiles = listFiles(spritesPath);

            // Get all sprite options
            for (Path spriteFile : spriteFiles) {
                if (!Files.isDirectory(spriteFile)) {
                    info("Skipping non-directory spriteFile: " + spriteFile.toString());
                    continue;
                }
                String spriteName = spriteFile.getFileName().toString();
                Sprite newSprite = new Sprite().setLabel(spriteName);

                // Get variations of sprite
                try {
                    Path[] variants = listFiles(spriteFile);
                    for (Path variant : variants) {
                        if (!Files.isDirectory(variant)) {
                            info("Skipping non-directory variant: " + variant.toString());
                            continue;
                        }
                        String variantName = variant.getFileName().toString();
                        Variant newVariant = new Variant().setName(variantName);

                        // Get images of variation
                        try {
                            Path[] spritesheets = listFiles(variant);
                            HashMap<String, BufferedImage> spritesheetImages = new HashMap<>();
                            HashMap<String, BufferedImage> spritesheetMasks = new HashMap<>();
                            HashMap<String, BufferedImage> spritesheetColorMasks = new HashMap<>();
                            for (Path spritesheet : spritesheets) {
                                String filename = spritesheet.getFileName().toString();
                                if (filename.toLowerCase().endsWith("-mask.png")) {
                                    spritesheetMasks.put(filename.toLowerCase().replace("-mask.png", ""), ImageIO.read(new File(spritesheet.toUri())));
                                } else if (filename.toLowerCase().endsWith("-colormask.png")) {
                                    spritesheetColorMasks.put(filename.toLowerCase().replace("-colormask.png", ""), ImageIO.read(new File(spritesheet.toUri())));
                                } else if (filename.toLowerCase().endsWith("-colourmask.png")) {
                                    spritesheetColorMasks.put(filename.toLowerCase().replace("-colourmask.png", ""), ImageIO.read(new File(spritesheet.toUri())));
                                } else if (filename.toLowerCase().endsWith(".png")) {
                                    spritesheetImages.put(filename.toLowerCase().replace(".png", ""), ImageIO.read(new File(spritesheet.toUri())));
                                }
                            }
                            newVariant.addImages(spritesheetImages);
                            newVariant.addMasks(spritesheetMasks);
                            newVariant.addColorMasks(spritesheetColorMasks);

                            newSprite.addVariant(newVariant.name, newVariant);

                            sprites.put(newSprite.label, newSprite);
                        } catch (Exception ex) {
                            error("Guh...", ex);
                        }
                    }
                } catch (Exception ex) {
                    error("Sprite messed up somehow", ex);
                }
            }

            for (String key : sprites.keySet()) {
                spriteList.add(key);
            }
            if (null != selectedSprite) {
                spriteList.setSelectedValue(selectedSprite, true);
                spriteList.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(this, 0, 0, true));
                if (null != selectedVariant) {
                    variantList.setSelectedValue(selectedVariant, true);
                    variantList.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(this, 0, 0, true));
                }
            }
            info("Loading complete!");
        }
        catch (Exception ex) {
            error("loadSprites failed in a major way.", ex);
        }
    }

    String path() {
        return installDirBox.getText() + File.separator + graphicsPath;
    }

    public HashMap<String, BufferedImage> generateImagesForVariant(Variant variant) {
        HashMap<String, BufferedImage> images = new HashMap<>();

        for (String key : variant.spritesheetImages.keySet()) {
            try {
                // Skip thumbnails when generating images - Those don't get copied to LM graphics folder
                if (key.equalsIgnoreCase(THUMBNAIL_NAME)) {
                    continue;
                }
                images.put(key, generateImageForVariant2(variant, key));
            } catch (Exception ex) {
                error("Failed to generate image", ex);
            }
        }
        return images;
    }

    // Reset adjustments for colour shuffling (New variant, or chaos + new spritesheet)
    public HashMap<Color, Float[]> newAdjustments() {
        info("New adjustments");
        adjustments = new HashMap<>();

        // Add null 'Color' for areas with no colorMask
        adjustments.put(
                null,
                new Float[]{rand.nextFloat(), rand.nextFloat() * 0.36f - 0.24f, rand.nextFloat() * 0.25f - 0.25f}
        );
        return adjustments;
    }

    private BufferedImage generateImageForVariant2(Variant variant, String key) throws IOException {
        info("Generating image: " + path() + File.separator + key + extension);
        BufferedImage newImage = null;
        if (freshStartBox.isSelected()) {
            newImage = copyImage(sprites.get(spriteList.getSelectedValue()).variants.get("DEFAULT").spritesheetImages.get(key));
        } else {
            try {
                newImage = ImageIO.read(new File(path() + File.separator + key + extension));
            } catch (Exception ex) {
                error("Could not find/load base image at: " + path() + File.separator + key + extension, ex);
            }
        }
        BufferedImage replacement = variant.spritesheetImages.get(key);
        BufferedImage mask = variant.spritesheetMasks.get(key);
        BufferedImage colorMask = variant.spritesheetColorMasks.get(key);

        Color transparent = new Color(0,0,0,0);

        // If there's no mask, delete everything
        if (null == mask) {
            newImage = copyImage(replacement);
        }
        if (null != mask || shuffleColorsBox.isSelected()) {
            if (chaosShuffleBox.isSelected()) {
                newAdjustments();
            }
            int shuffleMods = 0;
            if (Arrays.asList("01effect", "fog00", "fog01").contains(key.toLowerCase())) {
                info("BRIGHTNESS ONLY DOWN for: " + key);
                shuffleMods = SHUFFLE_BRIGHTNESS_ONLY_DOWN;
            }
            for (int y = 0; y < replacement.getHeight(); y++) {
                for (int x = 0; x < replacement.getWidth(); x++) {
                    Color replacementColor = new Color(replacement.getRGB(x, y), true);
                    int replacementAlpha = replacementColor.getAlpha();
                    // Replace the pixel if it's not totally transparent
                    if (replacementAlpha != 0) {
                        newImage.setRGB(x, y, replacement.getRGB(x, y));
                        if (shuffleColorsBox.isSelected()) {
                            newImage.setRGB(x, y, shuffleRGB(x, y, replacement, colorMask, shuffleMods));
                        }
                    }
                    // If it was totally transparent, check if this is something that should be left alone
                    // Delete areas where the mask colour is non-black
                    else if (null != mask && new Color(mask.getRGB(x, y)).getBlue() != 0) {
                        newImage.setRGB(x, y, transparent.getRGB());
                    }
                }
            }
        }

        return newImage;
    }

    private int shuffleRGB(int x, int y, BufferedImage img, BufferedImage colorMask) {
        return shuffleRGB(x, y, img, colorMask, SHUFFLE_NORMAL);
    }

    private int shuffleRGB(int x, int y, BufferedImage img, BufferedImage colorMask, int mods) {

        int transparent = new Color(0,0,0,0).getRGB();
        int black = new Color(0,0,0).getRGB();

        Color pixel = new Color(img.getRGB(x, y),true);
        Color maskPixel = (null == colorMask) ? null : new Color(colorMask.getRGB(x, y), true);

        // Add new randomized adjustment if this maskPixel is a colour not yet seen for this variant
        float h = rand.nextFloat();
        float s = rand.nextFloat()*0.36f-0.24f;
        float b = rand.nextFloat()*0.25f-0.25f; // Used to be +/- 0.25, now just -0.25 ~ 0

        switch (mods) {
            case SHUFFLE_SATURATION_IGNORE:
                s = 0;
                break;
            case SHUFFLE_SATURATION_ONLY_DOWN:
                s = rand.nextFloat()*0.36f-0.36f;
                break;
            case SHUFFLE_SATURATION_ONLY_UP:
                s =rand.nextFloat()*0.36f;
                break;
            case SHUFFLE_BRIGHTNESS_IGNORE:
                b = 0;
                break;
            case SHUFFLE_BRIGHTNESS_ONLY_DOWN: // Kinda pointless, since I'm gonna not increase brightness for ugliness-prevention. Though, I guess I could multiply instead of adding brightness, and that would solve a lot of my issues.
                b = rand.nextFloat()*0.25f-0.25f;
                break;
            case SHUFFLE_BRIGHTNESS_ONLY_UP:
                b =rand.nextFloat()*0.25f;
                break;
        }

        // +0.2 ~ +0.8 hue, to avoid too-similar-to-original
        if (chaosShuffleBox.isSelected() && mods != SHUFFLE_HUE_IGNORE) {
            h = rand.nextFloat() * 0.6f + 0.2f;
        }

        if (null == adjustments.get(maskPixel)) {
            adjustments.put(maskPixel,
                    new Float[] {h, s, b});
        }

        // Preserve black pixels for some sanity
        if (img.getRGB(x,y) != black) {
            if (pixel.getRGB() != transparent && (null == maskPixel || maskPixel.getRGB() != transparent)) {
                try {
                    return adjustPixelColor(pixel.getRGB(), adjustments.get(maskPixel));
                } catch (Exception ex) {
                    error("Something broke shuffling RGB at (" + x + "," + y + ")\n" +
                            "        pixel="+pixel +"\n"+
                            "    maskPixel=" +maskPixel+"\n"+
                            "  adjustments="+adjustments.get(maskPixel), ex);
                }
            }
        }
        return img.getRGB(x, y);
    }

    private int adjustPixelColor(int rgb, Float[] adjustment) {
        Color pixel = new Color(rgb, true);
        float[] hsb = Color.RGBtoHSB(pixel.getRed(), pixel.getGreen(), pixel.getBlue(), null);
        for (int i = 0; i < hsb.length; i++) {
            hsb[i] += adjustment[i];
        }
        Color tempPixel = new Color(Color.HSBtoRGB(hsb[0], floatClamp(hsb[1]), floatClamp(hsb[2])));
        Color newPixel = new Color(tempPixel.getRed(), tempPixel.getGreen(), tempPixel.getBlue(), pixel.getAlpha());
        return newPixel.getRGB();
    }

    private float floatClamp(float f) {
        return Math.min(1, Math.max(0, f));
    }

    private Path[] listFiles(Path path) {
        try {
            Object[] files = Files.list(path).toArray();
            return Arrays.copyOf(files, files.length, Path[].class);
        }
        catch (Exception ex) {
            error("Fffff- Failed to list files in: " + path, ex);
        }
        return null;
    }

    private BufferedImage copyImage(BufferedImage source){
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    private void save() {
        try {
            info("Trying to save changes");

            if (installDirBox.getText().trim().length() == 0 || !Files.exists(Paths.get(path()))) {
                installDirBox.setBackground(new Color(1.0f, 0.7f, 0.7f));
                warn("Failed to locate graphics directory at: " + path());
                return;
            } else {
                installDirBox.setBackground(new Color(1.0f, 1.0f, 1.0f));
            }

            // Process/save the images in another thread, so the UI doesn't hang
            new Thread(imageGenerator).start();
        }
        catch (Exception ex) {
            error("Save failed :(", ex);
        }
    }

    public void blockUI() {
        for (Component c : blockables) {
            c.setEnabled(false);
        }
    }

    public void unblockUI() {
        for (Component c : blockables) {
            c.setEnabled(true);
        }
    }

    public void info(String s) {
        System.out.println(s);
        console("[INFO]  " + s, 6);
    }

    public void warn(String s) {
        System.err.println(s);
        console("[WARN]  " + s, 4);
    }

    public void error(String s, @Nullable Exception ex) {
        System.err.println(s);
        console("[ERROR] " + s, 3);
        if (null != ex) {
            ex.printStackTrace();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            console(sw.toString() + "\n", 3);
        }
    }

    private void console(String s, int level) {
        if (level <= LOG_LEVEL) {
            console.append("\n" + s);

            // TODO: Clear out old stuff to avoid memory leaks
//            if (console.getText().length() > 2048) {
//
//            }

            // TODO: Only scroll if scrolled to the end
//            JScrollBar scroll = consoleScroll.getVerticalScrollBar();
//            scroll.revalidate();
//            System.out.println(scroll.getValue() + " / " + scroll.getMaximum());
            console.setCaretPosition(console.getDocument().getLength());
        }
    }
}
