package be.scc.client;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class FileRenderer extends JLabel implements TableCellRenderer {

    public FileRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable list, Object object, boolean isSelected, boolean hasFocus, int row, int column) {

        ImageIcon imageIcon = new ImageIcon(getClass().getResource("file.png"));

        setIcon(imageIcon);
        setText(object.toString());

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
