package com.project610.ui;

import com.project610.MainPanel;
import com.project610.Variant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class VariantInfoPanel extends JPanel {
    JEditorPane jep;
    private final String DEFAULT_STYLE = "<style type=\"text/css\"> div { padding: 5px; font-family: arial; font-size: 13px; } </style>\n";

    public VariantInfoPanel(JDialog parent, Variant variant) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBackground(new Color(230, 230,230));


        jep = new JEditorPane();
        jep.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    parent.dispose();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) { }
        });

        jep.setMargin(new Insets(0,0,0,0));
        jep.setContentType("text/html");
        jep.setEditable(false);
        jep.setText(DEFAULT_STYLE + variant.info);
        jep.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) { }
            }
        });
        add(jep);

        Dimension size = jep.getPreferredSize();
        size.width = Math.max(size.width + 20, 300);
        size.height += 39;
        parent.setSize(size);
    }
}
