package com.project610.ui;

import com.project610.MainPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DownloadPanel extends JPanel {
    MainPanel mainPanel;
    JLabel label = new JLabel();
    public boolean folderExists = true;

    public DownloadPanel(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        label.setBorder(new EmptyBorder(2, 6, 2, 2));
        setLabel("");
        add(label);

        JButton downloadYesButton = new JButton("Yes", mainPanel.icons.get("check"));
        downloadYesButton.setMargin(new Insets(20,25,20,25));
        downloadYesButton.setMinimumSize(new Dimension (800, 50));
        downloadYesButton.addActionListener(e -> {
            mainPanel.cancelLoading = true;
            mainPanel.downloadSprites();
            SwingUtilities.windowForComponent(this).setVisible(false);
        });
        add(downloadYesButton);

        JButton downloadNoButton = new JButton("No", mainPanel.icons.get("x"));
        downloadNoButton.setMargin(new Insets(20,10,20,10));
        downloadNoButton.addActionListener(e -> {
            if (!folderExists) {
                mainPanel.info("You need a `sprites` folder in the same directory as the Sprite Swapper jar file to use this app.\n  You can download it automatically by clicking 'Reload sprites' (top of Sprites panel),\n  or pull it from the repo manually, here:\n    https://github.com/Virus610/LaMulanaSpriteSwapper ");
            } else {
                mainPanel.info("If you decide you want me to check for downloads later... Good luck with that.\n          There isn't any button for that yet!");
            }
            SwingUtilities.windowForComponent(this).setVisible(false);
        });
        add(downloadNoButton);

        setVisible(true);
    }

    public void setLabel(String s) {
        label.setText("<html>" + s + "</html>");
    }
}
