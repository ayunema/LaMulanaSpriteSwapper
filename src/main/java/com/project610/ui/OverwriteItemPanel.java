package com.project610.ui;

import com.project610.Utils;

import javax.swing.*;
import java.awt.*;

public class OverwriteItemPanel extends JPanel {
    public String filename;

    public JCheckBox overwriteBox;

    public OverwriteItemPanel(String filename) {
        this.filename = filename;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        overwriteBox = new JCheckBox(filename);
        add (overwriteBox);

        add(Box.createRigidArea(new Dimension(250,20)));
    }
}
