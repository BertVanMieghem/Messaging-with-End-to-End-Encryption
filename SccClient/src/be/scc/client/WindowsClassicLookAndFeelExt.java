package be.scc.client;

import javax.swing.UIDefaults;

public class WindowsClassicLookAndFeelExt
        //extends com.sun.java.swing.plaf.windows.WindowsLookAndFeel
        //extends com.sun.java.swing.plaf.motif.MotifLookAndFeel
        extends javax.swing.plaf.metal.MetalLookAndFeel
{
    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        Object[] uiDefaults = {"LabelUI", WindowsLabelExtUI.class.getCanonicalName()};
        table.putDefaults(uiDefaults);
    }
}


