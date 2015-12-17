package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.TuplePlayerScore;

/**
 * Created by Rimle on 17.12.2015.
 *
 * player who received a goal sends the updated score to all other players.
 *
 * We do it by name this time to not have to think about the relative positions
 */
public class ScoreMessage extends Message{

    private final static String PLAYER_0_NAME_KEY = "playerName0";
    private final static String PLAYER_1_NAME_KEY = "playerName1";
    private final static String PLAYER_2_NAME_KEY = "playerName2";

    private final static String PLAYER_0_SCORE_KEY = "playerScore0";
    private final static String PLAYER_1_SCORE_KEY = "playerScore1";
    private final static String PLAYER_2_SCORE_KEY = "playerScore2";

    private final static String STATES_PLAYER_0_KEY = "statesPlayer0";
    private final static String STATES_PLAYER_1_KEY = "statesPlayer1";
    private final static String STATES_PLAYER_2_KEY = "statesPlayer2";


    private String playerName0;
    private String playerName1;
    private String playerName2;

    private int playerScore0;
    private int playerScore1;
    private int playerScore2;

    private JSONObject statesP0;
    private JSONObject statesP1;
    private JSONObject statesP2;


    public ScoreMessage(int receiverPos, TuplePlayerScore p0, TuplePlayerScore p1, TuplePlayerScore p2) {
        super(receiverPos, Message.SCORE_MSG);
        this.playerScore0 = p0.getScore();
        this.playerScore1 = p1.getScore();
        this.playerScore2 = p2.getScore();

        this.playerName0 = p0.getPlayer();
        this.playerName1 = p1.getPlayer();
        this.playerName2 = p2.getPlayer();

        try {
            mBody = new JSONObject();
            statesP0 = new JSONObject();
            statesP1 = new JSONObject();
            statesP2 = new JSONObject();

            statesP0.put(PLAYER_0_NAME_KEY, playerName0);
            statesP0.put(PLAYER_0_SCORE_KEY, playerScore0);

            statesP1.put(PLAYER_1_NAME_KEY, playerName1);
            statesP1.put(PLAYER_1_SCORE_KEY, playerScore1);

            statesP2.put(PLAYER_2_NAME_KEY, playerName2);
            statesP2.put(PLAYER_2_SCORE_KEY, playerScore2);

            mBody.put(STATES_PLAYER_0_KEY, statesP0);
            mBody.put(STATES_PLAYER_1_KEY, statesP1);
            mBody.put(STATES_PLAYER_2_KEY, statesP2);


            mMsg.put(BODY_KEY,mBody);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public ScoreMessage(Message msg){
        super(msg);

        try {

            mBody = new JSONObject();
            statesP0 = new JSONObject();
            statesP1 = new JSONObject();
            statesP2 = new JSONObject();

            mBody = mMsg.getJSONObject(BODY_KEY);
            statesP0 = mBody.getJSONObject(STATES_PLAYER_0_KEY);
            statesP1 = mBody.getJSONObject(STATES_PLAYER_1_KEY);
            statesP2 = mBody.getJSONObject(STATES_PLAYER_2_KEY);

            playerName0 = statesP0.getString(PLAYER_0_NAME_KEY);
            playerName1 = statesP1.getString(PLAYER_1_NAME_KEY);
            playerName2 = statesP2.getString(PLAYER_2_NAME_KEY);

            playerScore0 = statesP0.getInt(PLAYER_0_SCORE_KEY);
            playerScore1 = statesP1.getInt(PLAYER_1_SCORE_KEY);
            playerScore2 = statesP2.getInt(PLAYER_2_SCORE_KEY);





        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerName0(){
        return playerName0;
    }
    public String getPlayerName1(){
        return playerName1;
    }
    public String getPlayerName2(){
        return playerName2;
    }

    public int getPlayerScore0(){
        return playerScore0;
    }
    public int getPlayerScore1(){
        return playerScore1;
    }
    public int getPlayerScore2(){
        return playerScore2;
    }
}
