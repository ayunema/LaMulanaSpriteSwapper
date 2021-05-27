package com.project610.ui;

import com.project610.MainPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DownloadPanel extends JPanel {
    MainPanel mainPanel;
    public DownloadPanel(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        setLayout(new BorderLayout());
        JLabel label = new JLabel();
        label.setBorder(new EmptyBorder(2, 6, 2, 2));
        label.setText("<html>      No sprites folder detected.<br/> Download sprites from the app repository?</html>");
        add(label, BorderLayout.LINE_START);

        //                 How much you wanna bet these emojis don't work on Linux or something?
        JButton downloadYesButton = new JButton("✅ Yes");
        downloadYesButton.setMargin(new Insets(2,4,2,4));
        downloadYesButton.addActionListener(e -> {
            mainPanel.downloadSprites();
            SwingUtilities.windowForComponent(this).setVisible(false);
        });
        add(downloadYesButton, BorderLayout.CENTER);

        JButton downloadNoButton = new JButton("❌ No");
        downloadNoButton.setMargin(new Insets(2,4,2,4));
        downloadNoButton.addActionListener(e -> {
            mainPanel.info("You need a `sprites` folder in the same directory as the Sprite Swapper jar file to use this app.\n  You can download it automatically by clicking 'Refresh sprite files' (top right),\n  or pull it from the repo manually, here:\n    https://github.com/Virus610/LaMulanaSpriteSwapper ");
            SwingUtilities.windowForComponent(this).setVisible(false);
        });
        add(downloadNoButton, BorderLayout.LINE_END);
        setVisible(true);
    }
}
