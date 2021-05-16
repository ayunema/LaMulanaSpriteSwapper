package com.project610;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public class Variant {
    public String name;
    public HashMap<String, BufferedImage> spritesheetImages;
    public HashMap<String, BufferedImage> spritesheetMasks;

    public Variant setName (String s) {
        this.name = s;
        return this;
    }

    public Variant addImages(HashMap<String, BufferedImage> spritesheetImages) {
        this.spritesheetImages = spritesheetImages;
        return this;
    }

    public Variant addMasks(HashMap<String, BufferedImage> spritesheetMasks) {
        this.spritesheetMasks = spritesheetMasks;
        return this;
    }
}
