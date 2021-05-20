package com.project610;

import java.util.ArrayList;
import java.util.TreeMap;

public class Sprite {
    public String label;
    public ArrayList<String> files;
    public TreeMap<String, Variant> variants = new TreeMap<>();

    public Sprite setLabel(String s) {
        this.label = s;
        return this;
    }

    public Sprite addVariant (String name, Variant v) {
        variants.put(name, v);
        return this;
    }
}
