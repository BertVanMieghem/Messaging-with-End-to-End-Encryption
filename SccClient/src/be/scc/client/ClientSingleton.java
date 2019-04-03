package be.scc.client;

public class ClientSingleton {
    private static ClientSingleton single_instance = null;

    // private constructor restricted to this class itself
    private ClientSingleton() {
    }

    // static method to create instance of Singleton class
    public static ClientSingleton inst() {
        if (single_instance == null)
            single_instance = new ClientSingleton();

        return single_instance;
    }

    public ClientDB db = new ClientDB();

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
