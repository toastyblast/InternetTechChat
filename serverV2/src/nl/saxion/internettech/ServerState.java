package nl.saxion.internettech;

/**
 * Enum that houses the representation of the server states for each of the client threads statuses
 */
public enum ServerState {
    INIT,
    CONNECTING,
    CONNECTED,
    FINISHED
}
