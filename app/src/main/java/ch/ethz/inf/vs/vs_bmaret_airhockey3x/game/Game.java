package ch.ethz.inf.vs.vs_bmaret_airhockey3x.game;

/**
 * Created by Valentin on 14/11/15.
 *
 * The class Game contains all relevant information for the game.
 * It is modeled as a singleton because that way it can be initialized during the setup phase and
 * and then easily be used during the gaming phase later.
 *
 */

public class Game {
    private static Game ourInstance = new Game();

    public static Game getInstance() {
        return ourInstance;
    }

    private int nrPlayer;

    private Game() {}

    public void setNrPlayer(int nr) {nrPlayer = nr;}
}
