package nl.saxion.internettech;

/**
 * Method that has to be run if one wants the server to start. Change details about the server's startup by changing
 * the ServerConfiguration class.
 *
 * @author of the project as a whole (excluding this class, the server directory as a whole and some other classed) Martin S. Slavov (435666) & Yoran Kerbusch (430818)
 */
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
        //Loop through different possible arguments the user might've added.
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
