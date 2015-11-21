package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import java.util.UUID;

/**
 * Created by oliver on 21.11.15.
 *
 * UUID Provider.
 *
 * To maintain multiple Connections also multiple UUIDs are required.
 * This class provides 6 UUIDs to establish connections between 4 players.
 * To player with the lower position will serve as server for higher positioned players
 *
 * The <from>TO<to> refers to the connection from server at player <from> to client at player <to>.
 */
public class UUIDConfig {
    public static final UUID UUID_0TO1 = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a60");
    public static final UUID UUID_0TO2 = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a61");
    public static final UUID UUID_0TO3 = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a62");

    public static final UUID UUID_1TO2 = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a64");
    public static final UUID UUID_1TO3 = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a65");

    public static final UUID UUID_2TO3 = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a68");
}
