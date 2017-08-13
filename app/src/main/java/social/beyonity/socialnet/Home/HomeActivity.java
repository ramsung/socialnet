package social.beyonity.socialnet.Home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;

import social.beyonity.socialnet.Login.LoginActivity;
import social.beyonity.socialnet.R;
import social.beyonity.socialnet.Utils.BottomNavigationViewHelper;
import social.beyonity.socialnet.Utils.SectionsPagerAdapter;
import social.beyonity.socialnet.Utils.UniversalImageLoader;

public class HomeActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

	private static final String TAG = "HomeActivity";
	private static final int ACTIVITY_NUM = 0;
	GoogleApiClient mGoogleApiClient;

	private Context mContext = HomeActivity.this;

	//firebase
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener mAuthListener;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		Log.d(TAG, "onCreate: starting.");
		FirebaseApp.initializeApp(getApplicationContext());
		setupFirebaseAuth();

		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id))
				.requestEmail()
				.build();
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

					}
				} )
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build();


		initImageLoader();
		setupBottomNavigationView();
		setupViewPager();


	}


	private void initImageLoader() {
		UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
		ImageLoader.getInstance().init(universalImageLoader.getConfig());
	}

	/**
	 * Responsible for adding the 3 tabs: Camera, Home, Messages
	 */
	private void setupViewPager() {
		SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
		adapter.addFragment(new CameraFragment()); //index 0
		adapter.addFragment(new HomeFragment()); //index 1
		adapter.addFragment(new MessagesFragment()); //index 2
		ViewPager viewPager = (ViewPager) findViewById(R.id.container);
		viewPager.setAdapter(adapter);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(viewPager);

		tabLayout.getTabAt(0).setIcon(R.drawable.ic_camera);
		tabLayout.getTabAt(1).setIcon(R.drawable.ic_action_name);
		tabLayout.getTabAt(2).setIcon(R.drawable.ic_arrow);
	}

	/**
	 * BottomNavigationView setup
	 */
	private void setupBottomNavigationView() {
		Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
		BottomNavigationViewEx bottomNavigationViewEx = (BottomNavigationViewEx) findViewById(R.id.bottomNavViewBar);
		BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationViewEx);
		BottomNavigationViewHelper.enableNavigation(mContext, bottomNavigationViewEx);
		Menu menu = bottomNavigationViewEx.getMenu();
		MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
		menuItem.setChecked(true);
	}


     /*
    ------------------------------------ Firebase ---------------------------------------------
     */

	/**
	 * checks to see if the @param 'user' is logged in
	 *
	 * @param user
	 */
	private void checkCurrentUser(FirebaseUser user) {
		Log.d(TAG, "checkCurrentUser: checking if user is logged in.");

		if (user == null) {
			Intent intent = new Intent(mContext, LoginActivity.class);
			startActivity(intent);
		}
	}

	/**
	 * Setup the firebase auth object
	 */
	private void setupFirebaseAuth() {
		Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

		mAuth = FirebaseAuth.getInstance();

		mAuthListener = new FirebaseAuth.AuthStateListener() {
			@Override
			public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
				FirebaseUser user = firebaseAuth.getCurrentUser();

				//check if the user is logged in
				checkCurrentUser(user);

				if (user != null) {
					// User is signed in
					Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
				} else {
					// User is signed out
					Log.d(TAG, "onAuthStateChanged:signed_out");
				}
				// ...
			}
		};



	}

	@Override
	public void onStart() {
		super.onStart();
		mAuth.addAuthStateListener(mAuthListener);
		checkCurrentUser(mAuth.getCurrentUser());
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mAuthListener != null) {
			mAuth.removeAuthStateListener(mAuthListener);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);

		if (requestCode == 1) {
			Log.e("test", String.valueOf(requestCode));
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

			handleSignInResult(result);
		} else {

		}
	}

	private void handleSignInResult(GoogleSignInResult result) {
		Log.d("Sign In", "handleSignInResult:" + result.isSuccess());
		if (result.isSuccess()) {
			// Signed in successfully, show authenticated UI.
			GoogleSignInAccount acct = result.getSignInAccount();
			Log.i("user Name", acct.getDisplayName());


			firebaseAuthWithGoogle(acct);


		} else {


		}
	}

	private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
		Log.d("Sign in", "firebaseAuthWithGoogle:" + acct.getId());

		final AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
		mAuth.signInWithCredential(credential)
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.d("sign in", "signInWithCredential:onComplete:" + task.isSuccessful());
						//String pic = acct.getPhotoUrl().toString();
						//Toast.makeText(getApplicationContext(),pic,Toast.LENGTH_SHORT).show();
						// Picasso.with(getApplicationContext()).load(pic).into(profileImage);
						// If sign in fails, display a message to the user. If sign in succeeds
						// the auth state listener will be notified and logic to handle the
						// signed in user can be handled in the listener.
						if (task.isSuccessful()) {
							FirebaseUser user = mAuth.getCurrentUser();


						}

						//userEmailId.setText(user.getEmail());
						if (!task.isSuccessful()) {
							Log.w("Sign in", "signInWithCredential", task.getException());

						}
						// ...
					}
				});
	}

	public void signIn() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, 1);
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

	}
}
