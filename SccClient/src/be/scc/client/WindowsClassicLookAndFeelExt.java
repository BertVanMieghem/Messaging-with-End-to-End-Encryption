package be.scc.client;


import javax.swing.*;
import javax.swing.plaf.ComponentUI;

/**
 * https://stackoverflow.com/a/3587482/1448736
 */
public class WindowsClassicLookAndFeelExt
        //extends com.sun.java.swing.plaf.windows.WindowsLookAndFeel
        //extends com.sun.java.swing.plaf.motif.MotifLookAndFeel
        extends javax.swing.plaf.metal.MetalLookAndFeel {
    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        Object[] uiDefaults = {"LabelUI", WindowsLabelExtUI.class.getCanonicalName()};
        table.putDefaults(uiDefaults);
    }
}


class NoSelectionModel extends DefaultListSelectionModel {

    @Override
    public void setAnchorSelectionIndex(final int anchorIndex) {
    }

    @Override
    public void setLeadAnchorNotificationEnabled(final boolean flag) {
    }

    @Override
    public void setLeadSelectionIndex(final int leadIndex) {
    }

    @Override
    public void setSelectionInterval(final int index0, final int index1) {
    }
}