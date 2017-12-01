package nl.saxion.internettech;

public class Main {

    public static void main(String[] args) {
        System.out.println("Usage:");
        System.out.println("\t--bad-server: starts a server instance that drops client, packets and corrupts packets.");
        System.out.println("");

        ServerConfiguration config = new ServerConfiguration();
        if (args.length > 0 && args[0].equals("--bad-server")) {
            config.setSimulateCorruptedPackets(true);
            config.setSimulateDroppedPackets(true);
            config.setSimulateConnectionLost(true);
            System.out.println("Starting server in bad server mode");
        } else {

            System.out.println("Starting the server with the default configuration.");
        }
        System.out.println("\tversion:\t"+ config.VERSION);
        System.out.println("\tport:\t\t"+ config.SERVER_PORT);
        System.out.println("");
        new Server(config).run();
    }
}
