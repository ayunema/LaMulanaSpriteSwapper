package com.project610.runnables;

import com.project610.MainPanel;
import com.project610.Sprite;
import com.project610.Utils;
import com.project610.Variant;
import com.project610.ui.ChangePanel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.TreeMap;

public class ImageGenerator implements Runnable {

    MainPanel mainPanel;

    public ImageGenerator(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public void run() {
        mainPanel.blockUI();

        for (ChangePanel changePanel : mainPanel.changesList) {
            Sprite sprite = mainPanel.sprites.get(changePanel.spriteLabel.getText());
            Variant variant = sprite.variants.get(changePanel.variantLabel.getText());
            TreeMap<String, BufferedImage> images = mainPanel.generateImagesForVariant(
                sprite, variant
                , changePanel.freshStartBox.isSelected()
                , changePanel.shuffleColorBox.isSelected()
                , changePanel.chaosShuffleBox.isSelected()
            );

            int writeCount = 0;
            for (String key : images.keySet()) {
                try {
                    if (key.equalsIgnoreCase(mainPanel.THUMBNAIL_NAME)) {
                        continue;
                    }
                    mainPanel.info("Saving image: " + mainPanel.gfxPath() + File.separator + key + mainPanel.extension);
                    ImageIO.write(images.get(key), "png", new File(mainPanel.gfxPath() + File.separator + key + mainPanel.extension));
                    writeCount++;
                } catch (Exception ex) {
                    mainPanel.error("Failed to write a file. If `Fresh start` is unchecked, does the file exist in your game's graphics directory?", ex);
                }
            }

            // SFX
            for (String key : variant.sfx.keySet()) {
                String filename = mainPanel.sfxPath() + Utils.SLASH + key + ".wav";
                if (!Files.exists(Paths.get(filename))) {
                    mainPanel.warn("Tried to save SFX, but couldn't find file to replace: " + filename);
                    continue;
                }
                if (!Files.exists(Paths.get(filename+"_BAK"))) {
                    try {
                        Files.copy(Paths.get(filename), Paths.get(filename + "_BAK"));
                    } catch (Exception ex) {
                        mainPanel.error("Failed to make backup of base SFX file: " + filename, ex);
                        continue;
                    }
                }
                try {
                    mainPanel.info("Saving SFX: " + filename);
                    Files.copy(variant.sfx.get(key), Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);
                    writeCount++;
                } catch (Exception ex) {
                    mainPanel.error("Failed to copy SFX file to game directory: " + filename, ex);
                }
            }

            // Music
            for (String key : variant.music.keySet()) {
                String filename = mainPanel.musicPath() + Utils.SLASH + key + ".ogg";
                if (!Files.exists(Paths.get(filename))) {
                    mainPanel.warn("Tried to save music, but couldn't find file to replace: " + filename);
                    continue;
                }
                if (!Files.exists(Paths.get(filename+"_BAK"))) {
                    try {
                        Files.copy(Paths.get(filename), Paths.get(filename + "_BAK"));
                    } catch (Exception ex) {
                        mainPanel.error("Failed to make backup of base music file: " + filename, ex);
                        continue;
                    }
                }
                try {
                    mainPanel.info("Saving Music: " + filename);
                    Files.copy(variant.music.get(key), Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);
                    writeCount++;
                } catch (Exception ex) {
                    mainPanel.error("Failed to copy music file to game directory: " + filename, ex);
                }
            }

            if (writeCount > 0) {
                mainPanel.info("That save probably worked! Modified " + writeCount + " files");
            } else {
                mainPanel.info("That didn't seem to make any changes...");
            }
        }

        mainPanel.info("----- Done -----\n");

        mainPanel.unblockUI();
    }
}
