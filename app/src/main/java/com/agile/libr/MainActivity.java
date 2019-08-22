package com.agile.libr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.agile.mylibrary.CentralTendency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    RestService service;
    Tokencreds tokencreds;
    EditText et_user, et_password;
    Button btn_login;
    String loginToken, loginId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
git();
        sharedPreferences = getSharedPreferences("OAuth", Context.MODE_PRIVATE);
        sharedPreferences.getString("Login_Token", "NA");

        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CommenSettings.apiHostName)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(RestService.class);
        et_user = findViewById(R.id.et_user);
        et_password = findViewById(R.id.et_password);
        btn_login = findViewById(R.id.btn_login);
        getToken();

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticateuser();
              /*  Intent iMain = new Intent(LoginUserActivity.this, MainActivity.class);
                startActivity(iMain);
                finish();*/
            }
        });
    }

    private void git() {
        ArrayList<Integer> numbers = new ArrayList<>();
        Random r = new Random();

        for (int i = 0; i < 15; i++) {
            numbers.add(r.nextInt(7)+1);
        }

        double mean = CentralTendency.arithmeticMean(numbers).doubleValue();
        double median = CentralTendency.median(numbers);
        ArrayList<Integer> mode = CentralTendency.mode(numbers);

        Collections.sort(numbers);

        String res = String.format("Numbers:\n\n%s\nMean: %.1f\nMedian: %.1f\nMode: %s\n",
                numbers, mean, median, mode);
Log.e("res","res"+res);
        //textView.setText(res);
    }


    public void getToken() {
        RestService restService =
                ServiceGenerator.createService(RestService.class, "web_app", "changeit");
        Call<Tokencreds> call = restService.getToken("password", "openid", "admin", "admin");
        call.enqueue(new Callback<Tokencreds>() {
            @Override
            public void onResponse(Call<Tokencreds> call, Response<Tokencreds> response) {
                if (response.code() == 200) {
                    Log.e("Token1", response.body().toString());
                } else {

                    Toast.makeText(MainActivity.this, "resp Code" + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("Token2", response.raw().toString());
                    String responseHeaders = response.headers().get("Authorization");
                    tokencreds = new Tokencreds();
                    tokencreds.accessToken = responseHeaders;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Oauth_Token", tokencreds.accessToken);
                    editor.commit();

                    //CommenSettings.Oauth_Token = tokencreds.accessToken;

                }
            }

            @Override
            public void onFailure(Call<Tokencreds> call, Throwable t) {
                Log.d("Error", t.getMessage());
            }
        });
    }

    private void authenticateuser() {
        final String email = et_user.getText().toString().trim();
        String password = et_password.getText().toString().trim();
        User creds = new User();
        creds.setUsername(email);
        creds.setPassword(password);
        Log.d("Creds", "" + creds.toString());
        Call<Tokencreds> usercredentials = service.postlogin(sharedPreferences.getString("Oauth_Token", ""), creds);
        {
            usercredentials.enqueue(new Callback<Tokencreds>() {
                @Override
                public void onResponse(Call<Tokencreds> call, retrofit2.Response<Tokencreds> response) {
                    Log.d("Tokencreds***", "" + response.code());
                    Log.d("Login_Token", "" + response.body());
                    if (response.code() == 200) {


                        Log.e("Login_Token", response.body().toString());

                        String accessToken = response.body().getAccessToken();
                        final SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("Login_Token", "Bearer " + accessToken);
                        editor.putString("LoginId", email);
                        editor.commit();

                        SharedPreferences.Editor editor2 = getSharedPreferences("login", MODE_PRIVATE).edit();
                        editor2.putBoolean("loginval",true);

                        editor2.apply();

                        Intent iMain = new Intent(MainActivity.this, HomePage.class);
                        startActivity(iMain);
                        finish();

                    } else {
                        // error case
                        switch (response.code()) {
                            case 404:
                                Toast.makeText(MainActivity.this, "Service not found", Toast.LENGTH_SHORT).show();
                                break;
                            case 500:
                                Toast.makeText(MainActivity.this, "Server broken", Toast.LENGTH_SHORT).show();
                                break;
                            case 401:
                                Toast.makeText(MainActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    }
                }

                @Override
                public void onFailure(Call<Tokencreds> call, Throwable t) {
                    // UIUtil.hideDialog();
                }
            });
        }

    }

}
