package com.project610.runnables;

import com.project610.MainPanel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.project610.Utils.closeThing;

public class SpriteDownloader implements Runnable {

    MainPanel mainPanel;

    public SpriteDownloader(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public void run() {
        mainPanel.blockUI();

        final String ERASABLE_ZIP_PATH = "LaMulanaSpriteSwapper-main/";
        final String tempDir = "tmp";
        final String zipPath = tempDir + File.separator + "lmss-main.zip";

        ReadableByteChannel rbc = null;
        FileOutputStream zipFileOutputStream = null;
        FileInputStream zipFileInputStream = null;
        ZipInputStream zipInputStream = null;

        try {
            mainPanel.info("Downloading sprites from github: https://github.com/Virus610/LaMulanaSpriteSwapper");
            URL url = new URL("https://github.com/Virus610/LaMulanaSpriteSwapper/archive/refs/heads/main.zip");
            rbc = Channels.newChannel(url.openStream());

            // Download LMSS repo zip to temp folder
            Files.createDirectories(Paths.get("tmp"));
            File zipFile = new File(zipPath);
            zipFileOutputStream = new FileOutputStream(zipFile);
            zipFileOutputStream.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            mainPanel.info("  ... Downloaded zip file! Extracting the important bits");

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
                            mainPanel.info("    ...... Extracted: " + outputPath);
                        }
                        catch (FileAlreadyExistsException ex) {
                            // IDGAF right now
                        }
                        catch (Exception ex) {
                            mainPanel.error("Failed to extract sprite file from zip", ex);
                        }
                    }
                }
            }
            mainPanel.info("  ... Extraction complete!");
        } catch (Exception ex) {
            mainPanel.error("Failed to download sprites from github", ex);
        } finally {
            closeThing(rbc);
            closeThing(zipFileOutputStream);
            closeThing(zipFileInputStream);
            closeThing(zipInputStream);

            try {
                Files.deleteIfExists(Paths.get(zipPath));
            } catch (Exception ex) {
                mainPanel.error("Failed to clean up downloaded zip containing sprites.", ex);
            }
        }

        mainPanel.loadSprites();
        mainPanel.unblockUI();
    }
}
