package com.vasanthpandiarajan.guessinggame;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    final int START_GAME = 1;
    final int PLAYER1_GUESS = 2;
    final int PLAYER2_GUESS = 3;
    final int PLAYER1_SET_NUMBER = 4;
    final int PLAYER2_SET_NUMBER = 5;
    final int NEW_GUESS = 6;
    final int PLAYER1_RESPONSE = 7;
    final int PLAYER2_RESPONSE = 8;
    final int GET_GUESS = 9;
    final int MAKE_GUESS = 10;

    HandlerThread player1, player2;
    public Handler myHandler, player1Handler, player2Handler;
    final Random rnd = new Random();
    static final Object lock = new Object();
    int player1Number, player2Number, player1Guess, player2Guess, guessCount1, guessCount2;
    boolean player1State, player2State;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // UIThread Handler
        myHandler = new Handler() {

            @Override //Receives Message and Runnables from player1 and player2 worker Threads
            public void handleMessage(Message msg) {
                Log.i("Message received", "" +msg.what);
                Message message;
                switch(msg.what) {
                    case START_GAME: {
                        Message msg1 = player1Handler.obtainMessage(START_GAME);
                        Message msg2 = player2Handler.obtainMessage(START_GAME);
                        player1Handler.sendMessage(msg1);
                        player2Handler.sendMessage(msg2);
                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                        player1Handler.sendEmptyMessage(MAKE_GUESS);
                        player2Handler.sendEmptyMessage(MAKE_GUESS);
                        break;
                    }
                    case PLAYER1_GUESS: {
                        Log.i("Reached Turn1", "Turn1");
                        addTextToScrollView(R.id.scroll1_layout, ""+msg.arg1);
                        addTextToScrollView(R.id.scroll1_layout, ""+msg.obj);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        break;
                    }
                    case PLAYER2_GUESS: {
                            Log.i("Reached Turn2", "Turn2");
                            addTextToScrollView(R.id.scroll2_layout, "" + msg.arg1);
                            addTextToScrollView(R.id.scroll2_layout, "" + msg.obj);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        break;
                    }
                    case PLAYER1_SET_NUMBER: {
                        displayPlayerNumber(R.id.player1_number_field, player1Number);
                        break;
                    }
                    case PLAYER2_SET_NUMBER: {
                        displayPlayerNumber(R.id.player2_number_field, player2Number);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        };


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guessCount1 = guessCount2 = 0;

                player1 = new HandlerThread("player1");
                player1.start();
                player2 = new HandlerThread("player2");
                player2.start();

                player1Handler = new Handler(player1.getLooper()){
                    @Override
                    public void handleMessage(Message msg) {
                        Message message;
                        switch(msg.what) {
                            case START_GAME: {
                                synchronized(lock) {
                                    player1Number = getRandomNumber();
                                    message = myHandler.obtainMessage(PLAYER1_SET_NUMBER);
                                    myHandler.sendMessage(message);
                                    break;
                                }
                            }
                            case GET_GUESS: {
                                synchronized (lock) {
                                    int opponentGuess = msg.arg1;
                                    String response = "";
                                    if(opponentGuess != player1Guess) {
                                        response = "Wrong Guess";
                                    } else {
                                        response = "Correct Guess";
                                    }
                                    message = player2Handler.obtainMessage(PLAYER1_RESPONSE);
                                    message.arg1 = opponentGuess;
                                    message.obj = response;
                                    try {
                                        Thread.sleep(1000);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player2Handler.sendMessage(message);
                                    break;
                                }

                            }
                            case MAKE_GUESS: {
                                synchronized(lock) {
                                    Log.i("Player1", "Make Guess");
                                    message = player2Handler.obtainMessage(GET_GUESS);
                                    player1Guess = getRandomNumber();
                                    message.arg1 = player1Guess;
                                    try {
                                        Thread.sleep(1000);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player2Handler.sendMessage(message);
                                    break;
                                }
                            }
                            case PLAYER2_RESPONSE: {
                                synchronized (lock) {
                                    message = myHandler.obtainMessage(PLAYER1_GUESS);
                                    message.arg1 = player1Guess;
                                    message.obj = msg.obj;
                                    try {
                                        Thread.sleep(1000);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    myHandler.sendMessage(message);
                                    if(msg.obj.equals("Wrong Guess")) {
                                        message = player1Handler.obtainMessage(MAKE_GUESS);
                                        player1Handler.sendMessage(message);
                                    }
                                    break;
                                }

                            }
                        }
                    }
                };

                player2Handler = new Handler(player2.getLooper()){
                    @Override
                    public void handleMessage(Message msg) {
                        Message message;
                        switch(msg.what) {
                            case START_GAME: {
                                player2Number = getRandomNumber();
                                message = myHandler.obtainMessage(PLAYER2_SET_NUMBER);
                                myHandler.sendMessage(message);
                                break;
                            }
                            case GET_GUESS: {
                                synchronized (lock) {
                                    int opponentGuess = msg.arg1;
                                    String response = "";
                                    if(opponentGuess != player2Guess) {
                                        response = "Wrong Guess";
                                    } else {
                                        response = "Correct Guess";
                                    }
                                    message = player1Handler.obtainMessage(PLAYER2_RESPONSE);
                                    message.arg1 = opponentGuess;
                                    message.obj = response;
                                    try {
                                        Thread.sleep(1000);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player1Handler.sendMessage(message);
                                    break;
                                }

                            }
                            case MAKE_GUESS: {
                                synchronized (lock) {
                                    message = player1Handler.obtainMessage(GET_GUESS);
                                    player2Guess = getRandomNumber();
                                    message.arg1 = player2Guess;
                                    try {
                                        Thread.sleep(1000);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player1Handler.sendMessage(message);
                                    break;
                                }
                            }
                            case PLAYER1_RESPONSE: {
                                synchronized(lock) {
                                    message = myHandler.obtainMessage(PLAYER2_GUESS);
                                    message.arg1 = player2Guess;
                                    message.obj = msg.obj;
                                    try {
                                        Thread.sleep(1000);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    myHandler.sendMessage(message);
                                    if(msg.obj.equals("Wrong Guess")) {
                                        message = player2Handler.obtainMessage(MAKE_GUESS);
                                        player2Handler.sendMessage(message);
                                    }
                                    break;
                                }

                            }
                        }
                    }
                };

                myHandler.sendEmptyMessage(START_GAME);
            }
        });

    }

    public int getRandomNumber() {
        int n;
        int Low = 1000;
        int High = 10000;
        do {

            n = rnd.nextInt(High - Low) + Low;
        } while(containsRepeatingDigits(n));
        return n;
    }

    public boolean containsRepeatingDigits(final int n) {
        final boolean digits[] = new boolean[10];
        for(char c : String.valueOf(n).toCharArray()) {
            final int i = c-'0';
            if(digits[i])
                return true;
            digits[i] = true;
        }
        return false;
    }

    public void displayPlayerNumber(int view_id, int number) {
        TextView playerNumber = (TextView) findViewById(view_id);
        playerNumber.setText(""+number);
    }

    public void addTextToScrollView(int view_id, String displayText) {
        LinearLayout scrollView = (LinearLayout) findViewById(view_id);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView displayTextField = new TextView(this);
        displayTextField.setLayoutParams(lparams);
        displayTextField.setText(displayText);
        displayTextField.setTextSize(26);
        scrollView.addView(displayTextField);
    }

}
