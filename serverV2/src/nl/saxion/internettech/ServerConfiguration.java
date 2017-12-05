package nl.saxion.internettech;

public class ServerConfiguration {

    // Constants.
    public final String WELCOME_MESSAGE = "Welkom to WhatsUpp!";
    public final String VERSION = "1.2";
    public final int SERVER_PORT = 1337;
    // Colors for the console.
    public final String RESET_CLI_COLORS = "\u001B[0m";   // ANSI RESET
    public final String CLI_COLOR_INCOMING = "\u001B[31m";    // ANSI RED
    public final String CLI_COLOR_OUTGOING = "\u001B[32m";  // ANSI GREEN


    // Enable colors in debug messages
    private boolean showColors = true;

    // Simulation parameters for bad server modus.
    private boolean simulateDroppedPackets = false;
    private boolean simulateCorruptedPackets = false;
    private boolean simulateConnectionLost = false;

    public boolean doSimulateDroppedPackets() {
        return simulateDroppedPackets;
    }

    public void setSimulateDroppedPackets(boolean simulateDroppedPackets) {
        this.simulateDroppedPackets = simulateDroppedPackets;
    }

    public boolean doSimulateCorruptedPackets() {
        return simulateCorruptedPackets;
    }

    public void setSimulateCorruptedPackets(boolean simulateCorruptedPackets) {
        this.simulateCorruptedPackets = simulateCorruptedPackets;
    }

    public boolean doSimulateConnectionLost() {
        return simulateConnectionLost;
    }

    public void setSimulateConnectionLost(boolean simulateConnectionLost) {
        this.simulateConnectionLost = simulateConnectionLost;
    }

    public boolean isShowColors() {
        return showColors;
    }

    public void setShowColors(boolean showColors) {
        this.showColors = showColors;
    }

}
