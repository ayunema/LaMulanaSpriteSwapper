package com.project610;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.TreeMap;

public class Variant {
    public String name;
    public TreeMap<String, BufferedImage> spritesheetImages;
    public TreeMap<String, BufferedImage> spritesheetMasks;
    public TreeMap<String, BufferedImage> spritesheetColorMasks;
    public TreeMap<String, Path> sfx;
    public TreeMap<String, Path> music;
    public String info = "";

    public Variant setName (String s) {
        this.name = s;
        return this;
    }

    public Variant addImages(TreeMap<String, BufferedImage> spritesheetImages) {
        this.spritesheetImages = spritesheetImages;
        return this;
    }

    public Variant addMasks(TreeMap<String, BufferedImage> spritesheetMasks) {
        this.spritesheetMasks = spritesheetMasks;
        return this;
    }

    public Variant addColorMasks(TreeMap<String, BufferedImage> spritesheetColorMasks) {
        this.spritesheetColorMasks = spritesheetColorMasks;
        return this;
    }

    public Variant addSfx(TreeMap<String, Path> sfx) {
        this.sfx = sfx;
        return this;
    }

    public Variant addMusic(TreeMap<String, Path> music) {
        this.music = music;
        return this;
    }

    public Variant setInfo (String s) {
        this.info = s;
        return this;
    }
}
