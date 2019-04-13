package be.scc.client;

public class mainClass {
    public static void main(String[] args) throws Exception {
        System.out.println("SccClient is starting");
        ClientSingleton.inst().Initilise();
        ClientSingleton.inst().OpenLoginOrSkip();

        //System.exit(0); // Explicitly needed to close the application
    }
}
