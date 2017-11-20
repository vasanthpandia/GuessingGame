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
    final int PLAYER1_TURN = 2;
    final int PLAYER2_TURN = 3;
    final int PLAYER1_SET_NUMBER = 4;
    final int PLAYER2_SET_NUMBER = 5;
    final int NEW_GUESS = 6;

    public Handler myHandler, player1Handler, player2Handler;
    final Random rnd = new Random();
    static final Object lock = new Object();

    public Thread player1 = new TPlayer1("player1");
    public Thread player2 = new TPlayer2("player2");

    public class TPlayer1 extends HandlerThread {
        int currentGuess1, number1;

        public TPlayer1(String name) {
            super(name);
        }

        public void onLooperPrepared() {
            player1Handler = new Handler(getLooper()) {
              @Override
                public void handleMessage(Message msg) {
                  Message message1;
                  switch(msg.what) {
                      case START_GAME: {
                          message1 = player2Handler.obtainMessage(NEW_GUESS);
                          currentGuess1 = getRandomNumber();
                          message1.arg1 = currentGuess1;
                          player1Handler.sendMessage(message1);
                          break;
                      }
                      case NEW_GUESS: {
                          synchronized (lock) {
                              message1 = myHandler.obtainMessage(PLAYER1_TURN);
                              message1.arg1 = msg.arg1;
                              myHandler.sendMessage(message1);
                          }
                          break;
                      }
                  }
              }
            };
        }



        @Override
        public void run() {
            super.run();
            number1 = getRandomNumber();
            Message message = myHandler.obtainMessage(PLAYER1_SET_NUMBER);
            message.arg1 = number1;
            myHandler.sendMessage(message);

            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            currentGuess1 = getRandomNumber();
            Message newGuessMessage = player2Handler.obtainMessage(NEW_GUESS);
            newGuessMessage.arg1 = currentGuess1;
            player2Handler.sendMessage(newGuessMessage);
        }
    }

    public class TPlayer2 extends HandlerThread {
        int currentGuess2, number2;

        public TPlayer2(String name) {
            super(name);
        }

        public void onLooperPrepared() {
            player2Handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Message message;
                    switch(msg.what) {
                        case START_GAME: {
                            message = player1Handler.obtainMessage(NEW_GUESS);
                            currentGuess2 = getRandomNumber();
                            message.arg1 = currentGuess2;
                            player1Handler.sendMessage(message);
                            myHandler.sendEmptyMessage(START_GAME);
                            break;
                        }
                        case NEW_GUESS: {
                            synchronized (lock) {
                                message = myHandler.obtainMessage(PLAYER2_TURN);
                                message.arg1 = msg.arg1;
                                myHandler.sendMessage(message);
                            }
                            break;
                        }
                    }
                }
            };
        }


        public void run() {
            super.run();

            number2 = getRandomNumber();
            Message message = myHandler.obtainMessage(PLAYER2_SET_NUMBER);
            message.arg1 = number2;
            myHandler.sendMessage(message);

            currentGuess2 = getRandomNumber();
            Message newGuessMessage = player1Handler.obtainMessage(NEW_GUESS);
            newGuessMessage.arg1 = currentGuess2;
            player1Handler.sendMessage(newGuessMessage);
        }
    }

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
                        player1Handler.sendMessage(msg1);
                        break;
                    }
                    case PLAYER1_TURN: {
                        Log.i("Turn1", "I'm inside");
                        addTextToScrollView(R.id.scroll2_layout, ""+msg.arg1);
                        break;
                    }
                    case PLAYER2_TURN: {
                        addTextToScrollView(R.id.scroll1_layout, ""+msg.arg1);
                        break;
                    }
                    case PLAYER1_SET_NUMBER: {
                        displayPlayerNumber(R.id.player1_number_field, msg.arg1);
                        break;
                    }
                    case PLAYER2_SET_NUMBER: {
                        displayPlayerNumber(R.id.player2_number_field, msg.arg1);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        };

//        player1.start();
//        player2.start();


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (player1!=null && player2!=null){
//                    player1.interrupt();
//                    player2.interrupt();
//                    myHandler.removeCallbacksAndMessages(null);
//                    player1Handler.removeCallbacksAndMessages(null);
//                    player2Handler.removeCallbacksAndMessages(null);
//                    player1Handler.getLooper().quitSafely();
//                    player2Handler.getLooper().quitSafely();
//                }

                player1.start();
                player2.start();
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
        scrollView.addView(displayTextField);
    }

}
