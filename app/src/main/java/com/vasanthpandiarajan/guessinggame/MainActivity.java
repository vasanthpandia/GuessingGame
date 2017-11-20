package com.vasanthpandiarajan.guessinggame;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
    final int START_GAME = 1;
    final int END_GAME = 0;
    final int PLAYER1_SET_NUMBER = 2;
    final int PLAYER2_SET_NUMBER = 3;
    final int PLAYER1_RESPONSE = 4;
    final int PLAYER2_RESPONSE = 5;
    final int GET_GUESS = 6;
    final int MAKE_GUESS = 7;

    List<Integer> digits;

    HandlerThread player1, player2;
    public Handler myHandler, player1Handler, player2Handler;
    final Random rnd = new Random();
    static final Object lock = new Object();
    int player1Number, player2Number, player1Guess, player2Guess, guessCount1, guessCount2;
    String player1Response, player2Response;
    boolean player1State, player2State;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        digits = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            digits.add(i);
        }

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
                        player1Handler.sendEmptyMessage(MAKE_GUESS);
                        player2Handler.sendEmptyMessage(MAKE_GUESS);
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
                    case END_GAME: {
                        player1.interrupt();
                        player1.quit();
                        player2.interrupt();
                        player2.quit();
                        addTextToScrollView(R.id.scroll1_layout, "The winner is Player :" + msg.arg1);
                        addTextToScrollView(R.id.scroll2_layout, "The winner is Player :" + msg.arg1);
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

                resetView();

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
                                    String response = checkGuess(opponentGuess, player1Number);
                                    message = player2Handler.obtainMessage(PLAYER1_RESPONSE);
                                    message.arg1 = opponentGuess;
                                    message.obj = response;
                                    try {
                                        Thread.sleep(500);
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
                                    if(guessCount1 >= 19) {
                                        player1.interrupt();
                                        player1.quit();
                                    }
                                    message = player2Handler.obtainMessage(GET_GUESS);
                                    player1Guess = player1MakeGuess();
                                    message.arg1 = player1Guess;
                                    try {
                                        Thread.sleep(500);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player2Handler.sendMessage(message);
                                    break;
                                }
                            }
                            case PLAYER2_RESPONSE: {
                                synchronized (lock) {
                                    player2Response = "Player 1 Guessed " + player1Guess + " : Player2 responded " + msg.obj;
                                    try {
                                        Thread.sleep(500);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    myHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.this.addTextToScrollView(R.id.scroll1_layout, player2Response);
                                        }
                                    });
                                    if(msg.obj.equals("Wrong Guess")) {
                                        message = player1Handler.obtainMessage(MAKE_GUESS);
                                        player1Handler.sendMessage(message);
                                    } else if(msg.obj.equals("Correct Guess")) {
                                        message = myHandler.obtainMessage(END_GAME);
                                        message.arg1 = 1;
                                        myHandler.sendMessage(message);
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
                                    String response = checkGuess(opponentGuess, player2Number);
                                    message = player1Handler.obtainMessage(PLAYER2_RESPONSE);
                                    message.arg1 = opponentGuess;
                                    message.obj = response;
                                    try {
                                        Thread.sleep(500);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player1Handler.sendMessage(message);
                                    break;
                                }

                            }
                            case MAKE_GUESS: {
                                synchronized (lock) {
                                    if(guessCount2 >= 19) {
                                        player2.interrupt();
                                        player2.quit();
                                    }
                                    message = player1Handler.obtainMessage(GET_GUESS);
                                    player2Guess = player2MakeGuess();
//                                    player2Guess = player1Number;
                                    message.arg1 = player2Guess;
                                    try {
                                        Thread.sleep(500);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    player1Handler.sendMessage(message);
                                    break;
                                }
                            }
                            case PLAYER1_RESPONSE: {
                                synchronized(lock) {
                                    player1Response = "Player 2 Guessed " + player2Guess + " : Player1 responded " + msg.obj;
                                    try {
                                        Thread.sleep(500);
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    myHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.this.addTextToScrollView(R.id.scroll2_layout, player1Response);
                                        }
                                    });
                                    if(msg.obj.equals("Wrong Guess")) {
                                        message = player2Handler.obtainMessage(MAKE_GUESS);
                                        player2Handler.sendMessage(message);
                                    } else if(msg.obj.equals("Correct Guess")) {
                                        message = myHandler.obtainMessage(END_GAME);
                                        message.arg1 = 2;
                                        myHandler.sendMessage(message);
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
        Collections.shuffle(digits);
        while(digits.get(0) == 0) {
            Collections.shuffle(digits);
        }
        int result = 0;
        for(int i = 3; i >= 0; i--) {
            result += (int) (digits.get(i) * Math.pow(10, i));
        }
        return result;
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
        TextView playerNumber = findViewById(view_id);
        playerNumber.setText(""+number);
    }

    public void addTextToScrollView(int view_id, String displayText) {
        LinearLayout scrollView = findViewById(view_id);
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView displayTextField = new TextView(this);
        displayTextField.setLayoutParams(lparams);
        displayTextField.setText(displayText);
        displayTextField.setTextSize(22);
        scrollView.addView(displayTextField);
    }

    public String checkGuess(int guess, int originalGuess) {
        if(guess == originalGuess) {
            return "Correct Guess";
        } else {
            return "Wrong Guess";
        }
    }

    public int player1MakeGuess() {
        guessCount1 ++;
        return getRandomNumber();
    }

    public int player2MakeGuess() {
        guessCount2 ++;
        return getRandomNumber();
    }

    public void resetView() {
        TextView playerNumber1 = findViewById(R.id.player1_number_field);
        TextView playerNumber2 = findViewById(R.id.player2_number_field);
        player1Number = -1;
        player2Number = -1;
        player2Guess = -1;
        player1Guess = -1;
        player1Response = player2Response = "";
        guessCount1 = guessCount2 = -1;
        LinearLayout layout1 =  findViewById(R.id.scroll1_layout);
        layout1.removeAllViews();
        LinearLayout layout2 =  findViewById(R.id.scroll2_layout);
        layout2.removeAllViews();
        playerNumber1.setText("Not Set");
        playerNumber2.setText("Not Set");
    }
}
