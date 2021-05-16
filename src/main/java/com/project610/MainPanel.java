package com.project610;

import com.project610.structs.JList2;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

class MainPanel extends JPanel {

    private JTextField installDirBox;
    private JList2<String> spriteList;
    private JList2<String> variantList;
    private JCheckBox freshStart;
    private JLabel imageView;
    private JTextArea console;
    private JScrollPane consoleScroll;

    private String graphicsPath = "data" + File.separator + "graphics" + File.separator + "00";
    private String spritesDirName = "sprites";
    private Path spritesPath;
    private String extension = ".png";
    final private String THUMBNAIL_NAME = "thumbnail";

    private boolean debug = false;
    private boolean inIDE = true;

    private int LOG_LEVEL = 6; // 3 err, 4 warn, 6 info, 7 debug

    private HashMap<String,Sprite> sprites = new HashMap<>();

    private JFrame parent;

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

        loadSprites();
    }

    private void init() {
        removeAll();

        setLayout(new BorderLayout(0, 0));

        if (debug) setBackground(new Color (0.3f, 0.3f, 0.3f));

        JPanel topPane = new JPanel();
        if (debug) topPane.setBackground(new Color(1f, 0f, 0f));
        topPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(prefSize(topPane, 700, 30), BorderLayout.PAGE_START);

        topPane.add(prefSize(new JLabel("La-Mulana install directory"), 155, 20));

        installDirBox = new JTextField(40);
        topPane.add(prefSize(installDirBox, 400, 20));




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
        variantList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        variantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variantListPane.add(prefSize(variantList, 159, 266));

        variantList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) return;

            Sprite currentSprite = sprites.get(spriteList.getSelectedValue());
            Variant currentVariant = currentSprite.variants.get(variantList.getSelectedValue());

            try {
                // Ideally, a thumbnail will be provided to give an idea of what's being set
                String spritesheetName = THUMBNAIL_NAME;
                BufferedImage thumbnail;
                try {
                    thumbnail = currentVariant.spritesheetImages.get(THUMBNAIL_NAME);
                    thumbnail.getRGB(0,0);
                }
                // If there's no thumbnail, fall back on whatever first spritesheet we can find
                // TODO: Maybe prioritize something, in some way, somehow
                catch (Exception ex) {
                    for (String key : currentVariant.spritesheetImages.keySet()) {
                        spritesheetName = key;
                        break;
                    }
                }

                BufferedImage newImage = null;
                if (spritesheetName.equalsIgnoreCase(THUMBNAIL_NAME)) {
                        newImage = currentVariant.spritesheetImages.get(spritesheetName);
                }
                else {
                    newImage = generateImage(currentVariant, spritesheetName);
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

        freshStart = new JCheckBox("Fresh start", true);
        previewPane.add(prefSize(freshStart, 100, 20));

        JButton applyButton = new JButton("Apply");
        previewPane.add(prefSize(applyButton, 70, 22));
        applyButton.addActionListener(e -> save());

        JButton refreshButton = new JButton("Refresh sprite files");
        previewPane.add(prefSize(refreshButton, 150, 22));
        refreshButton.addActionListener(e -> loadSprites());

        imageView = new JLabel();
        previewPane.add(prefSize(imageView, 300, 270));

        JPanel bottomPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (debug) bottomPane.setBackground(new Color(0f, 1f, 0f));
        add(prefSize(bottomPane, 700, 220), BorderLayout.PAGE_END);

        bottomPane.add(prefSize(new JLabel("   Console"), 666, 20));

        console = new JTextArea();
        console.setLineWrap(true);
        console.setEditable(false);
        console.setFont(console.getFont().deriveFont(11f));
        consoleScroll = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (debug) consoleScroll.setBackground(new Color(1f,0f,1f));
        bottomPane.add(prefSize(consoleScroll, 680, 180));

        console.append(":)");
    }

    private void loadSprites() {

        try {
            // Ignore this. Proof of concept for re-scaling the window if I want to eventually allow minimizing the console
//            Dimension dim = parent.getMinimumSize();
//            dim.setSize(dim.width*1.1, dim.height*1.3);
//            parent.setMinimumSize(dim);
            info("Loading sprites from disk...");
            String selectedSprite = spriteList.getSelectedValue();
            String selectedVariant = variantList.getSelectedValue();
            sprites.clear();
            spriteList.clear();
            variantList.clear();

            // First try to copy sprites folder to disk, so peeps can use their own private sprites
            // (This should only be needed if running from a jar, as in-IDE, the resources should be properly copied to the target dir
            try {
                Path exportPath = Utils.exportResources("sprites", ".");
                if (null != exportPath) {
                    spritesPath = exportPath.normalize().toAbsolutePath();
                }
            } catch (Exception ex) {
                error("Failed to write base sprites to disk", ex);
            }

            if (null == spritesPath) spritesPath = Utils.getFolderPath(spritesDirName);
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
                            for (Path spritesheet : spritesheets) {
                                String filename = spritesheet.getFileName().toString();
                                if (filename.toLowerCase().endsWith("-mask.png")) {
                                    spritesheetMasks.put(filename.toLowerCase().replace("-mask.png", ""), ImageIO.read(new File(spritesheet.toUri())));
                                } else if (filename.toLowerCase().endsWith(".png")) {
                                    spritesheetImages.put(filename.toLowerCase().replace(".png", ""), ImageIO.read(new File(spritesheet.toUri())));
                                }
                            }
                            newVariant.addImages(spritesheetImages);
                            newVariant.addMasks(spritesheetMasks);

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

    private Component prefSize(Component component, int w, int h) {
        component.setPreferredSize(new Dimension(w, h));
        return component;
    }




    private String path() {
        return installDirBox.getText() + File.separator + graphicsPath;
    }

    private HashMap<String, BufferedImage> generateImages(Variant variant) {
        HashMap<String, BufferedImage> images = new HashMap<>();

        for (String key : variant.spritesheetImages.keySet()) {
            try {
                if (key.equalsIgnoreCase(THUMBNAIL_NAME)) {
                    continue;
                }
                images.put(key, generateImage(variant, key));
            } catch (Exception ex) {
                error("Failed to generate image", ex);
            }
        }

        return images;
    }

    private BufferedImage generateImage(Variant variant, String key) throws IOException {
        BufferedImage newImage = ImageIO.read(new File(path() + File.separator + key + extension));
        if (freshStart.isSelected()) {
            newImage = copyImage(sprites.get(spriteList.getSelectedValue()).variants.get("DEFAULT").spritesheetImages.get(key));
        }
        BufferedImage replacement = variant.spritesheetImages.get(key);
        BufferedImage mask = variant.spritesheetMasks.get(key);

        Color transparent = new Color(0,0,0,0);

        // If there's no mask, delete everything
        if (null == mask) {
            newImage = replacement;
        }
        else {
            for (int y = 0; y < replacement.getHeight(); y++) {
                for (int x = 0; x < replacement.getWidth(); x++) {
                    Color replacementColor = new Color(replacement.getRGB(x, y), true);
                    int replacementAlpha = replacementColor.getAlpha();
                    // Replace the pixel if it's not totally transparent
                    if (replacementAlpha != 0) {
                        newImage.setRGB(x, y, replacement.getRGB(x, y));
                    }
                    // If it was totally transparent, check if this is something that should be left alone
                    // Delete areas where the mask colour is non-black
                    else if (new Color(mask.getRGB(x, y)).getBlue() != 0) {
                        newImage.setRGB(x, y, transparent.getRGB());
                    }
                }
            }
        }
        return newImage;
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

            HashMap<String, BufferedImage> images = generateImages(
                    sprites.get(spriteList.getSelectedValue())
                            .variants.get(variantList.getSelectedValue()));
            for (String key : images.keySet()) {
                try {
                    if (key.equalsIgnoreCase(THUMBNAIL_NAME)) {
                        System.out.println("Skipping thumbnail");
                        continue;
                    }
                    ImageIO.write(images.get(key), "png", new File(path() + File.separator + key + extension));
                } catch (Exception ex) {
                    error("Failed to write a file. If `Fresh start` is unchecked, does the file exist in your game's graphics directory?", ex);
                }
            }

            info("That save probably worked!");
        }
        catch (Exception ex) {
            error("Save failed :(", ex);
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