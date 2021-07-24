package com.project610;

import javax.swing.*;
import java.awt.*;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * I am become spaghetti, destroyer of readability
 */
public class Utils {
    final public static String SLASH = File.separator;

    public static Path exportResources(String source, String dest) throws Exception {
        Path toReturn = null;
        URI uri = Utils.class.getClassLoader().getResource(source).toURI();
        if ("jar".equals(uri.getScheme())) {
            LinkedHashMap<Path, Boolean> fileTree = getStructure(Files.walk(getFolderPath(source)).toArray(x -> new Path[x]));
            for (Path path : fileTree.keySet()) {
                if (!fileTree.get(path)) {
                    if (!Files.exists(Paths.get(dest+File.separator+path))) {
                        Path result = Files.copy(path, Paths.get(dest + File.separator + path));
                    }
                } else {
                    Path result = Files.createDirectories(Paths.get(dest + File.separator + path.toString()));
                    if (null == toReturn) { toReturn = result; }
                }
            }
        }
        return toReturn;
    }

    // Folders in resources are treated as extensionless files, with their contained filenames listed in the file contents.
    // This hackily sorts that mess out
    private static LinkedHashMap<Path, Boolean> getStructure(Path[] paths) throws Exception {
        LinkedHashMap<Path, Boolean> files = new LinkedHashMap<>();
        Path lastPath = null;
        for (Path path : paths) {
            files.put(path, true);
            if (null != lastPath) {
                files.put(lastPath, isDir(lastPath, path));
            }
            lastPath = path;
        }
        // One last hurrah for the final file
        if (null != lastPath) {
            files.put(lastPath, isDir(lastPath, null));
        }

        return files;
    }

    // My variable naming is dumb, but returns whether the lastPath is a directory, not currentPath, since it needs context
    private static boolean isDir(Path lastPath, Path currentPath) {
        // If compared path is "lastpath/<something>" then lastPath must be a directory
        // Apparently the path to resources uses / as a separator, even on Windows. Who knew?
        if (null != currentPath && currentPath.normalize().toString().contains(lastPath.normalize().toString() + '/')) {
            return true;
        } else {
            // No data and no '.' in a filename: Assume empty folder
            if (!lastPath.getFileName().toString().contains(".") /* && file length == 0*/) {
                return true;
            }
        }
        return false;
    }

    public static Path getFolderPath(String folderName) throws URISyntaxException, IOException {
        URI uri = Utils.class.getClassLoader().getResource(folderName).toURI();
        if ("jar".equals(uri.getScheme())) {
            try {
                return FileSystems.newFileSystem(uri, Collections.emptyMap(), null).getPath(folderName);
            } catch (FileSystemAlreadyExistsException ex) {
                return FileSystems.getFileSystem(uri).getPath(folderName);
            }
        } else {
            return Paths.get(uri);
        }
    }

    /**
     * Tweaked after ripping from thezerothcat's LM Rando
     * https://github.com/thezerothcat/LaMulanaRandomizer/blob/master/src/main/java/lmr/randomizer/Settings.java
     */
    public static String guessInstallLocation(MainPanel main) {
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

                , "/home/gaming/.PlayOnLinux/wineprefix/GOG_LaMulana/drive_c/GOG Games/La-Mulana/"
        )) {
            try {
                if (new File(filename).exists()) {
                    laMulanaBaseDir = filename;
                    break;
                }
            } catch (Exception ex) {
                main.error("Something broke up while trying to find the game directory", ex);
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

        return laMulanaBaseDir;
    }

    // Lazy UI stuff, will eventually obsolete this crap
    public static Component prefSize(Component component, int w, int h) {
        component.setPreferredSize(new Dimension(w, h));
        return component;
    }

    public static Component minSize(Component component, int w, int h) {
        component.setMinimumSize(new Dimension(w, h));
        return component;
    }

    public static Component maxSize(Component component, int w, int h) {
        component.setMaximumSize(new Dimension(w, h));
        return component;
    }

    public static void closeThing(Closeable s) {
        try {
            if (null != s) s.close();
        } catch (Exception ex) {
            // Nyeh!
        }
    }

    public static JPanel hbox() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        return panel;
    }

    public static JPanel vbox() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
    }


}
