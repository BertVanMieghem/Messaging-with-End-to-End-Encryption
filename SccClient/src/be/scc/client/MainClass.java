package be.scc.client;

import javax.swing.*;

public class MainClass {
    public static void main(String[] args) throws Exception {
        System.out.println("SccClient is starting");
        UIManager.setLookAndFeel(WindowsClassicLookAndFeelExt.class.getCanonicalName());

        ClientSingleton.inst().initialise();
        ClientSingleton.inst().openLoginOrSkip();

        //System.exit(0); // Explicitly needed to close the application
    }
}
