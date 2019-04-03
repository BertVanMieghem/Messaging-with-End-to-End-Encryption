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

    LoginDialog loginDialog = new LoginDialog();
    ChatDialog chatDialog = new ChatDialog();

    public void ShowLoginDialog() {
        loginDialog.pack();
        loginDialog.setVisible(true);
    }

    public void FromLoginToChatDialog() {
        loginDialog.setVisible(false);

        chatDialog.pack();
        chatDialog.setVisible(true);
    }

}
