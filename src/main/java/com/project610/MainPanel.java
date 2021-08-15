package com.project610;

import com.project610.runnables.AlphaMaskGenerator;
import com.project610.runnables.FileReverter;
import com.project610.runnables.ImageGenerator;
import com.project610.runnables.SpriteDownloader;
import com.project610.structs.JList2;
import com.project610.ui.ChangePanel;
import com.project610.ui.DownloadPanel;
import com.project610.ui.VariantInfoPanel;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import static com.project610.Utils.*;

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

    public final int transparentRgb = new Color(0f,0f,0f,0f).getRGB();
    public final int blackRgb = new Color(0f,0f,0f).getRGB();
    public final int whiteRgb = new Color(1f,1f,1f).getRGB();

    private JTextField installDirBox;
    public JList2<String> spriteList;
    public JList2<String> variantList;
    //public JList2<JPanel> changesList;
    public ArrayList<ChangePanel> changesList;
    private JCheckBox freshStartBox;
    private JCheckBox shuffleColorsBox;
    private JCheckBox chaosShuffleBox;
    private JLabel imageView;
    private JTextArea console;
    private JScrollPane consoleScroll;
    private JDialog downloadDialog;
    private DownloadPanel downloadPanel;
    private JButton addButton;
    JPanel changesPanel;
    public HashMap<String, Icon> icons;

    private String graphicsPath = "data" + SLASH + "graphics" + SLASH + "00";
    private String sfxPath = "data" + SLASH + "sound";
    private String musicPath = "data" + SLASH + "music" + SLASH + "00";
    private String spritesDirName = "sprites";
    private String presetsDirName = "presets";
    private Path spritesPath = Paths.get("sprites");
    public Path defaultSpritesPath = Paths.get(spritesPath + SLASH + ".EVERYTHING" + SLASH + "DEFAULT");
    public String extension = ".png";
    public final String THUMBNAIL_NAME = "thumbnail";
    private float fontSize = 11f;
    public Random rand = new Random();
    private final Path settingsPath = Paths.get("lmss-settings.json");

    ImageGenerator imageGenerator = new ImageGenerator(this);
    AlphaMaskGenerator alphaMaskGenerator = new AlphaMaskGenerator(this);
    SpriteDownloader spriteDownloader = new SpriteDownloader(this);

    private boolean debug = false;
    private boolean inIDE = true;
    public String appVersion;
    public boolean newVersion = false;
    private List<String> properties = new ArrayList<>();
    private JSONObject settings;
    public boolean skipDownloadPrompt = false;
    public boolean alreadyLoading = false;
    public boolean cancelLoading = false;
    public boolean loadingPresets = false;

    private int LOG_LEVEL = 6; // 3 err, 4 warn, *6 info*, 7 debug

    public TreeMap<String,Sprite> sprites = new TreeMap<>();
    public HashMap<Color, Float[]> adjustments = new HashMap<>(); // Remember to reset this when changing between variants/spritesheets!
    public TreeMap<String, Preset> presets = new TreeMap<>();
    public JComboBox<String> presetBox;
    private JButton variantInfoButton;

    private JMenuBar menuBar;

    public JFrame parent;
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
            try {
                installDirBox.setText(settings.getString("installLocation"));
            } catch (Exception ex) {
                installDirBox.setText(Utils.guessInstallLocation(this));
            }
        }

    }

    private void init() {
        removeAll();
        blockables = new HashSet<>();

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        if (debug) setBackground(new Color (0.3f, 0.3f, 0.3f));

        ToolTipManager.sharedInstance().setInitialDelay(0);

        // Read settings stored to disk
        try {
            if (Files.exists(settingsPath)) {
                settings = new JSONObject(new String(Files.readAllBytes(settingsPath)));
            } else {
                settings = new JSONObject();
                writeSettings();

            }
        } catch (Exception ex) {
            error("Failed to read settings from disk", ex);
        }

        // Read settings from memory, compare to settings on disk
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(".properties");

            //properties = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource(".properties").toURI()));
            BufferedReader br = new BufferedReader (new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("version=")) {
                    appVersion = line.substring(line.indexOf("=") + 1);
                    if (!settings.has("version") || !settings.getString("version").equalsIgnoreCase(appVersion)) {
                        settings.put("version", appVersion);
                        writeSettings();
                        newVersion = true;
                    }
                    parent.setTitle(parent.getTitle() + " v" + appVersion);
                }
            }
        } catch (Exception ex) {
            // Not using console because DNE yet
            System.err.println("Failed to read app properties");
            ex.printStackTrace();
        }

        icons = new HashMap<>();
        try {
            icons.put("freshstart", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/freshstart.png"))));
            icons.put("shufflecolors", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/shufflecolors.png"))));
            icons.put("chaosshuffle", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/chaosshuffle.png"))));
            icons.put("check", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/check.png"))));
            icons.put("x", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/x.png"))));
            icons.put("lil-x", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/lil-x.png"))));
            icons.put("up", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/up.png"))));
            icons.put("down", new ImageIcon(ImageIO.read(getClass().getClassLoader().getResource("icons/down.png"))));
        } catch (Exception ex) {
            error("Failed to load icons from resources", ex);
        }

        changesPanel = vbox();

        menuBar = new JMenuBar();
        add(menuBar);
        menuBar.setLayout(new BoxLayout(menuBar, BoxLayout.LINE_AXIS));

        // Stuff menu
        JMenu stuffMenu = new JMenu("Stuff");
        menuBar.add(stuffMenu);
        blockables.add(stuffMenu);

        JMenuItem downloadSpritesButton = new JMenuItem("Check online for new sprites");
        downloadSpritesButton.addActionListener(e -> downloadSprites());
        stuffMenu.add(downloadSpritesButton);

        JMenuItem generateAlphaMaskButton = new JMenuItem("Generate Alpha Mask from spritesheet");
        generateAlphaMaskButton.addActionListener(e -> generateAlphaMask());
        stuffMenu.add(generateAlphaMaskButton);

        // SFX/Music menu
        JMenu sfxMusicMenu = new JMenu("SFX/Music");
        menuBar.add(sfxMusicMenu);
        blockables.add(sfxMusicMenu);

        JMenuItem revertSfxButton = new JMenuItem("Revert to backed-up SFX");
        revertSfxButton.addActionListener(e -> new Thread(new FileReverter(this, "sfx")).run());
        sfxMusicMenu.add(revertSfxButton);

        JMenuItem revertMusicButton = new JMenuItem("Revert to backed-up Music");
        revertMusicButton.addActionListener(e -> new Thread(new FileReverter(this, "music")).run());
        sfxMusicMenu.add(revertMusicButton);


        menuBar.add(Box.createHorizontalGlue());

        JPanel topPane = new JPanel();
        if (debug) topPane.setBackground(new Color(1f, 0f, 0f));
        topPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(prefSize(topPane, 700, 30));

        topPane.add(prefSize(new JLabel("La-Mulana install directory"), 155, 20));

        installDirBox = new JTextField();
        blockables.add(installDirBox);
        topPane.add(prefSize(installDirBox, 350, 20));

        /////////// This is really kinda bad, and I should probably not do it over just fixing the root of the Manjaro giant-text issue
        topPane.add(new JLabel("Text size:"));

        JButton textSizeDownButton = new JButton("-");
        textSizeDownButton.setMargin(new Insets(0,0,0,0));
        textSizeDownButton.addActionListener(e -> {
            changeFont(this, --fontSize);
            settings.put("fontSize", fontSize);
            writeSettings();
        });
        topPane.add(prefSize(textSizeDownButton, 20, 20));

        JButton textSizeUpButton = new JButton("+");
        textSizeUpButton.setMargin(new Insets(0,0,0,0));
        textSizeUpButton.addActionListener(e -> {
            changeFont(this, ++fontSize);
            settings.put("fontSize", fontSize);
            writeSettings();
        });
        topPane.add(prefSize(textSizeUpButton, 20, 20));

        JPanel midPane = hbox();
        if (debug) midPane.setBackground(new Color(1f, 1f, 0f));
        midPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));

        //midPane.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(prefSize(midPane, 700, 300));

        JPanel spriteListPane = new JPanel(new BorderLayout(5, 5));
        //JPanel spriteListPane = vbox();
        if (debug) spriteListPane.setBackground(new Color(0.3f, 0.3f, 0.6f));

        spriteListPane.setBorder(new CompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, 0, 1, Color.lightGray)
                , BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        midPane.add(prefSize(spriteListPane, 170, 300));

        JPanel spriteListTop = hbox();
        spriteListPane.add(spriteListTop, BorderLayout.PAGE_START);

        spriteListTop.add(new JLabel(" Sprite"));

        spriteListTop.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Reload sprites");
        blockables.add(refreshButton);
        refreshButton.addActionListener(e -> loadSprites());
        spriteListTop.add(refreshButton);

        spriteList = new JList2<>();
        blockables.add(spriteList);
        spriteList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        spriteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spriteListPane.add(spriteList, BorderLayout.CENTER);

        spriteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) return;

            variantList.clear();
            variantList.addAll(sprites.get(spriteList.getSelectedValue()).variants.keySet());
            variantInfoButton.setEnabled(false);
        });

        JPanel variantListPane = new JPanel(new BorderLayout(5,5));
        if (debug) variantListPane.setBackground(new Color(0.4f, 0.4f, 0.8f));

        JPanel variantListTop = hbox();
        variantListPane.add(variantListTop, BorderLayout.PAGE_START);

        variantListPane.setBorder(new CompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, 0, 1, Color.lightGray)
                , BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        midPane.add(prefSize(variantListPane, 170, 300));

        variantListTop.add(new JLabel(" Variant"));
        variantInfoButton = new JButton ("Info");
        variantInfoButton.setEnabled(false);
        variantInfoButton.addActionListener(e -> showVariantInfo(currentVariant()));

        variantListTop.add(Box.createHorizontalGlue());

        variantListTop.add(variantInfoButton);





        variantList = new JList2<>();
        blockables.add(variantList);
        variantList.setBorder(BorderFactory.createLineBorder(Color.lightGray));
        variantList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        variantListPane.add(prefSize(variantList, 159, 260), BorderLayout.CENTER);

        variantList.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    newChange();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        variantList.addListSelectionListener(e -> {
            if (null == variantList.getSelectedValue()) {
                addButton.setEnabled(false);
            } else {
                addButton.setEnabled(true);
                variantInfoButton.setEnabled(!currentVariant().info.trim().isEmpty());
            }

            if (!e.getValueIsAdjusting()) return;

            Sprite currentSprite = sprites.get(spriteList.getSelectedValue());
            Variant currentVariant = currentSprite.variants.get(variantList.getSelectedValue());

            newAdjustments(chaosShuffleBox.isSelected());

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
                /*if (spritesheetName.equalsIgnoreCase(THUMBNAIL_NAME)) {
                        newImage = currentVariant.spritesheetImages.get(spritesheetName);
                }
                else {
                    newImage = generateImageForVariant(currentVariant, spritesheetName);
                }*/

                // Scale image preview to fit imageView in previewPane
                /*AffineTransform transform = new AffineTransform();
                double scale = 2;
                scale = Math.min(scale, (double)(imageView.getWidth() - 10)/newImage.getWidth());
                scale = Math.min(scale, (double)(imageView.getHeight()- 20)/newImage.getHeight());
                transform.scale(scale, scale);
                AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
                BufferedImage scaledImage = new BufferedImage((int)(newImage.getWidth()*scale),(int)(newImage.getHeight()*scale), BufferedImage.TYPE_INT_ARGB);

                scaledImage = transformOp.filter(newImage, scaledImage);
                imageView.setIcon(new ImageIcon(scaledImage));*/

            } catch (Exception ex) {
                error("Failed to display image", ex);
            }
        });

        JPanel previewPane = new JPanel();
        if (debug) previewPane.setBackground(new Color(0.6f, 0.6f, 1.0f));
        previewPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        midPane.add(previewPane);
        midPane.add(prefSize(previewPane, 350, 300));

        JLabel freshStartLabel = new JLabel(icons.get("freshstart"));
        previewPane.add(freshStartLabel);
        freshStartBox = new JCheckBox("Fresh start", true);
        freshStartBox.setMargin(new Insets(0, 0, 0, 0));
        blockables.add(freshStartBox);
        freshStartBox.setToolTipText("Avoid overwriting previous changes, when possible");
        previewPane.add(freshStartBox);

        previewPane.add(Box.createRigidArea(new Dimension(2, 1)));

        JLabel shuffleColorsLabel = new JLabel(icons.get("shufflecolors"));
        previewPane.add(shuffleColorsLabel);
        shuffleColorsBox = new JCheckBox("Shuffle colors", false);
        shuffleColorsBox.setMargin(new Insets(0, 0, 0, 0));
        blockables.add(shuffleColorsBox);
        shuffleColorsBox.setToolTipText("Randomize colors if the variant has a color mask (eg: `00prof-COLORMASK.png`), just hue-shift if it does not");
        previewPane.add(shuffleColorsBox);

        previewPane.add(Box.createRigidArea(new Dimension(2, 1)));

        JLabel chaosShuffleLabel = new JLabel(icons.get("chaosshuffle"));
        previewPane.add(chaosShuffleLabel);
        chaosShuffleBox = new JCheckBox("Chaos shuffle", false);
        chaosShuffleBox.setMargin(new Insets(0, 0, 0, 0));
        blockables.add(chaosShuffleBox);
        chaosShuffleBox.setToolTipText("Will NOT try to keep consistency between spritesheets in a variant. Also, try to ensure at least kind-of significant shuffling");
        previewPane.add(chaosShuffleBox);





        changesPanel.setBackground(new Color(1f, 1f, 1f));
        JScrollPane changeScroll = new JScrollPane(changesPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //changesList = new JList2<JPanel>();
        changesList = new ArrayList<>();


        addButton = new JButton("Add to queue");
        blockables.add(addButton);
        addButton.setEnabled(false);
        addButton.addActionListener(e -> newChange());
        previewPane.add(prefSize(addButton, 342, 20));

        JPanel changesLegendPanel = hbox();
        changesLegendPanel.add(new JLabel("  Sprite/Variant"));
        changesLegendPanel.add(Box.createHorizontalGlue());
        changesLegendPanel.add(new JLabel(icons.get("freshstart")));
        changesLegendPanel.add(Box.createRigidArea(new Dimension(14,20)));
        changesLegendPanel.add(new JLabel(icons.get("shufflecolors")));
        changesLegendPanel.add(Box.createRigidArea(new Dimension(14,20)));
        changesLegendPanel.add(new JLabel(icons.get("chaosshuffle")));
        previewPane.add(prefSize(changesLegendPanel, 315, 20));

        previewPane.add(prefSize(changeScroll, 340, 186));


        presetBox = new JComboBox<>();
        blockables.add(presetBox);
        presetBox.setEditable(true);
        presetBox.addActionListener(e -> {
            if (null != presetBox.getSelectedItem() && !loadingPresets) {
                loadPreset(presetBox.getSelectedItem().toString());
            }
        });

        previewPane.add(prefSize(presetBox, 160, 22));

        JButton savePresetButton = new JButton("Save preset");
        blockables.add(savePresetButton);
        savePresetButton.addActionListener(e -> savePreset((String)presetBox.getSelectedItem()));
        previewPane.add(prefSize(savePresetButton, 90, 22));

        //previewPane.add(Box.createRigidArea(new Dimension(264,20)));

        JButton applyButton = new JButton("Apply");
        blockables.add(applyButton);
        previewPane.add(prefSize(applyButton, 70, 22));
        applyButton.addActionListener(e -> save());









        /*JButton colorButton = new JButton("†");
        previewPane.add(prefSize(colorButton, 10, 10));
        colorButton.addActionListener(e -> {
            imageView.getIcon()
        });*/

        imageView = new JLabel();
        //previewPane.add(prefSize(imageView, 300, 270));

        JPanel bottomPane = new JPanel(new BorderLayout(5,5));
        if (debug) bottomPane.setBackground(new Color(0f, 1f, 0f));
        add(prefSize(bottomPane, 700, 220));
        bottomPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JPanel bottomPaneTop = hbox();
        bottomPane.add(bottomPaneTop, BorderLayout.PAGE_START);

        bottomPaneTop.add(prefSize(new JLabel("   Console"), 80, 20));
        JButton clearConsoleButton = new JButton("x");
        bottomPaneTop.add(clearConsoleButton);
        clearConsoleButton.addActionListener(e -> console.setText(""));

        console = new JTextArea();
        console.setLineWrap(true);
        console.setEditable(false);
        console.setFont(console.getFont().deriveFont(11f));
        consoleScroll = new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if (debug) consoleScroll.setBackground(new Color(1f,0f,1f));
        bottomPane.add(prefSize(consoleScroll, 680, 180));


        parent.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                settings.put("windowPos", parent.getLocationOnScreen().x + "," + parent.getLocationOnScreen().y);
                writeSettings();
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        try {
            String[] pos = settings.getString("windowPos").split(",");
            Point p = new Point(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]));
            parent.setLocation(p);
        } catch (Exception ex) {
            debug("No window position: " + ex);
        }

        try {
            fontSize = settings.getFloat("fontSize");
            changeFont(this, fontSize);
        } catch (Exception ex) {

        }

        downloadDialog = new JDialog(parent, "Download sprites?");
        downloadDialog.setSize(500,95);
        downloadDialog.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 200);
        downloadDialog.setModal(true);

        downloadPanel = new DownloadPanel(this);
        downloadDialog.add(downloadPanel);


        console.append(":)");
    }

    // Try to generate an alpha mask (-MASK.png) for a spritesheet based on the original graphics
    //  (eg: Block out regions based on masks I may or may not have even made yet) (TODO - WIP)
    private void generateAlphaMask() {
        Path[] paths = getPathsFromDialog();
        if (paths.length == 0) {
            warn ("No path selected, cancelling alpha-mask generation");
            return;
        }
        alphaMaskGenerator.paths = paths;
        new Thread(alphaMaskGenerator).start();
    }

    private Path[] getPathsFromDialog() {
        Path[] paths = null;
        JDialog dialog = new JDialog(parent, "Select file(s)");

        dialog.setSize((int) (parent.getWidth() / 1.25), (int) (parent.getHeight() / 1.25));
        dialog.setLocation(parent.getLocationOnScreen().x + 50, parent.getLocationOnScreen().y + 50);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        dialog.setContentPane(panel);

        String dir;
        try {
            dir = settings.getString("alphaMaskDir");
        } catch (Exception ex) {
            dir = spritesDirName;
        }

        JFileChooser chooser = new JFileChooser(dir);
        chooser.setMultiSelectionEnabled(true);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("PNG Images", "png"));

        chooser.addActionListener(e -> {
            if (e.getActionCommand().equalsIgnoreCase("CancelSelection")) {
                dialog.dispose();
            } else if (e.getActionCommand().equalsIgnoreCase("ApproveSelection")) {
                setAlphaMaskGenPaths(chooser.getSelectedFiles());
                dialog.dispose();
            }
            debug ("File Chooser: " + e.getActionCommand());
            if (null != chooser.getSelectedFile()) {
                debug ("   > " + chooser.getSelectedFile().toPath().toString());
            }
        });
        panel.add(chooser, BorderLayout.CENTER);

        dialog.setModal(true);
        dialog.setVisible(true);

        paths = alphaMaskGenPaths;
        if (paths.length > 0) {
            settings.put("alphaMaskDir", paths[0].getParent().toString());
        }
        alphaMaskGenPaths = new Path[0];

        return paths;
    }

    // Lazy hack 'cause apparently I can't set the value of a var outside of a lambda method
    public Path[] alphaMaskGenPaths = new Path[0];
    public void setAlphaMaskGenPaths(File[] files) {
        alphaMaskGenPaths = new Path[files.length];
        for (int i = 0; i < files.length; i++) {
            alphaMaskGenPaths[i] = files[i].toPath();
        }
    }

    public void showVariantInfo(Variant variant) {
        Variant currentVariant = currentVariant(); //                                      v---------- But why -----------v
        JDialog infoDialog = new JDialog(parent, "Variant info: " + currentSprite().label + " / " + currentVariant.name);
/*
        infoDialog.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                System.out.println(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                System.out.println(e);
            }
        });
*/
        infoDialog.setSize(500,150);
        infoDialog.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 200);
        infoDialog.setModal(true);
        infoDialog.add(new VariantInfoPanel(infoDialog, currentVariant));

        /*KeyboardFocusManager
                .getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    System.out.println(e);
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        try {
                            infoDialog.dispose();
                            return true;
                        } catch (Exception ex) {}
                    }
                    return false;
                });*/

        infoDialog.setVisible(true);

    }

    public Sprite currentSprite() {
        return sprites.get(spriteList.getSelectedValue());
    }

    public Variant currentVariant() {
        return currentSprite().variants.get(variantList.getSelectedValue());
    }

    public void savePreset(String name) {
        info("Saving preset with name: " + name);

        JSONObject json = new JSONObject();
        json.put("name", name);
        String filename = name;
        filename = filename.replaceAll("[^A-Za-z0-9\\-_]", "_") + ".json";

        JSONArray array = new JSONArray();


        ArrayList<PresetSprite> presetSprites = new ArrayList<>();
        for (ChangePanel change : changesList) {

            JSONObject preset = new JSONObject();

            preset.put("spriteName", change.spriteLabel.getText());
            preset.put("variantName", change.variantLabel.getText());
            preset.put("freshStart", change.freshStartBox.isSelected());
            preset.put("shuffleColors", change.shuffleColorBox.isSelected());
            preset.put("chaosShuffle", change.chaosShuffleBox.isSelected());

            array.put(preset);
        }

        json.put("presetSprites", array);
/*
        Preset preset = new Preset();
        preset.name = name;
        preset.presetSprites = presetSprites;

        presets.put(name, preset);*/

        try {
            Files.write(Paths.get("./presets/" + filename).normalize(), json.toString().getBytes());
        } catch (Exception ex) {
            error("heck", ex);
        }

        info("JSON: \n" + json.toString());


        loadPresets();
        presetBox.setSelectedItem(name);
    }

    public void loadPreset(String name) {
        if (name == null) {
            warn ("wtf yo");
            return;
        }

        if (null == presets.get(name)) {
            return;
        }

        clearChanges();

        if (name.equalsIgnoreCase(".EMPTY")) {
            return;
        }

        info("Loading preset: '" + name + "'");
        Preset preset = presets.get(name);


        for (PresetSprite sprite : preset.presetSprites) {


            String changeLabel = sprite.spriteName + "/" + sprite.variantName;

            // Don't add if already exists
            for (ChangePanel change : changesList) {
                String temp = change.spriteLabel.getText() + "/" + change.variantLabel.getText();
                if (changeLabel.equalsIgnoreCase(temp)) {
                    continue;
                }
            }
            ChangePanel newChange = new ChangePanel(this, sprite.spriteName, sprite.variantName, sprite.freshStart, sprite.shuffleColors, sprite.chaosShuffle);
            changesList.add(newChange);
            addChange(newChange);
            changesPanel.revalidate();
        }
    }

    public void loadPresets() {
        try {
            loadingPresets = true;

            presets.clear();
            presetBox.removeAllItems();

            for (Path path : listFiles(Paths.get("./presets"))) {
                Preset preset = new Preset();
                JSONObject json = new JSONObject(new String(Files.readAllBytes(path.normalize())));

                preset.name = json.getString("name");
                JSONArray array = json.getJSONArray("presetSprites");
                for (Object obj : array) {
                    JSONObject temp = new JSONObject(obj.toString());
                    PresetSprite sprite = new PresetSprite();
                    sprite.spriteName = temp.getString("spriteName");
                    sprite.variantName = temp.getString("variantName");
                    sprite.freshStart = temp.getBoolean("freshStart");
                    sprite.shuffleColors = temp.getBoolean("shuffleColors");
                    sprite.chaosShuffle = temp.getBoolean("chaosShuffle");
                    preset.presetSprites.add(sprite);
                }
                presets.put(preset.name, preset);
            }

            presets.put(".EMPTY", new Preset());

            for (String key : presets.keySet()) {
                presetBox.addItem(key);
            }
        } catch (Exception ex) {
            error("Screwed up loading presets :(", ex);
        }

        loadingPresets = false;
    }

    public void writeSettings() {
        try {
            // Write stuff
            Files.write(settingsPath, settings.toString().getBytes());
        } catch (Exception ex) {
            error("Failed to write settings to file", ex);
        }
    }

    public ChangePanel newChange() {
        String changeLabel = spriteList.getSelectedValue() + "/" + variantList.getSelectedValue();

        // Don't add if already exists
        for (ChangePanel change : changesList) {
            String temp = change.spriteLabel.getText() + "/" + change.variantLabel.getText();
            if (changeLabel.equalsIgnoreCase(temp)) {
                return null;
            }
        }
        ChangePanel newChange = new ChangePanel(this, spriteList.getSelectedValue(), variantList.getSelectedValue(), freshStartBox.isSelected(), shuffleColorsBox.isSelected(), chaosShuffleBox.isSelected());
        changesList.add(newChange);
        addChange(newChange);
        changesPanel.revalidate();

        return newChange;
    }

    public void addChange(ChangePanel newChange) {
        /*if (changesPanel.getComponents().length % 2 == 0) {
            newChange.setBackground(new Color(.87f, .87f, 1f));
        } else {
            newChange.setBackground(new Color(0.97f, 0.97f, 0.97f)); // #F7F7F7
        }*/
        changeFont(newChange, fontSize);
        changesPanel.add(prefSize(newChange, 280, 24));
        revalidateChangeList();
    }

    public void moveChange(ChangePanel change, int pos, JButton button) {
        int index = changesList.indexOf(change);
        int newIndex = index+pos;

        if (newIndex >= 0 && newIndex < changesList.size() && null != changesList.get(newIndex)) {
            changesList.set(index, changesList.get(newIndex));
            changesList.set(newIndex, change);

            changesPanel.removeAll();
            for (ChangePanel panel : changesList) {
                addChange(panel);
            }
            changesPanel.revalidate();

            try {
                Robot r = new Robot();
                Point point = MouseInfo.getPointerInfo().getLocation();
                r.mouseMove(point.x, point.y + (pos * change.getHeight()));
            } catch (Exception ex) {
                error("Failed to move mouse to new button location", ex);
            }
            button.grabFocus();
        } else  {
            warn("Can't move there");
        }
    }

    public void clearChanges() {
        while (changesList.size() > 0) {
            removeChange(changesList.get(0));
        }
    }

    public void removeChange(ChangePanel change) {
        change.setVisible(false);
        int pos = changesList.indexOf(change);
        changesList.remove(change);
        changesPanel.remove(change);
        //fixChangeList(pos);
        revalidateChangeList();
        changesPanel.revalidate();
    }

    public void fixChangeList(int pos) {
        for (int i = pos; i < changesList.size(); i++) {
            if (i %2 == 0) {
                changesList.get(i).setBackground(new Color(.87f, .87f, 1f));
            } else {
                changesList.get(i).setBackground(new Color(0.97f, 0.97f, 0.97f));
            }
        }
    }

    // This isn't hella efficient, but I'll deal with that if the scope ever gets big enough that it matters
    public void revalidateChangeList() {
        for (int i = 0; i < changesList.size(); i++) {
            changesList.get(i).upButton.setEnabled(i != 0);
            changesList.get(i).downButton.setEnabled(i != changesList.size()-1);
            if (i %2 == 0) {
                changesList.get(i).setBackground(new Color(.87f, .87f, 1f));
            } else {
                changesList.get(i).setBackground(new Color(0.97f, 0.97f, 0.97f));
            }
        }
    }

    private void changeFont(Container c, float size) {

        for (Component c2 : c.getComponents()) {
            c2.setFont(c2.getFont().deriveFont(size));
            if (c2 instanceof Container) {
                changeFont((Container)c2, size);
            }
        }
    }

    public void downloadSprites() {
        newVersion = false; // Probably don't need to prompt for new-version stuff if we just downloaded stuff
        new Thread(spriteDownloader).start();
    }

    private void downloadPopup(String s) {
        downloadPanel.setLabel(s);
        downloadDialog.setVisible(true);
        downloadDialog.toFront();
    }

    // Load sprites from disk (`sprites` folder in same dir as app)
    // If sprites folder doesn't exist, offer to download it for the user
    //  * Don't do any internet stuff without asking
    public void loadSprites() {
        if (alreadyLoading) {
            info("Already loading sprites, not gonna try loading again");
            return;
        }

        blockUI();
        try {
            // Ignore this. Proof of concept for re-scaling the window if I want to eventually allow minimizing the console
//            Dimension dim = parent.getMinimumSize();
//            dim.setSize(dim.width*1.1, dim.height*1.3);
//            parent.setMinimumSize(dim);

            if (!skipDownloadPrompt) {
                if (!Files.exists(Paths.get(spritesDirName))) {
                    downloadPanel.folderExists = false;
                    cancelLoading = true;
                    warn("No sprites folder located. Prompting to download from main repository.");
                    downloadPopup("      No sprites folder detected.<br/> Download sprites/presets from the app repository?");
                } else if (!Files.exists(Paths.get(presetsDirName))) {
                    downloadPanel.folderExists = false;
                    cancelLoading = true;
                    warn("No presets folder located. Prompting to download from main repository.");
                    downloadPopup("      No presets folder detected.<br/> Download sprites/presets from the app repository?");
                } else if (newVersion) {
                    newVersion = false;
                    alreadyLoading = true;
                    warn("App has been updated. Prompting to download from main repository.");
                    downloadPopup("      Looks like you updated this app recently.<br/> Want me to check for/download new sprites?");
                    alreadyLoading = false;
                }
            } else {
                skipDownloadPrompt = false;
            }

            if (cancelLoading) {
                cancelLoading = false;
                unblockUI();
                return;
            }

            info("Loading sprites from disk...");

            // If something is already selected, store the selected values for re-selection if/after reloading
            String selectedSprite = spriteList.getSelectedValue();
            String selectedVariant = variantList.getSelectedValue();
            sprites.clear();
            spriteList.clear();
            variantList.clear();

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
                            Path[] paths = listFiles(variant);
                            TreeMap<String, BufferedImage> spritesheetImages = new TreeMap<>();
                            TreeMap<String, BufferedImage> spritesheetMasks = new TreeMap<>();
                            TreeMap<String, BufferedImage> spritesheetColorMasks = new TreeMap<>();
                            TreeMap<String, Path> sfx = new TreeMap<>();
                            TreeMap<String, Path> music = new TreeMap<>();
                            for (Path path : paths) {
                                String filename = path.getFileName().toString();
                                if (filename.toLowerCase().endsWith("-mask.png")) {
                                    spritesheetMasks.put(filename.toLowerCase().replace("-mask.png", ""), ImageIO.read(new File(path.toUri())));
                                } else if (filename.toLowerCase().endsWith("-colormask.png")) {
                                    spritesheetColorMasks.put(filename.toLowerCase().replace("-colormask.png", ""), ImageIO.read(new File(path.toUri())));
                                } else if (filename.toLowerCase().endsWith(".png")) {
                                    spritesheetImages.put(filename.toLowerCase().replace(".png", ""), ImageIO.read(new File(path.toUri())));
                                } else if (filename.toLowerCase().endsWith(".txt")) {
                                    if (filename.equals("info.txt")) {
                                        newVariant.setInfo(new String(Files.readAllBytes(path)));
                                    }
                                } else if (filename.toLowerCase().endsWith(".wav")) {
                                    sfx.put(filename.toLowerCase().replace(".wav",""), path);
                                }else if (filename.toLowerCase().endsWith(".ogg")) {
                                    music.put(filename.toLowerCase().replace(".ogg",""), path);
                                }
                            }

                            newVariant.addImages(spritesheetImages);
                            newVariant.addMasks(spritesheetMasks);
                            newVariant.addColorMasks(spritesheetColorMasks);
                            newVariant.addSfx(sfx);
                            newVariant.addMusic(music);

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

            /* TODO: #20210601a
            if (checkForSprites("https://api.github.com/repos/Virus610/LaMulanaSpriteSwapper/contents/sprites?ref=main", 0, "sprites")) {
                warn("Found some (new?) sprite data online, prompting to download from main repository.");
                downloadDialog.setVisible(true);
                downloadDialog.toFront();
            }
            */

            info("Loading complete!");
        }
        catch (Exception ex) {
            error("loadSprites failed in a major way.", ex);
        }

        unblockUI();

        if (changesList.size() > 0) {
            info("Reload and reapply");
            save();
        }

        loadPresets();

    }



    // Thought I was being super cool. Turns out stuff is hella rate-limited by github without an auth token,
    //     and I simply don't feel like dealing with that nonsense right now.
    /* TODO: #20210601a
    public boolean checkForSprites(String spritesUrl, int level, String data) {
        InputStream is = null;
        BufferedReader br = null;
        try {
            HttpsURLConnection conn = (HttpsURLConnection)new URL(spritesUrl).openConnection();
            conn.setRequestProperty("User-Agent", "LaMulanaSpriteSwapper");
            //is = new URL(spritesUrl).openStream();
            String jsonString = "";
            br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String line;
            while ((line = br.readLine()) != null) {
                jsonString += line + "\n";
            }
            JSONArray array = new JSONArray(jsonString);

            Sprite sprite = null;
            Variant variant = null;
            if (level == 1) {
                sprite = sprites.get(data.substring(data.indexOf("/")+1));
            }
            else if (level == 2) {
                sprite = sprites.get(data.substring(data.indexOf("/")+1,data.lastIndexOf("/")));
                variant = sprite.variants.get(data.substring(data.lastIndexOf("/")+1));
            }

            for (Object item : array) {
                JSONObject jsonObject = new JSONObject(item.toString());

                String name = jsonObject.get("name").toString();
                String url = jsonObject.get("url").toString();
                String type = jsonObject.get("type").toString();

                if (level == 0) {
                    if (!sprites.keySet().contains(name)) {
                        warn("Detected missing Sprite: " + name);
                        return true;
                    }
                } else if (level == 1) {
                    if (!sprite.variants.keySet().contains(name)) {
                        warn("Detected missing Variant: " + name);
                        return true;
                    }
                } else if (level == 2) {
                    String simpleName = name.replaceAll("(?i)\\.png$", "");
                    if (simpleName.endsWith("-COLORMASK")) {
                        if (!variant.spritesheetColorMasks.keySet().contains(simpleName.replace("-COLORMASK", ""))) {
                            warn("Detected missing Color Mask: " + name);
                            return true;
                        }
                    } else if (simpleName.endsWith("-MASK")) {
                        if (!variant.spritesheetMasks.keySet().contains(simpleName.replace("-MASK", ""))) {
                            warn("Detected missing Mask: " + name);
                            return true;
                        }
                    } else if (!variant.spritesheetImages.keySet().contains(simpleName)) {
                        warn("Detected missing Spritesheet: " + name);
                        return true;
                    }
                }
                if (type.equalsIgnoreCase("dir")) {
                    if (checkForSprites(url, level+1, data + "/" + name)) {
                        return true;
                    }
                }
            }

        } catch (Exception ex) {
            error("Failed to check for new sprites", ex);
        } finally {
            closeThing(is);
            closeThing(br);
        }
        return false;
    }
    */

    public String gfxPath() {
        return installDirBox.getText() + SLASH + graphicsPath;
    }

    public String sfxPath() {
        return installDirBox.getText() + SLASH + sfxPath;
    }

    public String musicPath() {
        return installDirBox.getText() + SLASH + musicPath;
    }

    public TreeMap<String, BufferedImage> generateImagesForVariant(Sprite sprite, Variant variant, boolean freshStart, boolean shuffleColors, boolean chaosShuffle) {
        TreeMap<String, BufferedImage> images = new TreeMap<>();

        for (String key : variant.spritesheetImages.keySet()) {
            try {
                // Skip thumbnails when generating images - Those don't get copied to LM graphics folder
                if (key.equalsIgnoreCase(THUMBNAIL_NAME)) {
                    continue;
                }
                images.put(key, generateImageForVariant(sprite, variant, key, freshStart, shuffleColors, chaosShuffle));
            } catch (Exception ex) {
                error("Failed to generate image", ex);
            }
        }
        return images;
    }

    // Reset adjustments for color shuffling (New variant, or chaos + new spritesheet)
    public HashMap<Color, Float[]> newAdjustments(boolean chaosShuffle) { // chaosShuffle param is kinda vestigial at this point
        debug("New adjustments");
        adjustments = new HashMap<>();

        float h = rand.nextFloat();
        float s = rand.nextFloat()*0.36f-0.18f;
        float b = 1 + rand.nextFloat()*0.50f-0.25f;

        /*if (chaosShuffle) {
            h = rand.nextFloat() * 0.8f + 0.1f;
        }*/

        // Add null 'Color' for areas with no colorMask
        adjustments.put(
                null,
                new Float[]{h, s, b}
        );
        return adjustments;
    }

    private BufferedImage generateImageForVariant(Sprite sprite, Variant variant, String key, boolean freshStart, boolean shuffleColors, boolean chaosShuffle) {
        info("Generating image: " + gfxPath() + SLASH + key + extension);
        BufferedImage newImage = null;
        if (freshStart) {
            // TODO: It occurs to me that Fresh Start is kinda dumb, and needs reworking. Saving that for 0.8 or something
            //      Maybe just reference .EVERYTHING/DEFAULT instead?
            newImage = copyImage(sprite.variants.get("DEFAULT").spritesheetImages.get(key));
        } else {
            try {
                newImage = ImageIO.read(new File(gfxPath() + SLASH + key + extension));
            } catch (Exception ex) {
                error("Could not find/load base image at: " + gfxPath() + SLASH + key + extension, ex);
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
        if (null != mask || shuffleColors) {
            if (chaosShuffle) {
                newAdjustments(chaosShuffle);
            }
            int shuffleMods = 0;
            /*if (Arrays.asList("01effect", "fog00", "fog01").contains(key.toLowerCase())) {
                info("BRIGHTNESS ONLY DOWN for: " + key);
                shuffleMods = SHUFFLE_BRIGHTNESS_ONLY_DOWN;
            }*/
            for (int y = 0; y < replacement.getHeight(); y++) {
                for (int x = 0; x < replacement.getWidth(); x++) {
                    Color replacementColor = new Color(replacement.getRGB(x, y), true);
                    int replacementAlpha = replacementColor.getAlpha();
                    // Replace the pixel if it's not totally transparent
                    if (replacementAlpha != 0) {
                        newImage.setRGB(x, y, replacement.getRGB(x, y));
                        if (shuffleColors) {
                            newImage.setRGB(x, y, shuffleRGB(x, y, replacement, colorMask, shuffleMods, chaosShuffle));
                        }
                    }
                    // If it was totally transparent, check if this is something that should be left alone
                    // Delete areas where the mask color is non-black
                    else if (null != mask && new Color(mask.getRGB(x, y)).getBlue() != 0) {
                        newImage.setRGB(x, y, transparent.getRGB());
                    }
                }
            }
        }

        return newImage;
    }

    private int shuffleRGB(int x, int y, BufferedImage img, BufferedImage colorMask, int mods, boolean chaosShuffle) {
        Color pixel = new Color(img.getRGB(x, y),true);
        Color maskPixel = (null == colorMask) ? null : new Color(colorMask.getRGB(x, y), true);

        // Add new randomized adjustment if this maskPixel is a color not yet seen for this variant
        if (null == adjustments.get(maskPixel)) {
            float h = rand.nextFloat();
            float s = rand.nextFloat()*0.36f-0.18f;
            float b = 1 + rand.nextFloat()*0.50f-0.25f;

            // heck the mods for now
            /*
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
            */

            adjustments.put(maskPixel,
                    new Float[] {h, s, b});
        }

        // Preserve black pixels for some sanity
        if (img.getRGB(x,y) != blackRgb) {
            if (pixel.getRGB() != transparentRgb && (null == maskPixel || maskPixel.getRGB() != transparentRgb)) {
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
        hsb[0] += adjustment[0];
        hsb[1] += adjustment[1];
        hsb[2] *= adjustment[2];
        Color tempPixel = new Color(Color.HSBtoRGB(hsb[0], floatClamp(hsb[1]), floatClamp(hsb[2])));
        Color newPixel = new Color(tempPixel.getRed(), tempPixel.getGreen(), tempPixel.getBlue(), pixel.getAlpha());
        return newPixel.getRGB();
    }

    private float floatClamp(float f) {
        return Math.min(1, Math.max(0, f));
    }

    private Path[] listFiles(Path path) {
        Object[] files = new Object[0];
        try {
            files = Files.list(path).toArray();
            return Arrays.copyOf(files, files.length, Path[].class);
        }
        catch (Exception ex) {
            error("Fffff- Failed to list files in: " + path, ex);
        }
        return Arrays.copyOf(files, files.length, Path[].class);
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
            info("----------\n");
            info("Trying to save changes");

            newAdjustments(chaosShuffleBox.isSelected());

            if (installDirBox.getText().trim().length() == 0 || !Files.exists(Paths.get(gfxPath()))) {
                installDirBox.setBackground(new Color(1.0f, 0.7f, 0.7f));
                warn("Failed to locate graphics directory at: " + gfxPath());
                return;
            } else {
                installDirBox.setBackground(new Color(1.0f, 1.0f, 1.0f));
                settings.put("installLocation", installDirBox.getText());
                writeSettings();
            }

            if (changesList.size() == 0) {
                warn("Can't save without adding at least one Variant");
                return;
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

    public void debug(String s) {
        System.out.println(s);
        console("[DEBUG]  " + s, 7);
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
