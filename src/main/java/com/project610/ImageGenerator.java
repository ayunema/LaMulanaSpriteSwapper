package com.project610;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

public class ImageGenerator implements Runnable {

    MainPanel mainPanel;

    public ImageGenerator(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public void run() {
        mainPanel.blockUI();

        HashMap<String, BufferedImage> images = mainPanel.generateImagesForVariant(
                mainPanel.sprites.get(mainPanel.spriteList.getSelectedValue())
                        .variants.get(mainPanel.variantList.getSelectedValue()));

        int writeCount = 0;
        for (String key : images.keySet()) {
            try {
                if (key.equalsIgnoreCase(mainPanel.THUMBNAIL_NAME)) {
                    continue;
                }
                mainPanel.info("Saving image: " + mainPanel.path() + File.separator + key + mainPanel.extension);
                ImageIO.write(images.get(key), "png", new File(mainPanel.path() + File.separator + key + mainPanel.extension));
                writeCount++;
            } catch (Exception ex) {
                mainPanel.error("Failed to write a file. If `Fresh start` is unchecked, does the file exist in your game's graphics directory?", ex);
            }
        }
        if (writeCount > 0) {
            mainPanel.info("That save probably worked! Modified " + writeCount + " files");
        } else {
            mainPanel.info("That didn't seem to make any changes...");
        }

        mainPanel.unblockUI();
    }
}
