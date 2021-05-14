package com.project610;

import javax.swing.*;
import java.awt.*;

public class SpriteSwapper {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Look and feel broke, probably falling back on the garbo L&F");
            ex.printStackTrace();
        }
        JFrame jf = new JFrame("La-Mulana Sprite Swapper");
        jf.setSize(100, 100);
        jf.setMinimumSize(new Dimension(710, 591));
        MainPanel mainPanel = new MainPanel(args);
        mainPanel.setBackground(new Color(230, 230,230));
        jf.setContentPane(mainPanel);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //jf.setResizable(false);
        jf.setVisible(true);
    }
}
