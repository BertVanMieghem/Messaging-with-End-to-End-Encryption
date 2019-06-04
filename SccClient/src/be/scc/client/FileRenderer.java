package be.scc.client;

import javax.swing.*;
import java.awt.*;

public class FileRenderer extends JLabel implements ListCellRenderer<FileMessage> {

    public FileRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends FileMessage> list, FileMessage file, int index,
                                            boolean isSelected, boolean cellHasFocus) {

        ImageIcon imageIcon = new ImageIcon(getClass().getResource("file_icon.png"));

        setIcon(imageIcon);
        setText(file.file_name);

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
