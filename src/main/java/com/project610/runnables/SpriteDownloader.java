package com.project610.runnables;

import com.project610.MainPanel;
import com.project610.ui.OverwriteItemPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.project610.Utils.closeThing;
import static com.project610.Utils.prefSize;
import static java.awt.SystemColor.info;

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
            mainPanel.info("Downloading sprites/presets from github: https://github.com/Virus610/LaMulanaSpriteSwapper");
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

            FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(zipPath), null);

            TreeMap<String, Path> mismatches = new TreeMap<>();

            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().contains("/sprites/") || entry.getName().toLowerCase().contains("/presets/")) {
                    String outputPath = entry.getName().replace(ERASABLE_ZIP_PATH, "");
                    if (entry.isDirectory()) {
                        Files.createDirectories(Paths.get(outputPath));
                    } else {
                        //FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(zipPath), null);
                        try {
                            Files.copy(fileSystem.getPath(entry.getName()), Paths.get(outputPath));
                            mainPanel.info("    ...... Extracted: " + outputPath);
                        }
                        catch (FileAlreadyExistsException ex) {
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            md.update(Files.readAllBytes(fileSystem.getPath(entry.getName())));
                            byte[] digest = md.digest();
                            String zipChecksum = DatatypeConverter.printHexBinary(digest).toUpperCase();

                            md = MessageDigest.getInstance("MD5");
                            md.update(Files.readAllBytes(Paths.get(outputPath)));
                            digest = md.digest();
                            String localChecksum = DatatypeConverter.printHexBinary(digest).toUpperCase();

                            if (zipChecksum.equalsIgnoreCase(localChecksum)) {
                                // No need to do do anything
                                mainPanel.debug("Checksum match, ignoring: " + outputPath);
                            } else {
                                mismatches.put(outputPath, fileSystem.getPath(entry.getName()));
                                mainPanel.debug("Checksum mismatch! Stashing: " + outputPath);
                            }
                        }
                        catch (Exception ex) {
                            mainPanel.error("Failed to extract sprite file from zip", ex);
                        }
                    }
                }
            }

            if (mismatches.size() > 0) {
                JDialog overwriteDialog = new JDialog(mainPanel.parent, "Updated files detected, overwrite?");

                JPanel overwritePanel = new JPanel();
                overwritePanel.setLayout(new BoxLayout(overwritePanel, BoxLayout.PAGE_AXIS));
                overwriteDialog.setContentPane(overwritePanel);

                JPanel overwriteListPanel = new JPanel();
                overwriteListPanel.setLayout(new BoxLayout(overwriteListPanel, BoxLayout.PAGE_AXIS));

                ArrayList<OverwriteItemPanel> itemList = new ArrayList<>();

                JPanel selectPanel = new JPanel();
                selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.LINE_AXIS));

                JButton selectNoneButton = new JButton("Select none");
                selectNoneButton.addActionListener(e -> {
                    for (OverwriteItemPanel panel : itemList) {
                        panel.overwriteBox.setSelected(false);
                    }
                });
                selectPanel.add(selectNoneButton);

                JButton selectAllButton = new JButton("Select all");
                selectAllButton.addActionListener(e -> {
                    for (OverwriteItemPanel panel : itemList) {
                        panel.overwriteBox.setSelected(true);
                    }
                });
                selectPanel.add(selectAllButton);
                overwritePanel.add(selectPanel);

                JScrollPane overwriteScroll = new JScrollPane(overwriteListPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                overwritePanel.add(prefSize(overwriteScroll, 300, 230));

                for (String key : mismatches.keySet()) {
                    mainPanel.warn("Checksum mismatch for: " + key);
                    OverwriteItemPanel itemPanel = new OverwriteItemPanel(key);
                    itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                    overwriteListPanel.add(prefSize(itemPanel, 300, 20));
                    itemList.add(itemPanel);
                }

                JButton okButton = new JButton("OK");
                okButton.addActionListener(e -> {
                    for (OverwriteItemPanel item : itemList) {
                        if (item.overwriteBox.isSelected()) {
                            try {
                                Files.copy(mismatches.get(item.filename), Paths.get(item.filename), StandardCopyOption.REPLACE_EXISTING);
                                mainPanel.info("    ...... Overwriting: " + item.filename);
                            } catch (Exception ex) {
                                mainPanel.error("Failed to overwrite file: " + item.filename, ex);
                            }
                        }
                    }
                    overwriteDialog.dispose();
                });
                overwritePanel.add(okButton);

                overwriteDialog.setSize(300, 250);
                overwriteDialog.setLocation(mainPanel.getLocationOnScreen().x + 50, mainPanel.getLocationOnScreen().y + 50);
                overwriteDialog.setModal(true);
                overwriteDialog.setVisible(true);
            }
            mainPanel.info("  ... Extraction complete!");
        } catch (Exception ex) {
            mainPanel.error("Failed to download sprites/presets from github", ex);
        } finally {
            closeThing(rbc);
            closeThing(zipFileOutputStream);
            closeThing(zipFileInputStream);
            closeThing(zipInputStream);

            try {
                Files.deleteIfExists(Paths.get(zipPath));
            } catch (Exception ex) {
                mainPanel.error("Failed to clean up downloaded zip containing sprites/presets.", ex);
            }
        }

        mainPanel.loadSprites();
        mainPanel.unblockUI();
    }
}
