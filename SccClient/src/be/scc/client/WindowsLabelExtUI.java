package be.scc.client;


import javax.swing.*;
import javax.swing.plaf.ComponentUI;

public class WindowsLabelExtUI
        //extends com.sun.java.swing.plaf.windows.WindowsLabelUI
        //extends com.sun.java.swing.plaf.motif.MotifLabelUI
        extends javax.swing.plaf.metal.MetalLabelUI
{
    static WindowsLabelExtUI singleton = new WindowsLabelExtUI();

    public static ComponentUI createUI(JComponent c) {
        c.putClientProperty("html.disable", Boolean.TRUE);
        return singleton;
    }
}
