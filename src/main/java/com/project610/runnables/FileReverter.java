package com.project610.runnables;

import com.project610.MainPanel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileReverter implements Runnable {
    private MainPanel mainPanel;
    String revertType = "";
    List<String> supportedTypes = Arrays.asList("sfx", "music");

    public FileReverter(MainPanel mainPanel, String revertType) {
        this.mainPanel = mainPanel;
        this.revertType = revertType;
    }

    public void run() {
        if (!supportedTypes.contains(revertType.toLowerCase())) {
            mainPanel.warn("Tried to revert data of unsupported type: " + revertType);
            return;
        }

        String extension = "";
        String dir = "";
        if (revertType.equalsIgnoreCase("sfx")) {
            extension = ".wav";
            dir = mainPanel.sfxPath();
        } else if (revertType.equalsIgnoreCase("music")) {
            extension = ".ogg";
            dir = mainPanel.musicPath();
        }

        int reverts = 0;
        try {
            for (Object o : (Files.list(Paths.get(dir)).toArray())) {
                Path path = (Path)o;
                if (path.toString().endsWith("_BAK")) {
                    String toRevert = path.toString().substring(0, path.toString().length()-4);
                    mainPanel.info("Backup found for file, attempting to revert: " + toRevert);
                    if (Files.exists(Paths.get(toRevert))) {
                        mainPanel.debug("Original located: " + toRevert);
                        try {
                            Files.move(Paths.get(toRevert), Paths.get(toRevert + "_DELETE"), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception ex) {
                            mainPanel.error("Failed to earmark modified file for deletion: " + toRevert, ex);
                            continue;
                        }

                        try {
                            Files.move(path, Paths.get(toRevert));
                        } catch (Exception ex) {
                            mainPanel.error("Failed to rename backup of file: " + path.toString() + ", trying to restore file to earlier state", ex);
                            Files.move(Paths.get(toRevert + "_DELETE"), Paths.get(toRevert), StandardCopyOption.REPLACE_EXISTING);
                            continue;
                        }

                        reverts++;

                        try {
                            Files.delete(Paths.get(toRevert+"_DELETE"));
                        } catch (Exception ex) {
                            mainPanel.warn("Failed to delete earmarked file: " + toRevert + "_DELETE, but revert should have worked.");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            mainPanel.error("Failed to revert files of type: " + revertType, ex);
        }

        if (reverts == 0) {
            mainPanel.info("No " + revertType + " files reverted");
        } else {
            mainPanel.info("Reverted: " + reverts + " " + revertType + " files");
        }
    }
}
