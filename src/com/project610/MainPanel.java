package com.project610;

import com.project610.utils.JList2;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainPanel extends JPanel {

    JTextField installDirBox;
    JList2<String> spriteList;
    JList2<String> variantList;
    JCheckBox freshStart;
    JLabel imageView;

    String graphicsPath = "data\\graphics\\00";
    String cwd = "sprites";
    String extension = ".png";

    HashMap<String,Sprite> sprites = new HashMap<>();

    public MainPanel(String[] args) {
        init();

        if (installDirBox.getText().trim().isEmpty()) {
            guessInstallLocation();
        }

        reload();
    }

    public void init() {
        removeAll();

        // basically a vbox
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setLayout(null);


        JPanel topPane = new JPanel();
        //topPane.setLayout(new BoxLayout(topPane, BoxLayout.X_AXIS));
        topPane.setLayout(null);

        //topPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        //topPane.setBackground(new Color(1f, 0f, 0f));
        topPane.setSize(700, 30);
        add(topPane);

        topPane.add(place(new JLabel("La-Mulana install directory"), 5, 5, 135, 20));

        installDirBox = new JTextField(15);
        topPane.add(place(installDirBox, 145, 5, 400, 20));




        JPanel midPane = new JPanel(null);
        midPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));

        //midPane.setLayout(new BoxLayout(midPane, BoxLayout.X_AXIS));
        //midPane.setBackground(new Color(0f, 1f, 0f));
        add(place(midPane, 0, 30, 700, 300));

        JPanel spriteListPane = new JPanel(null);
        spriteListPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        //firstListPane.setSize(50,300);
        //firstListPane.setBackground(new Color(0.5f, 0.5f, 0.5f));
        midPane.add(place(spriteListPane, 0, 0, 170, 290));

        spriteListPane.add(place(new JLabel("Sprite"), 10, 2, 150, 20));
        spriteList = new JList2<>();
        spriteList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        spriteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spriteListPane.add(place(spriteList, 4, 25, 160, 260));
        
        spriteList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("spriteList Click on " + spriteList.getSelectedValue() + ": " + e.getFirstIndex() + "/" + e.getLastIndex());
                variantList.clear();
                variantList.addAll(sprites.get(spriteList.getSelectedValue()).variants.keySet());
            }
        });


        JPanel variantListPane = new JPanel(null);
        variantListPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        //secondListPane.setBackground(new Color(0.25f, 0.25f, 0.25f));
        midPane.add(place(variantListPane, 175, 0, 170, 290));

        variantListPane.add(place(new JLabel("Variant"), 10, 2, 150, 20));
        variantList = new JList2<>();
        variantList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        variantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variantListPane.add(place(variantList, 4, 25, 160, 260));

        variantList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) return;

                Sprite currentSprite = sprites.get(spriteList.getSelectedValue());
                //Variant currentVariant = currentSprite.variants.get(secondList.getSelectionModel().getSelectedItem());
                Variant currentVariant = currentSprite.variants.get(variantList.getSelectedValue());

                //Image image = new Image(cwd + File.separator + firstList.getSelectionModel().getSelectedItem());
                try {
                    // Grab name of first spritesheet (for now)
                    String spritesheetName = "thumbnail";
                    BufferedImage thumbnail;
                    try {
                        thumbnail = currentVariant.spritesheetImages.get("thumbnail");
                        thumbnail.getRGB(0,0);
                    } catch (Exception ex) {
                        for (String key : currentVariant.spritesheetImages.keySet()) {
                            spritesheetName = key;
                            break;
                        }
                    }

                    BufferedImage newImage = null;
                    if (freshStart.isSelected()) {
                        newImage = copyImage(sprites.get(spriteList.getSelectedValue()).variants.get("DEFAULT").spritesheetImages.get(spritesheetName));
                    } else {
                        newImage = ImageIO.read(new File(path() + File.separator + spritesheetName + ".png"));
                    }
                    BufferedImage replacement = currentVariant.spritesheetImages.get(spritesheetName);
                    BufferedImage mask = currentVariant.spritesheetMasks.get(spritesheetName);

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
                                // Leave this pixel alone
                                else {
                                }
                            }
                        }
                    }

                    AffineTransform transform = new AffineTransform();
                    double scale = 1;
                    scale = Math.min(scale, (double)imageView.getWidth()/newImage.getWidth());
                    scale = Math.min(scale, (double)imageView.getHeight()/newImage.getHeight());
                    transform.scale(scale, scale);
                    AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
                    BufferedImage scaledImage = new BufferedImage((int)(newImage.getWidth()*scale),(int)(newImage.getHeight()*scale), BufferedImage.TYPE_INT_ARGB);
                    scaledImage = transformOp.filter(newImage, scaledImage);
                    imageView.setIcon(new ImageIcon(scaledImage));

                } catch (Exception ex) {
                    System.err.println("Failed to display image");
                    ex.printStackTrace();
                }
            }
        });

        JPanel previewPane = new JPanel();
        previewPane.setLayout(null);
        midPane.add(place(previewPane, 350, 0, 350, 300));

        freshStart = new JCheckBox("Fresh start", true);
        previewPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        previewPane.add(place(freshStart, 5, 5, 80, 20));

        JButton applyButton = new JButton("Apply");
        previewPane.add(place(applyButton, 90, 5, 70, 22));
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });

        JButton refreshButton = new JButton("Refresh sprite files");
        previewPane.add(place(refreshButton, 205, 5, 130, 22));
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });

        imageView = new JLabel();
        previewPane.add(place(imageView, 0, 30, 300, 270));

        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.X_AXIS));
        //bottomPane.setBackground(new Color(0f,0f,0.6f));
        add(bottomPane);
        bottomPane.add(new JButton("mooooooooooooooooooooooooo"));
        bottomPane.add(new JButton("mooooooooooooooooooooooooo"));

        //GridBagConstraints constraints = new GridBagConstraints(5, 10, 25, 8, 1.0, 0.25, GridBagConstraints.HORIZONTAL, 2, new Insets(5, 10, 25, 30), 40, 69);
        //layout.setConstraints(panel1, constraints);

        //panel2.add(butt);



        //add(mainPanel);
    }

    /*public void paintComponent(Graphics g) {
        try {
            g.drawImage(imageView, 300, 80, 600, 380, null);
        } catch (Exception ex) {

        }
    }*/



    public void reload() {
        sprites.clear();
        spriteList.clear();
        variantList.clear();

        Path thisDir = Paths.get(cwd);
        Path[] spriteFiles = listFiles(thisDir);

        /*imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());*/

        // Get all sprite options
        for (Path spriteFile : spriteFiles) {
            String spriteName = spriteFile.getFileName().toString();
            Sprite newSprite = new Sprite().setLabel(spriteName);

            // Get variations of sprite
            try {
                Path[] variants = listFiles(spriteFile);
                for (Path variant : variants) {
                    String variantName = variant.getFileName().toString();
                    Variant newVariant = new Variant().setName(variantName);

                    // Get images of variation
                    try {
                        Path[] spritesheets = listFiles(variant);
                        HashMap<String, BufferedImage> spritesheetImages = new HashMap<>();
                        HashMap<String,BufferedImage> spritesheetMasks = new HashMap<>();
                        for (Path spritesheet : spritesheets) {
                            String filename = spritesheet.getFileName().toString();
                            if (filename.toLowerCase().endsWith("-mask.png")) {
                                spritesheetMasks.put(filename.toLowerCase().replace("-mask.png", ""), ImageIO.read(new File(spritesheet.toUri())));
                            } else if (filename.toLowerCase().endsWith(".png")){
                                spritesheetImages.put(filename.toLowerCase().replace(".png", ""), ImageIO.read(new File(spritesheet.toUri())));
                            }
                        }
                        newVariant.addImages(spritesheetImages);
                        newVariant.addMasks(spritesheetMasks);

                        newSprite.addVariant(newVariant.name, newVariant);

                        sprites.put(newSprite.label, newSprite);
                    } catch (Exception ex) {
                        System.err.println("Guh...");
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                // heck
                System.err.println("Sprite messed up somehow");
                ex.printStackTrace();
            }
        }

        for (String key : sprites.keySet()) {
            spriteList.add(key);
        }
    }

    public Component place(Component component, int x, int y, int w, int h) {
        component.setBounds(x, y, w, h);
        return component;
    }


    /**
     * Tweaked after ripping from thezerothcat's LM Rando
     * https://github.com/thezerothcat/LaMulanaRandomizer/blob/master/src/main/java/lmr/randomizer/Settings.java
     */
    public void guessInstallLocation() {
        String laMulanaBaseDir = "";

        for (String filename : Arrays.asList(
                  "C:\\Games\\La-Mulana Remake 1.3.3.1"
                , "C:\\GOG Games\\La-Mulana"
                , "C:\\GOG Games\\La-Mulana"
                , "C:\\Steam\\steamapps\\common\\La-Mulana"
                , "C:\\Program Files (x86)\\Steam\\steamapps\\common\\La-Mulana"
                , "C:\\Program Files\\Steam\\steamapps\\common\\La-Mulana"
                , "C:\\Program Files (x86)\\GOG Galaxy\\Games\\La Mulana"
                , "C:\\Program Files (x86)\\GOG.com\\La-Mulana"

                , "D:\\Games\\La-Mulana Remake 1.3.3.1"
                , "D:\\GOG Games\\La-Mulana"
                , "D:\\GOG Games\\La-Mulana"
                , "D:\\Steam\\steamapps\\common\\La-Mulana"
                , "D:\\Program Files (x86)\\Steam\\steamapps\\common\\La-Mulana"
                , "D:\\Program Files\\Steam\\steamapps\\common\\La-Mulana"
                , "D:\\Program Files (x86)\\GOG Galaxy\\Games\\La Mulana"
                , "D:\\Program Files (x86)\\GOG.com\\La-Mulana"

                , "$HOME/.steam/steam/steamapps/common/La-Mulana"
                , "$HOME/.local/share/Steam/steamapps/common/La-Mulana"
                , "~/.var/app/com.valvesoftware.Steam/data/Steam/steamapps/common/La-Mulana"
        )) {
            if (new File(filename).exists()) {
                laMulanaBaseDir = filename;
                break;
            }
        }

        if (laMulanaBaseDir.isEmpty()) {
            try {
                // Try to find the GOG game on Linux
                // Also honor file system hierachy (local installs supersede global installs)
                for (String menu_entry_file_path : Arrays.asList(
                          "/usr/share/applications/gog_com-La_Mulana_1.desktop"
                        , "/usr/local/share/applications/gog_com-La_Mulana_1.desktop"
                        , System.getProperty("user.home") + "/.local/share/applications/gog_com-La_Mulana_1.desktop"
                        , System.getProperty("user.home") + "/Desktop/gog_com-La_Mulana_1.desktop"
                        /* other valid paths for the .desktop file to be located? */)) {

                    File menu_entry_file = new File(menu_entry_file_path);
                    if (!menu_entry_file.exists()) {
                        continue; // Try next item if file doesn't exist
                    }

                    List<String> menu_file_lines = Files.readAllLines(menu_entry_file.toPath());
                    menu_file_lines.removeIf(l -> !l.startsWith("Path="));

                    if (menu_file_lines.size() != 1) {
                        continue; // File is malformed, there should be exactly one "Path=..." line
                    }

                    laMulanaBaseDir = menu_file_lines.get(0).substring(5);
                }

                // The GOG version has some fluff around the *actual* game install, moving it into the
                // "game" subdirectory. If it exists, then just use that, otherwise the rcdReader won't
                // be able to find the necessary files!
                File dir = new File(laMulanaBaseDir, "game");
                if (dir.exists() && dir.isDirectory()) {
                    laMulanaBaseDir += "/game";
                }
            } catch (Exception e) {
                // do nothing
            }
        }


        if (!laMulanaBaseDir.isEmpty()) {
            installDirBox.setText(laMulanaBaseDir);
        }
    }

    public String path() {
        return installDirBox.getText() + File.separator + graphicsPath;
    }

    public HashMap<String, BufferedImage> generateImages(Variant variant) {
        HashMap<String, BufferedImage> images = new HashMap<>();

        for (String key : variant.spritesheetImages.keySet()) {
            try {
                images.put(key, generateImage(variant, key));
            } catch (Exception ex) {
                System.err.println("Failed to generate image");
                ex.printStackTrace();
            }
        }

        return images;
    }

    public BufferedImage generateImage(Variant variant, String key) throws IOException {
        BufferedImage newImage = ImageIO.read(new File(path() + File.separator + key + extension));
        /*if (freshStart.isSelected()) {
            newImage = copyImage(sprites.get(firstList.getSelectionModel().getSelectedItem()).variants.get("DEFAULT").spritesheetImages.get(key));
        }*/
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
                    // Leave this pixel alone
                    else {
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
            System.err.println("Fffff- Failed to list files in: " + path);
            ex.printStackTrace();
        }
        return null;
    }

    public BufferedImage copyImage(BufferedImage source){
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    public void save() {
        if (installDirBox.getText().trim().length() == 0 || !Files.exists(Paths.get(path()))) {
            installDirBox.setBackground(new Color(1.0f, 0.7f, 0.7f));
            return;
        } else {
            installDirBox.setBackground(new Color(1.0f, 1.0f, 1.0f));
        }

        HashMap<String, BufferedImage> images = generateImages(
                sprites.get(spriteList.getSelectedValue())
                        .variants.get(variantList.getSelectedValue()));
        for (String key : images.keySet()) {
            try {
                ImageIO.write(images.get(key), "png", new File(path() + File.separator + key + extension));
            } catch (Exception ex) {
                System.err.println("Failed to write a file");
                ex.printStackTrace();
            }
        }


    }
}
