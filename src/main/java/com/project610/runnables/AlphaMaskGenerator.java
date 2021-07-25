package com.project610.runnables;

import com.project610.MainPanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.project610.Utils.SLASH;

public class AlphaMaskGenerator implements Runnable {

    MainPanel mainPanel;
    public Path[] paths = new Path[0];

    public AlphaMaskGenerator(MainPanel mainPanel) { this.mainPanel = mainPanel; }

    public void run() {
        mainPanel.blockUI();

        int count = 0;
        mainPanel.info("Generating " + paths.length + " alpha-masks...");

        for (Path path : paths) {
            count++;
            mainPanel.info(" > " + count + "/" + paths.length + ": Generating alpha-mask for: " + path.toString());

            if (path.getParent().normalize().toString().endsWith(mainPanel.defaultSpritesPath.normalize().toString())) {
                mainPanel.warn("   > Yo, I don't think you should be messing with .EVERYTHING/DEFAULT. Maybe make a copy or something? Giving up!");
                continue;
            }

            String justFilename = path.toString().substring(path.toString().lastIndexOf(SLASH) + 1);
            String extension = justFilename.substring(justFilename.lastIndexOf('.'));
            justFilename = justFilename.substring(0, justFilename.lastIndexOf('.'));

            Path defaultFilePath = Paths.get(mainPanel.defaultSpritesPath + SLASH + justFilename + extension);
            if (!Files.exists(defaultFilePath)) {
                mainPanel.warn("   > Couldn't find `.EVERYTHING/DEFAULT` equivalent at: " + defaultFilePath.toString() + "; Giving up!");
                continue;
            } else if (justFilename.toLowerCase().endsWith("-mask") || justFilename.toLowerCase().endsWith("-colormask")) {
                mainPanel.warn("   > Doesn't really make sense to make a mask of a mask... Giving up!");
                continue;
            }

            Path defaultMaskPath = Paths.get(mainPanel.defaultSpritesPath + SLASH + justFilename + "-MASK.png");

            if (Files.exists(defaultMaskPath)) {
                mainPanel.info("   > Found mask for original file!");
                try {
                    BufferedImage newSprite = ImageIO.read(new File(path.toUri()));
                    BufferedImage newMask = new BufferedImage(newSprite.getWidth(), newSprite.getHeight(), newSprite.getType());
                    BufferedImage defaultSprite = ImageIO.read(new File(defaultFilePath.toUri()));
                    BufferedImage defaultMask = ImageIO.read(new File(defaultMaskPath.toUri()));

                    // Scan image for mismatch to original, if any mismatch is found, mask the whole color-region based on the default mask
                    for (int y = 0; y < newMask.getHeight(); y++) {
                        for (int x = 0; x < newMask.getWidth(); x++) {
                            // Default to black
                            if (new Color(newMask.getRGB(x, y), true).getAlpha() == 0) {
                                newMask.setRGB(x, y, mainPanel.blackRgb);
                            }

                            // Don't bother if it's transparent. That's kinda the whole point of all this
                            if (new Color(newSprite.getRGB(x, y), true).getAlpha() == 0) {
                                continue;
                            }

                            if (newSprite.getRGB(x, y) != defaultSprite.getRGB(x, y)) {
                                if (newMask.getRGB(x, y) == mainPanel.blackRgb) {
                                    // Match found! Scan default mask for other pixels sharing the mask colour, mark them, and neutralize image in memory for efficiency and stuff
                                    int defaultMaskRGB = defaultMask.getRGB(x, y);
                                    // But only if there's no mask there, otherwise probably just do nothing
                                    if (new Color(defaultMaskRGB, true).getAlpha() != 0) {
                                        // Hate that I have to start from y2=0, but it's possible things will get skipped due to transparency early on
                                        for (int y2 = 0; y2 < newSprite.getHeight(); y2++) {
                                            for (int x2 = 0; x2 < newSprite.getWidth(); x2++) {
                                                if (defaultMask.getRGB(x2, y2) == defaultMaskRGB) {
                                                    newMask.setRGB(x2, y2, mainPanel.whiteRgb);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Do a once-over to see if anything can be scrapped from the original
                    boolean suggestNewSpritesheet = false;
                    for (int y = 0; y < newSprite.getHeight(); y++) {
                        for (int x = 0; x < newSprite.getWidth(); x++) {
                            // If the new sprite matches the original thing, there's not much reason to have it, so propose removing that bit and just not masking it
                            //  (Probably gonna have issues with overlays on this one)
                            if (new Color(newSprite.getRGB(x, y),true).getAlpha() != 0 && newMask.getRGB(x, y) == mainPanel.blackRgb && newSprite.getRGB(x, y) == defaultSprite.getRGB(x, y)) {
                                newSprite.setRGB(x, y, mainPanel.transparentRgb);
                                suggestNewSpritesheet = true;
                            }
                        }
                    }
                    // When all is said and done, save the thing
                    String outFilename = path.getParent().toString() + SLASH + justFilename + "-MASK";
                    String suffix = "";
                    int n = 0;

                    //String temp = outFilename + "_" + n + extension;
                    while (Files.exists(Paths.get(outFilename + suffix + extension))) {
                        n++;
                        suffix = "_" + n;
                    }
                    outFilename = outFilename + suffix + extension;

                    ImageIO.write(newMask, "png", new File(outFilename));
                    mainPanel.info("Saved new mask to: " + outFilename);
                    if (n > 0) {
                        mainPanel.info("    You may want to replace the existing mask with this one");
                    }
                    if (suggestNewSpritesheet) {
                        outFilename = path.getParent().toString() + SLASH + justFilename + "-optimized.png";
                        ImageIO.write(newSprite, "png", new File(outFilename));
                        mainPanel.info("There were unchanged sprites detected. Consider replacing your spritesheet with this optimized one, and regenerating an alpha mask: " + outFilename);
                    }

                } catch (Exception ex) {
                    mainPanel.error("Messed up generating alpha mask", ex);
                }
            } else {
                mainPanel.warn(" > No mask found for original file. "/* This is gonna be janky... When it's supported at all... */+ "Giving up!");
                continue;
            }
        }

        paths = new Path[0];
        mainPanel.unblockUI();
    }
}
