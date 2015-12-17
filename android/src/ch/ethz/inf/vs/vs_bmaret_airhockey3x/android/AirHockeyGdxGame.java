package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

import android.os.SystemClock;
import android.util.Log;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Random;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.ExitGameMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.Message;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.PuckMovementMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.ScoreMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game.Game;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game.Player;


/**
 *
 * Notes for etienne
 *
 * 1. We created a PuckMovementMessage to inform the other player about his puck. Currently there are
 * only two float values available for the position (x,y). If you need another two for velocity add them
 * the same way as the position values
 *
 * 2. Scores: We store the scores in the players which are in the mGame instance. Use updateScore()
 * to update the scores. It updates the local value and broadcasts a message to all others. We thought it
 * best that just the player who sent the puck gets one point. There are no "eigengoal" during the game.
 * -> What to do when we start and make an eigengoal; who gets the point?
 *
 * 3. Could not test if it works to leave the game and reenter, because we dont now how to /**
 *
 * Notes for etienne
 *
 * 1. We created a PuckMovementMessage to inform the other player about his puck. Currently there are
 * only two float values available for the position (x,y). If you need another two for velocity add them
 * the same way as the position values
 *
 * 2. Scores: We store the scores in the players which are in the mGame instance. Use updateScore()
 * to update the scores. It updates the local value and broadcasts a message to all others. We thought it
 * best that just the player who sent the puck gets one point. There are no "eigengoal" during the game.
 * -> What to do when we start and make an eigengoal; who gets the point?
 *
 * 3. Could not test if it works to leave the game and reenter, because we didnt know how to leave the game in
 * code (for example in onPlayerDisconnected() or when received an exitGame message)
 *
 * 4. As soon as one leaves the game all should leave. We broadcast an exit_game message in dispose()
 * and in onPlayerDisconnected()
 *
 * 5. Dont forget to remove buttons in setupactivities
 */



public class AirHockeyGdxGame extends ApplicationAdapter implements InputProcessor, BluetoothCommListener {

    private final static String LOGTAG = "AirHockeyGdxGame";

    public static final boolean PHYSICS_MULTITHREAD_ENABLED = true; // Decouple physics from drawing (highly recommended)
    public static final int PHYSICS_TIMESTEP = 10; // Time in milliseconds between physics updates

    public static final boolean GRAVITY_ON = false; // For testing purposes only
    public static final boolean CEILING_ON = false; // Does the puck collide with ceiling? (testing)

    public static final float PUCK_RADIUS = 75; // I think 75 would be a good number here
    public static final float MALLET_RADIUS = 90; // And 90 pixels here

    public static final float COEFFICIENT_OF_RESTITUTION = 0.75f; // Bounciness

    public static final float GOAL_SIZE = 550; // Goal width in pixels
    public static final float RAIL_THICKNESS = 20; // Rail thickness in pixels

    public static final int RAIL_COLOR = Color.rgba8888(0, 1, 0, 1);

    public static final int NUM_PLAYERS = 3;


    SpriteBatch batch;

    float w;
    float h;

    float scaleFactor; // For screens that are not 1920x1080
    private Pixmap pixmap;
    private Texture rail_img;
    private float railThickness;
    private float goalSize;
    private float leftGoalPost;
    private float rightGoalPost;

    class Circle {
        public Vector2 pos;
        public Vector2 vel;
        public Texture img;
        public float radius;
        public float mass;
        boolean isMallet = false;
        boolean dragging;
        boolean previouslyDragging;
        boolean collided = false;
        boolean messageSent = false;

        public Circle(Vector2 p, Vector2 v) {
            pos = p;
            vel = v;
            img = new Texture("circle-256.png");
            radius = 128;
            mass = 10;
        }

        public void draw(SpriteBatch batch) {
            Float d = (System.nanoTime() - lastUpdate) / 1000000000f;
            if (dragging || collided) {
                batch.draw(img, pos.x - radius, pos.y - radius, 2 * radius, 2 * radius);
            } else {
                batch.draw(img, pos.x + d * vel.x - radius, pos.y + d * vel.y - radius, 2 * radius, 2 * radius);
            }
        }

        public void updatePosition(float d) {
            // Check if we have NaN values
            if (Float.isNaN(vel.x)) vel.x = 0;
            if (Float.isNaN(vel.y)) vel.y = 0;

            // Good checks to catch rare bugs
            vel.clamp(0.000001f, 230400);
            if (vel.isZero()) vel.set(0, 0.000001f);

            // Do the core update
            pos.x = pos.x + d * vel.x;
            pos.y = pos.y + d * vel.y;
        }

        public void processCollisions(float d) {
            collided = false;

            if (pos.y < radius + railThickness && isMallet) {
                vel.y = -vel.y * COEFFICIENT_OF_RESTITUTION;
                pos.y = radius + railThickness;
                collided = true;
            }
            if (pos.x < radius + railThickness) {
                vel.x = -vel.x * COEFFICIENT_OF_RESTITUTION;
                pos.x = radius + railThickness;
                collided = true;
            }
            if (pos.x > w - radius - railThickness) {
                vel.x = -vel.x * COEFFICIENT_OF_RESTITUTION;
                pos.x = w - radius - railThickness;
                collided = true;
            }
            if (pos.y > h - radius && (isMallet || CEILING_ON)) {
                vel.y = -vel.y * COEFFICIENT_OF_RESTITUTION;
                pos.y = h - radius;
                collided = true;
            }

            // Process puck collisions with lower rail
            if (!isMallet) {
                if (pos.y < radius + railThickness) {
                    if (pos.x < leftGoalPost || pos.x > rightGoalPost) {
                        vel.y = -vel.y * COEFFICIENT_OF_RESTITUTION;
                        pos.y = radius + railThickness;
                        collided = true;
                    } else {
                        if (pos.y < railThickness) {
                            if (pos.x < radius + leftGoalPost) {
                                vel.x = -vel.x * COEFFICIENT_OF_RESTITUTION;
                                pos.x = radius + leftGoalPost;
                                collided = true;
                            }
                            if (pos.y < railThickness && pos.x > rightGoalPost - radius) {
                                vel.x = -vel.x * COEFFICIENT_OF_RESTITUTION;
                                pos.x = rightGoalPost - radius;
                                collided = true;
                            }
                        } else {
                            if (pos.dst(leftGoalPost, railThickness) < radius) {
                                Vector2 normal = pos.cpy().sub(leftGoalPost, railThickness).nor();
                                Vector2 normalComponent = normal.cpy().scl(Math.abs(2 * normal.cpy().dot(vel)));
                                vel.add(normalComponent).scl(COEFFICIENT_OF_RESTITUTION);
                                collided = true;
                            }
                            if (pos.dst(rightGoalPost, railThickness) < radius) {
                                Vector2 normal = pos.cpy().sub(rightGoalPost, railThickness).nor();
                                Vector2 normalComponent = normal.cpy().scl(Math.abs(2 * normal.cpy().dot(vel)));
                                vel.add(normalComponent).scl(COEFFICIENT_OF_RESTITUTION);
                                collided = true;
                            }
                        }
                    }
                }
            }

            // Process goals
            if (!isMallet && pos.y < - radius) {
                // Todo: Register goals
                pos.set(w/2,h/2);
                vel.set(0, 0.000001f);
            }

            // Process exiting the screen
            if (!isMallet && pos.y > h + radius && !messageSent) {

                Log.d("Exiting screen", "a");

                if (pos.x+((2743*scaleFactor-pos.y)/vel.y)*vel.x < w/2){
                    // Send to left player
                    Log.d("","Send to left player");
                    float new_pos_x = pos.x + 3282.76877526612220178634848784563747226509304365796540293357f;
                    float new_pos_y = pos.y + 24.6925639128062614951789755868289218508851629423944608498642f;
                    // Todo: Apply rotation, send message

                    PuckMovementMessage pmsg = new PuckMovementMessage(1,new_pos_x,new_pos_y);
                    mBC.sendMessage(pmsg);

                } else {
                    // Send to right player
                    Log.d("","Send to right player");
                    float new_pos_x = pos.x + 2855.30743608719373850482102441317107814911483705760553915013f;
                    float new_pos_y = pos.y + 1620f;
                    // Todo: Apply rotation, send message

                    PuckMovementMessage pmsg = new PuckMovementMessage(3,new_pos_x,new_pos_y);
                    mBC.sendMessage(pmsg);
                }

                pos.set(w/2,h/2); // For testing
                vel.set(0, 0.000001f);

                //messageSent = true;
            }
        }
    }

    Circle puck;
    Circle mallet;

    private OrthographicCamera camera;

    private BluetoothComm mBC;
    private Game mGame;

    Vector3 tp3 = new Vector3();

    long lastUpdate;

    @Override
    public void create() {

        mBC = BluetoothComm.getInstance();
        mBC.registerListener(this); // TODO: Deregister when leaving the game
        mGame = Game.getInstance();

        // TODO: delete this; This is only to test score messages
        Random random = new Random();
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        TuplePlayerScore t1 = new TuplePlayerScore(mGame.getPlayer(0).getName(),5);
                        TuplePlayerScore t2 = new TuplePlayerScore(mGame.getPlayer(1).getName(),2);
                        TuplePlayerScore t3 = new TuplePlayerScore(mGame.getPlayer(3).getName(),5);
                        ScoreMessage smsg = new ScoreMessage(Message.BROADCAST,t1,t2,t3);
                        mBC.sendMessage(smsg);
                    }
                },
                5000 + random.nextInt(200)
        );


        w = Gdx.graphics.getWidth();
        h = Gdx.graphics.getHeight();

        scaleFactor = w / 1920f;

        railThickness = RAIL_THICKNESS * scaleFactor;
        goalSize = GOAL_SIZE * scaleFactor;
        leftGoalPost = (w - goalSize) / 2;
        rightGoalPost = w - (w - goalSize) / 2;

        // Puck, mallet initial properties
        puck = new Circle(new Vector2(w / 2, h / 2), new Vector2(0, -1));
        puck.radius = PUCK_RADIUS * scaleFactor;
        puck.mass = puck.radius * puck.radius;
        mallet = new Circle(new Vector2(w / 2, h / 2 - 200), new Vector2(0, 1));
        mallet.radius = MALLET_RADIUS * scaleFactor;
        mallet.mass = mallet.radius * mallet.radius;
        mallet.isMallet = true;

        // Initialize LibGDX things
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);

        // Prepare our rail textures
        pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(RAIL_COLOR);
        pixmap.fill();
        rail_img = new Texture(pixmap);

        // Enable touch input
        Gdx.input.setInputProcessor(this);

        // Create our physics thread
        Runnable r = new Runnable() {
            @Override
            public void run() {
                long time;
                while (true) {
//                    time = System.nanoTime();
                    SystemClock.sleep(PHYSICS_TIMESTEP);
//                    Log.d("Sleep took", Long.toString(System.nanoTime() - time));
//                    time = System.nanoTime();
                    update(PHYSICS_TIMESTEP / 1000f);
//                    Log.d("Update took", Long.toString(System.nanoTime() - time));
                }
            }
        };

        if (PHYSICS_MULTITHREAD_ENABLED) new Thread(r).start();
    }

    @Override
    public void render() {
        if (!PHYSICS_MULTITHREAD_ENABLED)
            update(Gdx.graphics.getDeltaTime()); // Update physics before rendering (old behavior)

//        if(updating){
//            int count = 0;
//            while(updating){
//                count++;
//            }
//            Log.d("Busy waited",Integer.toString(count) + " times.");
//        }

        // No need to wait for updating to finish, just draw our objects wherever they are now.

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        puck.draw(batch);
        mallet.draw(batch);

        //Draw rails
        batch.draw(rail_img, 0, 0, railThickness, h);
        batch.draw(rail_img, w - railThickness, 0, railThickness, h);
        batch.draw(rail_img, 0, 0, leftGoalPost, railThickness);
        batch.draw(rail_img, rightGoalPost, 0, leftGoalPost, railThickness);

        batch.end();
    }

    @Override
    public void dispose() {

        // TODO: This code goes into function that gets called when leaving the game. Right place here?
        /*
            Tell the others to exit too. We do it simple and stop the entire game as soon as one
            leaves
         */
        ExitGameMessage emsg = new ExitGameMessage(Message.BROADCAST);
        mBC.sendMessage(emsg);
        mBC.unregisterListener(this);

    }

    private void update(float d) {
        // Physics units: pixels (on a 1080x1920 screen), seconds

//        updating = true;

        lastUpdate = System.nanoTime();

        // Calculate touch point position and velocity
        Vector2 tp = new Vector2(tp3.x, tp3.y);
        Vector2 tp_vel = tp.cpy();
        tp_vel.sub(mallet.pos);
        tp_vel.scl(1 / d);

        if (mallet.dragging || mallet.previouslyDragging) {
            mallet.vel.set(tp_vel);
            if (!mallet.previouslyDragging) {
                mallet.pos.set(tp);
                mallet.vel.set(0, 0.000001f);
            }
        }


        // Make things a little more interesting during testing
        if (GRAVITY_ON) {
            puck.vel.y = puck.vel.y - d * 3000f;
            mallet.vel.y = mallet.vel.y - d * 3000f;
        }

        // Position += velocity
        puck.updatePosition(d);
        mallet.updatePosition(d);

        // Move mallet to the touch point (which is frowned upon nowadays)
        // if (mallet.dragging) mallet.pos.set(tp);

        // Calculate other, more boring collisions
        puck.processCollisions(d);
        mallet.processCollisions(d);

        // This whole next section is to resolve collisions between the puck and the mallet
        if (mallet.pos.dst(puck.pos) < mallet.radius + puck.radius) {
//            Log.d("Puck oldVel:", String.format("%f,%f", puck.vel.x, puck.vel.y));
//            Log.d("Mallet oldVel:", String.format("%f,%f", mallet.vel.x, mallet.vel.y));

            // Turn back time to when they weren't intersecting
            double backTimeRoot = 0.5 * Math.sqrt(4 * Math.pow(puck.pos.x * (puck.vel.x - mallet.vel.x) +
                    mallet.pos.x * (-puck.vel.x + mallet.vel.x) + (puck.pos.y - mallet.pos.y) * (puck.vel.y - mallet.vel.y), 2) -
                    4 * (puck.pos.x * puck.pos.x + puck.pos.y * puck.pos.y - 2 * puck.pos.x * mallet.pos.x + mallet.pos.x * mallet.pos.x - 2 * puck.pos.y * mallet.pos.y + mallet.pos.y * mallet.pos.y -
                            puck.radius * puck.radius - 2 * puck.radius * mallet.radius - mallet.radius * mallet.radius) * (puck.vel.x * puck.vel.x + puck.vel.y * puck.vel.y - 2 * puck.vel.x * mallet.vel.x + mallet.vel.x * mallet.vel.x -
                            2 * puck.vel.y * mallet.vel.y + mallet.vel.y * mallet.vel.y));
            double backTimeSummand = puck.pos.x * puck.vel.x - mallet.pos.x * puck.vel.x + puck.pos.y * puck.vel.y - mallet.pos.y * puck.vel.y - puck.pos.x * mallet.vel.x + mallet.pos.x * mallet.vel.x - puck.pos.y * mallet.vel.y + mallet.pos.y * mallet.vel.y;
            double backTimeDivisor = puck.vel.x * puck.vel.x + puck.vel.y * puck.vel.y - 2 * puck.vel.x * mallet.vel.x + mallet.vel.x * mallet.vel.x - 2 * puck.vel.y * mallet.vel.y + mallet.vel.y * mallet.vel.y;
            double backTime = (backTimeSummand + backTimeRoot) / backTimeDivisor;
            backTime += 0.001; //compensate for floating point errors

            puck.pos.sub(puck.vel.cpy().scl((float) backTime));
            mallet.pos.sub(mallet.vel.cpy().scl((float) backTime));

            // Calculate collision normal
            Vector2 collisionNormal = mallet.pos.cpy().sub(puck.pos).nor();

            // Decompose puck vel in parallel and orthogonal part
            float pvDot = collisionNormal.dot(puck.vel);
            Vector2 pvCollide = collisionNormal.cpy().scl(pvDot);
            Vector2 pvRemainder = puck.vel.cpy().sub(pvCollide);

            // Decompose mallet vel in parallel and orthogonal part
            float mvDot = collisionNormal.dot(mallet.vel);
            Vector2 mvCollide = collisionNormal.cpy().scl(mvDot);
            Vector2 mvRemainder = mallet.vel.cpy().sub(mvCollide);

            // Calculate the collision
            float pvLength = pvCollide.len() * Math.signum(pvDot);
            float mvLength = mvCollide.len() * Math.signum(mvDot);
            float commonVelocity = 2 * (puck.mass * pvLength + mallet.mass * mvLength) / (puck.mass + mallet.mass);
            float pvLengthAfterCollision = commonVelocity - pvLength * COEFFICIENT_OF_RESTITUTION;
            float mvLengthAfterCollision = commonVelocity - mvLength * COEFFICIENT_OF_RESTITUTION;
            pvCollide.scl(pvLengthAfterCollision / pvLength);
            mvCollide.scl(mvLengthAfterCollision / mvLength);

            // Recombine the velocity
            puck.vel.set(pvCollide.cpy().add(pvRemainder));
            mallet.vel.set(mvCollide.cpy().add(mvRemainder));

            // Undo time travel
            puck.pos.add(puck.vel.cpy().scl((float) backTime));
            mallet.pos.add(mallet.vel.cpy().scl((float) backTime));

//            Log.d("Puck newVel:", String.format("%f,%f", puck.vel.x, puck.vel.y));
//            Log.d("Mallet newVel:", String.format("%f,%f", mallet.vel.x, mallet.vel.y));

            // Small hack to make things more sane
            if (Float.isNaN(mallet.pos.x) || Float.isNaN(mallet.pos.y)) mallet.pos.set(tp);
        }

        // Keep track of some stuff for the next update
        mallet.previouslyDragging = mallet.dragging;

//        updating = false;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT || pointer > 0) return false;
        camera.unproject(tp3.set(screenX, screenY, 0));
        mallet.dragging = true;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT || pointer > 0) return false;
        camera.unproject(tp3.set(screenX, screenY, 0));
        mallet.dragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!mallet.dragging) return false;
        camera.unproject(tp3.set(screenX, screenY, 0));
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }


    /**
     * Set score and broadcast message to others
     * @param player    Position of player who has made the goal
     *
     * We do it as follows: The last to touch the puck (other the ourselves) gets the point.
     * If player 1
     */
    private void updateScore(int player)
    {
        // Local update
        Player luckyOne = mGame.getPlayer(player);
        luckyOne.setScore(luckyOne.getScore() + 1);

        // Remote update
        Player p0 = mGame.getPlayer(0);
        Player p1 = mGame.getPlayer(1);
        Player p3 = mGame.getPlayer(3);

        TuplePlayerScore t1 = new TuplePlayerScore(p0.getName(),p0.getScore());
        TuplePlayerScore t2 = new TuplePlayerScore(p1.getName(),p1.getScore());
        TuplePlayerScore t3 = new TuplePlayerScore(p3.getName(),p3.getScore());

        ScoreMessage smsg = new ScoreMessage(Message.BROADCAST,t1,t2,t3);
        mBC.sendMessage(smsg);
    }

    /**
     *
     * BluetoothComm callbacks
     *
     */

    /**
     * Handle incoming messages
     * @param msg   Message
     */
    public void onReceiveMessage(Message msg)
    {

        // TODO: Everything

        String msgType = msg.getType();
        Log.d(LOGTAG, "Received message with type " + msgType);
        switch (msgType) {
            case Message.TEST_MSG:
                break;
            case Message.PUCK_MOVEMENT_MSG:
                PuckMovementMessage pmsg = new PuckMovementMessage(msg);
                float xpos = pmsg.getXPosition();
                float ypos = pmsg.getYPosition();

                Log.d(LOGTAG,"remote position: x " + Float.toString(xpos) + " y "
                                + Float.toString(ypos));
                break;
            case Message.SCORE_MSG:

                ScoreMessage smsg = new ScoreMessage(msg);
                String player0 = smsg.getPlayerName0();
                String player1 = smsg.getPlayerName1();
                String player2 = smsg.getPlayerName2();
                int score0 = smsg.getPlayerScore0();
                int score1 = smsg.getPlayerScore1();
                int score2 = smsg.getPlayerScore2();
                Log.d(LOGTAG,"Score of players");
                Log.d(LOGTAG,player0 + ": " + score0);
                Log.d(LOGTAG,player1 + ": " + score1);
                Log.d(LOGTAG, player2 + ": " + score2);

                // Save score
                Player p0 = mGame.getPlayer(player0);
                Player p1 = mGame.getPlayer(player1);
                Player p2 = mGame.getPlayer(player2);

                if(p0 != null)  p0.setScore(score0);
                if(p1 != null)  p1.setScore(score1);
                if(p2 != null)  p2.setScore(score2);


                break;
            case Message.EXIT_GAME_MSG:

                Log.d(LOGTAG, "Going to exit game because player received a exit message from player "
                        + Integer.toString(msg.getSender()));
                // TODO: Exit game


                break;
            default:
        }
    }

    public void onPlayerDisconnected(int pos)
    {
        Log.d(LOGTAG,"Going to exit game because player " + Integer.toString(pos) + " was disconnected");

        // Use this to exit game
        ExitGameMessage emsg = new ExitGameMessage(Message.BROADCAST);
        mBC.sendMessage(emsg);
        mGame.getPlayer(pos).setName(null);
        mGame.getPlayer(pos).setConnected(false);

        // TODO: Exit game
    }


    public void onPlayerConnected(int pos, String name) {Log.d(LOGTAG,"Called unused callback - onPlayerConnected" );}
    public void onDeviceFound(String name,String address) {Log.d(LOGTAG,"Called unused callback - onDeviceFound" );}
    public void onStartConnecting() {Log.d(LOGTAG,"Called unused callback - onStartConnecting" );}
    public void onScanDone() {Log.d(LOGTAG,"Called unused callback - onScanDone" );}
    public void onNotDiscoverable() {Log.d(LOGTAG,"Called unused callback - onNotDiscoverable" );}
    public void onBluetoothNotSupported() {Log.d(LOGTAG,"Called unused callback - onBluetoothNotSupported" );}

}