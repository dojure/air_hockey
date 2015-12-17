package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game.Player;

/**
 * Created by Rimle on 17.12.2015.
 */
public class TuplePlayerScore {

    private String player;
    private int score;

    public TuplePlayerScore(String player, int score){
        this.player = player;
        this.score = score;
    }

    public int getScore(){
        return score;
    }

    public String getPlayer(){
        return player;
    }
}