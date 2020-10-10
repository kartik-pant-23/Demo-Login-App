package com.example.demologinapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Button linkedInSignIn;
    private LoginButton facebookSignIn;
    private TextView info;
    private ImageView profile;

    private CallbackManager callbackManager;
    private ApiCalls apiCalls;
    private Retrofit LinkedIn, Facebook;
    private OkHttpClient.Builder httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if(intent != null && intent.getData()!=null) {
            String _code = intent.getData().getQueryParameter("code");
            Log.i("TAG", _code);
            _initializeLinkedInUser(_code);
        }

        linkedInSignIn = findViewById(R.id.signInWithLinkedInBtn);
        facebookSignIn = findViewById(R.id.signInWithFacebookBtn);
        info = findViewById(R.id.info);
        profile = findViewById(R.id.profile);

        // Facebook retrofit object to get details
        /*Facebook = new Retrofit.Builder()
                .baseUrl("https://graph.facebook.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiCalls = Facebook.create(ApiCalls.class);*/

        callbackManager = CallbackManager.Factory.create();

        facebookSignIn.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                getFbDetails(loginResult);
            }

            @Override
            public void onCancel() {
                Toast.makeText(MainActivity.this, "Unknown error occurred!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        linkedInSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithLinkedIn();
            }
        });

    }

    private void _initializeLinkedInUser(String code) {

        setupRetrofit("Content-Type", "application/x-www-form-urlencoded");
        apiCalls = LinkedIn.create(ApiCalls.class);
        Call<LinkedInAccessDetails> call = apiCalls.getAccessToken(getResources().getString(R.string.linkedInClientID),
                getResources().getString(R.string.linkedInClientSecret),
                "authorization_code",
                getResources().getString(R.string.redirectedUri),
                code
                );
        call.enqueue(new Callback<LinkedInAccessDetails>() {
            @Override
            public void onResponse(Call<LinkedInAccessDetails> call, retrofit2.Response<LinkedInAccessDetails> response) {
                if(!response.isSuccessful()){
                    Log.i("TAG", String.valueOf(response.code()));
                    return;
                }
                String accessToken = response.body().getAccess_token();
                getLinkedInDetails(accessToken);
            }

            @Override
            public void onFailure(Call<LinkedInAccessDetails> call, Throwable t) {
                Log.i("TAG", Objects.requireNonNull(t.getMessage()));
            }
        });

    }

    private void getLinkedInDetails(String _accessToken) {

        LinkedIn = new Retrofit.Builder()
                .baseUrl("https://api.linkedin.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiCalls = LinkedIn.create(ApiCalls.class);
        Call<String> call = apiCalls.getUserDetails(
                "(id,localizedFirstName,localizedLastName, profilePicture(displayImage~:playableStreams))",
                _accessToken
        );
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                if(!response.isSuccessful()){
                    Log.i("TAG", "Error from retrofit2, code: "+response.code());
                    return;
                }
                try {
                    JSONObject object = new JSONObject(response.body());
                    String _firstName = object.getString("localizedFirstName");
                    String _lastName = object.getString("localizedLastName");
                    JSONObject profilePicture = object.getJSONObject("profilePicture");
                    JSONArray arrayIdentifiers = profilePicture.getJSONObject("displayImage~")
                            .getJSONArray("elements").getJSONObject(0)
                            .getJSONArray("identifiers");
                    String _photoUrl = arrayIdentifiers.getJSONObject(0).getString("identifier");
                    info.append(_firstName+" "+_lastName);
                    Picasso.get().load(_photoUrl).into(profile);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });

        /*try{
            URL url = new URL("https://api.linkedin.com/v2/me");
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + _accessToken);
            con.setRequestProperty("cache-control", "no-cache");
            con.setRequestProperty("X-Restli-Protocol-Version", "2.0.0");

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            Log.i("TAG", jsonString.toString());
        } catch (Exception e) {
            Log.i("TAG", "Error");
        }*/

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    void getFbDetails(final LoginResult _loginResult) {
        GraphRequest request = GraphRequest.newMeRequest(_loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                try {
                    String _name = response.getJSONObject().getString("name");
                    String _photoUrl = response.getJSONObject().getJSONObject("picture").getJSONObject("data").getString("url");
                    info.append(_name+"\n");
                    Picasso.get().load(_photoUrl).into(profile);
                } catch(Exception e) {
                    Log.i("TAG", response.toString());
                }
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,link,email,picture");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void signInWithLinkedIn() {
        Intent linkedInIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.linkedin.com/oauth/v2/authorization"+
                        "?response_type=code"+
                        "&client_id="+getResources().getString(R.string.linkedInClientID)+
                        "&redirect_uri="+getResources().getString(R.string.redirectedUri)+
                        "&scope=r_liteprofile"+
                        "&state=SECRET"));
        startActivity(linkedInIntent);
    }

    private void setupRetrofit(final String headerKey, final String headerValue) {
        httpClient = new OkHttpClient.Builder();

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder()
                        .addHeader(headerKey, headerValue)
                        .build();
                return chain.proceed(request);
            }
        });
        LinkedIn = new Retrofit.Builder()
                .baseUrl("https://www.linkedin.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
    }
}