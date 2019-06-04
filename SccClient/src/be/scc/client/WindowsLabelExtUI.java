package be.scc.client;

import com.sun.java.swing.plaf.windows.WindowsLabelUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;

public class WindowsLabelExtUI extends WindowsLabelUI {
    private static WindowsLabelExtUI singleton = new WindowsLabelExtUI();

    public static ComponentUI createUI(JComponent c) {
        c.putClientProperty("html.disable", Boolean.TRUE);
        return singleton;
    }
}
