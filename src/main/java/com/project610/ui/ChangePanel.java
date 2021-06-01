package com.project610.ui;

import com.project610.MainPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import static com.project610.Utils.prefSize;

public class ChangePanel extends JPanel {
    MainPanel mainPanel;
    public JLabel spriteLabel;
    public JLabel variantLabel;
    public JCheckBox freshStartBox;
    public JCheckBox shuffleColorBox;
    public JCheckBox chaosShuffleBox;

    public ChangePanel(MainPanel mainPanel, String spriteName, String variantName, boolean freshStart, boolean shuffleColor, boolean chaosShuffle) {
        this.mainPanel = mainPanel;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JButton deleteButton = new JButton(mainPanel.icons.get("lil-x"));
        deleteButton.setBorderPainted(false);
        deleteButton.setBorder(new EmptyBorder(7, 7, 7,7));
        deleteButton.setContentAreaFilled(false);
        deleteButton.addActionListener(e -> mainPanel.removeChange(this));
        add(deleteButton);

        JButton upButton = new JButton(mainPanel.icons.get("up"));
        upButton.setBorderPainted(false);
        upButton.setBorder(new EmptyBorder(7, 2, 7,2));
        upButton.setContentAreaFilled(false);
        upButton.addActionListener(e -> mainPanel.moveChange(this, -1, upButton));
        add(upButton);

        JButton downButton = new JButton(mainPanel.icons.get("down"));
        downButton.setBorderPainted(false);
        downButton.setBorder(new EmptyBorder(7, 2, 7,2));
        downButton.setContentAreaFilled(false);
        downButton.addActionListener(e -> mainPanel.moveChange(this, 1, downButton));
        add(downButton);

        //setLayout(new FlowLayout(FlowLayout.LEFT));
        spriteLabel = new JLabel(spriteName);
        add(spriteLabel);

        add(new JLabel(" / "));

        variantLabel = new JLabel(variantName);
        add(variantLabel);

        add(Box.createHorizontalGlue());

        freshStartBox = new JCheckBox("", freshStart);
        freshStartBox.setBackground(null);
        freshStartBox.setMargin(new Insets(0,0,0,0));
        add(freshStartBox);
        add(Box.createRigidArea(new Dimension(10,20)));

        shuffleColorBox = new JCheckBox("", shuffleColor);
        shuffleColorBox.setBackground(null);
        shuffleColorBox.setMargin(new Insets(0,0,0,0));
        add(prefSize(shuffleColorBox, 20, 20));

        add(Box.createRigidArea(new Dimension(10,20)));

        chaosShuffleBox = new JCheckBox("", chaosShuffle);
        chaosShuffleBox.setBackground(null);
        chaosShuffleBox.setMargin(new Insets(0,0,0,0));
        add(prefSize(chaosShuffleBox, 20, 20));

        setVisible(true);
    }
}
