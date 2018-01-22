package nl.saxion.internettech;

public class Main {

    public static void main(String[] args) {
        System.out.println("Usage:");
        System.out.println("\t--bad-server: starts a server instance that drops client, packets and corrupts packets.");
        System.out.println("\t--bad-server-drop-msg: starts a server instance that drops packets.");
        System.out.println("\t--bad-server-corrupt: starts a server instance that corrupts packets.");
        System.out.println("\t--bad-server-drop-client: starts a server instance that drops client, packets and corrupts packets.");
        System.out.println("\t--no-colors: log bedug messages without colors in the console.");
        System.out.println("");

        if (args.length == 0) {
            System.out.println("Starting the server with the default configuration.");
        } else {
            System.out.println("Starting the server with:");
        }

        ServerConfiguration config = new ServerConfiguration();
        for (String arg : args) {
            if (arg.equals("--no-colors")) {
                config.setShowColors(false);
                System.out.println(" * Colors in debug message disabled");
            } else if (arg.equals("--bad-server-drop-msg")) {
                config.setSimulateDroppedPackets(true);
                System.out.println(" * Drop message simulation enabled");
            } else if (arg.equals("--bad-server-corrupt")) {
                config.setSimulateCorruptedPackets(true);
                System.out.println(" * Corrupt message simulation enabled");
            } else if (arg.equals("--bad-server-drop-client")) {
                config.setSimulateConnectionLost(true);
                System.out.println(" * Connection lost simulation (drop clients) enabled");
            } else if (arg.equals("--bad-server")) {
                config.setSimulateCorruptedPackets(true);
                config.setSimulateDroppedPackets(true);
                config.setSimulateConnectionLost(true);
                System.out.println(" * Full bad server mode enabled");
            }
        }
        System.out.println("-------------------------------");
        System.out.println("\tversion:\t" + config.VERSION);
        System.out.println("\tport:\t\t" + config.SERVER_PORT);
        System.out.println("-------------------------------");
        new Server(config).run();
    }
}
