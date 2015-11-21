package ch.ethz.inf.vs.vs_bmaret_airhockey3x.game;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Valentin on 14/11/15.
 *
 * The class Game contains all relevant information for the game.
 * It is modeled as a singleton because that way it can be initialized during the setup phase and
 * and then easily be used during the gaming phase later.
 *
 */

public class Game {

    private final String LOGTAG = "Game";

    private static Game ourInstance = new Game();

    public static Game getInstance() {
        return ourInstance;
    }

    private int mNrPlayer;
    private Map<Integer,Player> mPlayers = new HashMap<>();


    private Game() {}

    public void setNrPlayer(int nr) {mNrPlayer = nr;}

    /**
     * Add player p ad position position
     * Topology:
     *   2
     * 1  3
     *  0
     * @param p         Given player
     */
    public void addPlayer(Player p)
    {
        if (p != null) mPlayers.put(p.getPosition(),p);
        else Log.d(LOGTAG,"Attempted to add null player");
    }

    /**
     * Get player object for position
     * @param position  Position of player on game
     * @return          Player
     */
    public Player getPlayer(int position)
    {
        // TODO: Check if position is valid (consistent with mPlayerNr)
        if (position > 3 && position < 0) return null;
        return mPlayers.get(position);
    }
}
