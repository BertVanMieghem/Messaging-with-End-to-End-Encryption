package be.scc.client;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ChannelRenderer extends JLabel implements ListCellRenderer<Channel> {

    public ChannelRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Channel> list, Channel country, int index,
                                                  boolean isSelected, boolean cellHasFocus) {

        ImageIcon imageIcon = new ImageIcon(getClass().getResource("gb.png"));

        setIcon(imageIcon);
        setText(country.name);

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
