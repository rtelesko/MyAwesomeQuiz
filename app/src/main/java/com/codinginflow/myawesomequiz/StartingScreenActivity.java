package com.codinginflow.myawesomequiz;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

// Code basis from Coding in flow: https://codinginflow.com/tutorials/android/quiz-app-with-sqlite/part-1-layouts
// Extension to save quiz data in a Firebase database

public class StartingScreenActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_QUIZ = 1;
    public static final String EXTRA_CATEGORY_ID = "extraCategoryID";
    public static final String EXTRA_CATEGORY_NAME = "extraCategoryName";
    public static final String EXTRA_DIFFICULTY = "extraDifficulty";

    private TextView textViewHighscore;
    private TextView textViewHighscoreDate;
    private TextView textViewHighscoreUser;
    private EditText editTextHighscoreUser;
    private Spinner spinnerCategory;
    private Spinner spinnerDifficulty;

    // Highscore, Date and the user
    private int highscore;
    private String highscoreDate;
    private static String user, highscoreUser;

    // Database references
    FirebaseDatabase database;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting_screen);
        textViewHighscore = findViewById(R.id.text_view_highscore);
        textViewHighscoreDate = findViewById(R.id.text_view_highscoredate);
        textViewHighscoreUser = findViewById(R.id.text_view_highscoreuser);

        editTextHighscoreUser = findViewById(R.id.edit_text_user);

        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerDifficulty = findViewById(R.id.spinner_difficulty);

        loadCategories();
        loadDifficultyLevels();
        loadHighscoreData();

        Button buttonStartQuiz = findViewById(R.id.button_start_quiz);
        buttonStartQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user = editTextHighscoreUser.getText().toString();
                if (!user.equals("")) {
                    startQuiz();
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter a user name!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startQuiz() {
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        int categoryID = selectedCategory.getId();
        String categoryName = selectedCategory.getName();
        String difficulty = spinnerDifficulty.getSelectedItem().toString();

        Intent intent = new Intent(StartingScreenActivity.this, QuizActivity.class);
        intent.putExtra(EXTRA_CATEGORY_ID, categoryID);
        intent.putExtra(EXTRA_CATEGORY_NAME, categoryName);
        intent.putExtra(EXTRA_DIFFICULTY, difficulty);
        startActivityForResult(intent, REQUEST_CODE_QUIZ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_QUIZ) {
            if (resultCode == RESULT_OK) {
                int score = data.getIntExtra(QuizActivity.EXTRA_SCORE, 0);
                editTextHighscoreUser.getText().clear();        // Clear existing entry
                // Update Data
                if (score > highscore) {
                    updateHighscoreData(score);
                }
            }
        }
    }

    private void loadCategories() {
        QuizDbHelper dbHelper = QuizDbHelper.getInstance(this);
        List<Category> categories = dbHelper.getAllCategories();

        ArrayAdapter<Category> adapterCategories = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapterCategories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapterCategories);
    }

    private void loadDifficultyLevels() {
        String[] difficultyLevels = Question.getAllDifficultyLevels();

        ArrayAdapter<String> adapterDifficulty = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, difficultyLevels);
        adapterDifficulty.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapterDifficulty);
    }

    private void loadHighscoreData() {
        myRef = FirebaseDatabase.getInstance().getReference();
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    highscore = ds.child("highscore").getValue(Integer.class);
                    textViewHighscore.setText("Highscore: " + highscore);

                    highscoreDate = ds.child("date").getValue(String.class);
                    textViewHighscoreDate.setText("Date: " + highscoreDate);

                    highscoreUser = ds.child("user").getValue(String.class);
                    textViewHighscoreUser.setText("User: " + highscoreUser);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.i("telesko", "Failed to read value.", error.toException());
            }
        });
    }

    private void updateHighscoreData(int highscoreNew) {
        highscore = highscoreNew;
        highscoreUser = user;
        // Entries in Firebase Database
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("best");
        // Update highscore
        myRef.child("highscore").setValue(highscore);
        // Update Date (in GMT + 1)
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1")); // Sets the time zone to GMT + 1
        Date date = new Date();
        highscoreDate = dateFormat.format(date);
        myRef.child("date").setValue(highscoreDate);
        // Update User name
        myRef.child("user").setValue(highscoreUser);
    }
}