package be.scc.client;

import java.awt.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ChannelRenderer extends JLabel implements ListCellRenderer<Channel> {

    public ChannelRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Channel> list, Channel listItem, int index,
                                                  boolean isSelected, boolean cellHasFocus) {

        ImageIcon imageIcon = new ImageIcon(getClass().getResource("gb.png"));

        setIcon(imageIcon);
        setText(listItem.name);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}

