package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

import android.util.Log;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothComm;

public class AirHockeyGdxGame extends ApplicationAdapter implements InputProcessor {
    SpriteBatch batch;
    Texture puck_img;
    float w;
    float h;
    Vector2 puck_pos;
    Vector2 puck_vel;

    int counter = 0;
    int pcounter = 0;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private ShapeRenderer shapes;

    // Box2D garbage, unused
//    private Box2DDebugRenderer b2dr;
//    private World world;
//    private Body player;

    private BluetoothComm mBC;


    @Override
    public void create() {

        Runnable r = new Runnable() {
            @Override
            public void run() {
//                while (true){
                counter++;
//                }
            }
        };

        new Thread(r).start();

        w = Gdx.graphics.getWidth();
        h = Gdx.graphics.getHeight();

        // Puck properties
        puck_pos = new Vector2(w / 2,h / 2);
        puck_vel = new Vector2(300,200);

        batch = new SpriteBatch();
        puck_img = new Texture("circle-256.png");

        camera = new OrthographicCamera();
        camera.setToOrtho(false, w, h);

        viewport = new FitViewport(1920, 1080, camera);

        shapes = new ShapeRenderer();

        Gdx.input.setInputProcessor(this);


//        world = new World(new Vector2(0,-500f), false);
//        b2dr = new Box2DDebugRenderer();
//
//        player = createPlayer();
    }

    @Override
    public void render() {
        update(Gdx.graphics.getDeltaTime());

//		long time = TimeUtils.millis() % 4000L;
//		float angle = 0.050f * ((int) time);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        batch.draw(puck_img, puck_pos.x - 128, puck_pos.y - 128);
        batch.draw(puck_img, tp3.x - 128, tp3.y - 128);
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.circle(tp3.x, tp3.y, 0.25f, 16);
        shapes.end();


//        b2dr.render(world, camera.combined);

//        pcounter = counter;
    }

    Vector3 tp3 = new Vector3();
    boolean dragging;
    Vector2 old_tp = new Vector2(0,0);

    @Override
    public void dispose() {
//        world.dispose();
//        b2dr.dispose();
        shapes.dispose();
    }

    private void update(float d) {
//        world.step(d, 6, 2);
        Vector2 tp = new Vector2(tp3.x,tp3.y);
        Vector2 tp_vel = tp.cpy();
        tp_vel.sub(old_tp);
        tp_vel.scl(1/d);
        
        puck_vel.y = puck_vel.y - d * 500f;

        // rudimentary puck mallet collision
        if (tp.dst(puck_pos) < 256) {
            Vector2 gdist = puck_pos.cpy();
            gdist.sub(tp).nor().scl(256);
//            puck_vel.sub(normal.scl(2f * normal.dot(puck_vel)));
//            puck_vel.add(tp_vel);
            puck_vel.x = (puck_vel.x * (10-10) + (2 * 10 * tp_vel.x)) / (10 + 10);
            puck_vel.y = (puck_vel.y * (10-10) + (2 * 10 * tp_vel.y)) / (10 + 10);
            puck_pos = tp.cpy().add(gdist);
        }

        puck_pos.x = puck_pos.x + d * puck_vel.x;
        puck_pos.y = puck_pos.y + d * puck_vel.y;


        if (puck_pos.y < 128) {
            puck_vel.y = -puck_vel.y;
            puck_pos.y = 128;
        }
        if (puck_pos.x < 128) {
            puck_vel.x = -puck_vel.x;
            puck_pos.x = 128;
        }
        if (puck_pos.x > w - 128) {
            puck_vel.x = -puck_vel.x;
            puck_pos.x = w - 128;
        }
        if (puck_pos.y > h - 128) {
            puck_vel.y = -puck_vel.y;
            puck_pos.y = h - 128;
        }

        old_tp = tp.cpy();
    }

//    public Body createPlayer() {
//        Body pBody;
//        BodyDef def = new BodyDef();
//        def.type = BodyDef.BodyType.DynamicBody;
//        def.position.set(w/2, h/2);
//        def.fixedRotation = true;
//        pBody = world.createBody(def);
//
//        CircleShape shape = new CircleShape();
//        shape.setRadius(128);
//
//        pBody.createFixture(shape, 1.0f);
//        shape.dispose();
//
//        return pBody;
//    }

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
        dragging = true;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT || pointer > 0) return false;
        camera.unproject(tp3.set(screenX, screenY, 0));
        dragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!dragging) return false;
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
}