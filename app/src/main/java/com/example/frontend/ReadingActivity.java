package com.example.frontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;



public class ReadingActivity extends AppCompatActivity {

    private TextView paragraphTextView, expectedReadingTimeView , levelTextView ;
    private Chronometer chronometerTimer;
    private Button startStopButton, doneButton;
    private boolean isTimerRunning = false;
    private long timeElapsedMillis = 0;
    private int wordCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);
        Log.d("ReadingActivity", "onCreate: Activity created");
        if (isFirstTimeUser()) {
            initializeLevel(); // Initialize level to 1 for new users
        }
        paragraphTextView = findViewById(R.id.tvParagraph);
        expectedReadingTimeView = findViewById(R.id.expectedReadingTime);
        chronometerTimer = findViewById(R.id.chronometerTimer);
        startStopButton = findViewById(R.id.startStopButton);
        levelTextView = findViewById(R.id.levelTextView);
        doneButton = findViewById(R.id.doneButton);
//        ImageView imageView = findViewById(R.id.themePicture);

//        int l = retrieveLevel();
        Log.d("ReadingActivity", "Attempting to fetch ");
           fetchParagraphForAge();

        Log.d("ReadingActivity", "no activity fetched");

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTimer();
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                processReadingResult();
            }
        });
    }


private void fetchParagraphForAge() {
    int age = getUserAgeFromSharedPreferences();
    Log.d("InitialScreening", "Retrieved Age: " + age);
    int level = retrieveLevel();
    Log.d("InitialScreening", "Retrieved level: " + level);
    if (age == -1) {
        Log.e(" InitialScreening", "Age not found in Shared Preferences");
        return;
    }
    Log.d("InitialScreening", "Retrieved Age: " + age);
    Log.d("InitialScreening", "Retrieved Age: " + age);


    String url = " http://192.168.10.16:5000/get_paragraph";
    JSONObject postData = new JSONObject();
    try {
        postData.put("age", age);
        postData.put("level", 1); // Default level is 1
    } catch (JSONException e) {
        e.printStackTrace();
        Log.e("ReadingActivity", "JSON Exception", e);
    }
    levelTextView.setText("Level: " + level);
    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String paragraph = response.optString("paragraph");
                        String theme = response.optString("theme"); // Fetch the theme

                        // Set the background image based on the theme
                        int backgroundImageResource, backgroundTint;
                        switch (theme) {
                            case "Ocean":
                                backgroundImageResource = R.drawable.ocean;
                                backgroundTint = Color.parseColor("#C69AA7C7");
                                break;
                            case "Space":
                                backgroundImageResource = R.drawable.space;
                                backgroundTint = Color.parseColor("#EBE3DA");
                                break;
                            case "Park":
                                backgroundImageResource = R.drawable.park;
                                backgroundTint = Color.parseColor("#C69AA7C7");
                                break;
                            default:
                                backgroundImageResource = R.drawable.ocean;
                                backgroundTint = Color.parseColor("#C69AA7C7");
                                break;
                        }

                        paragraphTextView.setText(paragraph);
                        ImageView imageView = findViewById(R.id.themePicture);
                        imageView.setBackgroundResource(backgroundImageResource);

                        // Set the background tint of the TextView
                        TextView tvParagraph = findViewById(R.id.tvParagraph);
                        tvParagraph.setBackgroundTintList(ColorStateList.valueOf(backgroundTint));

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("InitialScreening", "Error parsing response", e);
                    }
                }
            }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            error.printStackTrace();
            Log.e("ReadingActivity", "Error Response", error);
        }
    });

    Volley.newRequestQueue(this).add(jsonObjectRequest);
}


    private void toggleTimer() {
        if (isTimerRunning) {
            chronometerTimer.stop();
            timeElapsedMillis = SystemClock.elapsedRealtime() - chronometerTimer.getBase();
            startStopButton.setText("Start");
        } else {
            chronometerTimer.setBase(SystemClock.elapsedRealtime() - timeElapsedMillis);
            chronometerTimer.start();
            startStopButton.setText("Stop");
        }
        isTimerRunning = !isTimerRunning;
    }



    private void processReadingResult() {
        int readingSpeed = calculateReadingSpeed(wordCount, timeElapsedMillis);
        int age = getUserAgeFromSharedPreferences();
        Log.d("ReadingResult", "Age: " + age);

//        boolean isNormal = isReadingAbilityNormal(age , readingSpeed);
        String resultMessage;
        boolean isNormal = true ;

        if (isNormal) {
            resultMessage = "Reading ability is within the normal range for your age group";
            greatJob();
            incrementLevel();// Increment level and save it

            fetchParagraphForAge(); // Fetch a new paragraph for the incremented level
        } else {
            resultMessage = "Reading speed is outside the normal range for your age group";
            showTryAgain();

            fetchParagraphForAge(); // Fetch a new paragraph for the current level
        }
    }

    // Storing the user's level when a new user logs in or when the level is incremented
//    private void incrementLevel() {
//        SharedPreferences sharedPreferences = getSharedPreferences("LevelData", MODE_PRIVATE);
//        int currentLevel = sharedPreferences.getInt("Level", 1); // Default level is 1
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putInt("Level", currentLevel + 1); // Increment the level
//        editor.apply();
//    }
    // Increment the user's level, up to a maximum of level 4
    private void incrementLevel() {
        SharedPreferences sharedPreferences = getSharedPreferences("LevelData", MODE_PRIVATE);
        int currentLevel = sharedPreferences.getInt("Level", 1); // Default level is 1
        if (currentLevel < 4) { // Check if current level is less than 4
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("Level", currentLevel + 1); // Increment the level
            editor.apply();
        }
        else {
            LevelCompleted();
        }

    }

    private boolean isFirstTimeUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("LevelData", MODE_PRIVATE);
        return !sharedPreferences.contains("Level"); // If "Level" key doesn't exist, it's a new user
    }
    // Retrieving the user's level from SharedPreferences
    private int retrieveLevel() {
        SharedPreferences sharedPreferences = getSharedPreferences("LevelData", MODE_PRIVATE);
        return sharedPreferences.getInt("Level", 1); // Default level is 1
    }
    // Initialize the user's level to 1 when a new user logs in
    private void initializeLevel() {
        SharedPreferences sharedPreferences = getSharedPreferences("LevelData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("Level", 1); // Set the level to 1 for new users
        editor.apply();
    }

    private void greatJob() {
        LayoutInflater inflater = getLayoutInflater();
        View splashView = inflater.inflate(R.layout.great, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(splashView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Dismiss the splash screen after a short delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 2000); // Adjust the delay time as needed
    }
    private void LevelCompleted() {
        LayoutInflater inflater = getLayoutInflater();
        View splashView = inflater.inflate(R.layout.completelevels, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(splashView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Dismiss the splash screen after a short delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 2000); // Adjust the delay time as needed
    }
    private void showTryAgain() {
        LayoutInflater inflater = getLayoutInflater();
        View splashView = inflater.inflate(R.layout.tryagain, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(splashView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Dismiss the splash screen after a short delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 2000); // Adjust the delay time as needed
    }


    private int calculateReadingSpeed(int wordCount, long timeElapsedMillis) {
        double timeElapsedMinutes = timeElapsedMillis / 60000.0;
        return (int) (wordCount / timeElapsedMinutes);
    }

    private boolean isReadingAbilityNormal(int age, int readingSpeed) {
        if (age >= 4 && age <= 6) {
            return readingSpeed >= 60 && readingSpeed <= 80;
        } else if (age >= 7 && age <= 8) {
            return readingSpeed >= 115 && readingSpeed <= 138;
        } else if (age >= 9 && age <= 12) {
            return readingSpeed >= 158 && readingSpeed <= 185;
        }
        return true;
    }

    private void displayExpectedReadingTime(int wordCount) {
        int age = getUserAgeFromSharedPreferences();
        double averageSpeed = getAverageSpeedForAgeGroup(age);
        double expectedTime = wordCount / averageSpeed;
        double expectedTimeSeconds = expectedTime * 60;
        expectedReadingTimeView.setText(String.format("Expected reading time: %.2f seconds", expectedTimeSeconds));
    }

    private double getAverageSpeedForAgeGroup(int age) {
        if (age >= 4 && age <= 6) {
            return 70.0; // Average of 60 and 80
        } else if (age >= 7 && age <= 8) {
            return 126.5; // Average of 115 and 138
        } else if (age >= 9 && age <= 12) {
            return 171.5; // Average of 158 and 185
        }
        return 0;
    }

    private int getUserAgeFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
        return sharedPreferences.getInt("UserAge", -1);
    }
}




