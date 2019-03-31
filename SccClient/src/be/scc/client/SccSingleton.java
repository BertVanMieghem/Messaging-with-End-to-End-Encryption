package be.scc.client;

public class SccSingleton {
    private static SccSingleton single_instance = null;

    // private constructor restricted to this class itself
    private SccSingleton() {
    }

    // static method to create instance of Singleton class
    public static SccSingleton inst() {
        if (single_instance == null)
            single_instance = new SccSingleton();

        return single_instance;
    }

    MainDialog mainDialog = new MainDialog();
    MessageDialog messageDialog = new MessageDialog();

    public void ShowLoginDialog() {
        mainDialog.pack();
        mainDialog.setVisible(true);
    }

    public void FromLoginToMessageDialog() {
        mainDialog.setVisible(false);

        messageDialog.pack();
        messageDialog.setVisible(true);
    }

}
