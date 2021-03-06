package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

		import android.os.Bundle;
		import android.support.v4.app.FragmentActivity;
		import android.support.v4.app.FragmentTransaction;
		import android.util.Log;
		import android.view.LayoutInflater;
		import android.view.View;
		import android.view.ViewGroup;

        import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
		import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
        import com.badlogic.gdx.backends.android.surfaceview.RatioResolutionStrategy;

public class AndroidLauncher extends FragmentActivity implements AndroidFragmentApplication.Callbacks {

	public static FragmentActivity instance;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        instance = this; // We store a reference to the single instance of this class here.
		GameFragment fragment = new GameFragment();
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		trans.replace(android.R.id.content, fragment);
		trans.commit();
	}

	public static class GameFragment extends AndroidFragmentApplication
	{

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{          AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
			config.useImmersiveMode = true;
			config.useWakelock = true;
			return initializeForView(new AirHockeyGdxGame(), config);   }
	}

	@Override
	public void exit() {
	}
}