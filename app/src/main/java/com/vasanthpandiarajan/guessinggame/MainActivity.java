package com.vasanthpandiarajan.guessinggame;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends Activity {
    final int START_GAME = 1;
    final int PLAYER1_TURN = 2;
    final int PLAYER2_TURN = 3;
    final int PLAYER1_SET_NUMBER = 4;
    final int PLAYER2_SET_NUMBER = 5;

    public Handler myHandler, player1Handler, player2Handler;
    Thread player1, player2;
    final Random rnd = new Random();

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
                        break;
                    }
                    case PLAYER1_TURN: {
                        break;
                    }
                    case PLAYER2_TURN: {
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

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Player1 worker thread
                player1 = new Thread(new Runnable() {
                    int number1, turn1;
                   @Override
                    public void run() {
                       try {
                           Thread.sleep(1000);
                           number1 = getRandomNumber();
                           Message message = myHandler.obtainMessage(PLAYER1_SET_NUMBER);
                           message.arg1 = number1;
                           myHandler.sendMessage(message);
                       } catch (InterruptedException e) {
                           e.printStackTrace();
                       }


                       Looper.prepare();

                       player1Handler = new Handler() {
                           @Override
                           public void handleMessage(Message msg1) {
                               Message message1 = msg1;
                               myHandler.sendMessage(message1);

                           }
                       };
                   }
                });

                player2 = new Thread(new Runnable() {
                    int number2, turn2;
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            number2 = getRandomNumber();
                            Message message = myHandler.obtainMessage(PLAYER2_SET_NUMBER);
                            message.arg1 = number2;
                            myHandler.sendMessage(message);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

//                        Looper.prepare();
                    }
                });
                player1.start();
                player2.start();
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
}
