package be.scc.client;

import javax.swing.UIDefaults;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

/**
 * https://stackoverflow.com/a/3587482/1448736
 */
public class WindowsClassicLookAndFeelExt extends WindowsLookAndFeel {
    @Override
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        Object[] uiDefaults = {"LabelUI", WindowsLabelExtUI.class.getCanonicalName()};
        table.putDefaults(uiDefaults);
    }
}


