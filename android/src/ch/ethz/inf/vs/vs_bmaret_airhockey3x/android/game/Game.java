package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    public int getNrPlayer() {return mNrPlayer;}

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
     * @param position  Position of player in game
     * @return          Player
     */
    public Player getPlayer(int position)
    {
        // TODO: Check if position is valid (consistent with mPlayerNr)
        if (position > 3 && position < 0) return null;
        return mPlayers.get(position);
    }

    /**
     * Get player object for name
     * @param name      Name of player in game
     * @return          Player
     */
    public Player getPlayer(String name)
    {
        Player res = null;
        if (name != null) {
            for (Player p : mPlayers.values()) {
                if (p.getName().equals(name)) {
                    res = p;
                    break;
                }
            }
        }
        if(res == null) Log.d(LOGTAG,"Player " + name + " does not exist");
        return res;
    }

    public List<Player> getAllPlayers()
    {
        Collection<Player> players = mPlayers.values();
        List<Player> playerList = new ArrayList<>(players);
        return playerList;
    }


    /**
     * Checks whether there is currently a player whith given name in the game.
     * @param name  Name to check
     * @return      Whether there is said player or not
     */
    public boolean existsName(String name)
    {
        boolean res = false;
        for (Player p : mPlayers.values()) {
            if (p.getName().equals(name)) res = true;
        }
        return res;
    }

    public void resetScores()
    {
        for (Player p : mPlayers.values()) p.setScore(0);
    }

}
