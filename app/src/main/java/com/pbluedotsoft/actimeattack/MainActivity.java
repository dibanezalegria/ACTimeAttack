package com.pbluedotsoft.actimeattack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private final static String LOG = MainActivity.class.getSimpleName();

//    private static final String AC_SERVER_IP = "192.168.1.39";  /* AC Server */
    private static final String AC_SERVER_IP = "192.168.1.100";  /* AC Server */
    private static final int PORT = 9996;   /* AC UDP port */
    private WifiManager.MulticastLock mLock;    /* Allows app to receive Wifi multicast packets */
    private DatagramSocket mSocket;

    boolean mAppOn;     /* Helps pausing the PacketHandlerAsyncTask AsyncTask when onPause() */
    private boolean mChatON;     /* Handshake finished, client/server are connected */

    private PacketParser mParser;   /* Used by PacketHandlerAsyncTask */
    private Timer mTimer;   /* Timer runs the TimeTask that runs the AsyncTasks */

    private ListView mSessionListView;
    private TextView mCarTV, mTrackTV, mTopSpeedTV, mSpeedTV, mLaptimeTV, mLastlapTV,
            mBestlapTV, mRecordlapTV;

    private float mTopSpeed;
    private int mCurLap;     /* Current lap number */

    private List<Lap> mSessionLapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTrackTV = findViewById(R.id.track_tv);
        mCarTV = findViewById(R.id.car_tv);
        mTopSpeedTV = findViewById(R.id.topSpeed_tv);

        mSpeedTV = findViewById(R.id.speed_tv);
        mLaptimeTV = findViewById(R.id.laptime_tv);
        mLastlapTV = findViewById(R.id.lastlap_tv);
        mBestlapTV = findViewById(R.id.bestlap_tv);
        mRecordlapTV = findViewById(R.id.recordlap_tv);

        mTopSpeedTV.setTypeface(Typeface.MONOSPACE);
        mSpeedTV.setTypeface(Typeface.MONOSPACE);
        mLaptimeTV.setTypeface(Typeface.MONOSPACE);
        mLastlapTV.setTypeface(Typeface.MONOSPACE);
        mBestlapTV.setTypeface(Typeface.MONOSPACE);
        mRecordlapTV.setTypeface(Typeface.MONOSPACE);

        // Current lap number
        mCurLap = 0;

        // Session laptime list
        mSessionLapList = new ArrayList<>();
        mSessionListView = findViewById(R.id.session_list_view);
        mSessionListView.setEmptyView(findViewById(R.id.session_empty_list_view));

        // Restart button
        Button restartBtn = findViewById(R.id.restart_btn);
        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG, "Button pressed.");
                // Restart application (borrowed code from stack overflow)
                Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context
                        .ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            }
        });

        try {
            mSocket = new DatagramSocket(PORT);
            mSocket.setBroadcast(true);
            mAppOn = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(getApplicationContext(), "Is your WiFi on? Try changing your " +
                    "broadcast address in settings and restart.", Toast.LENGTH_LONG).show();
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null)
            mLock = wifiManager.createMulticastLock("lock");

        // Connect to AC server
        new HandshakeAsyncTask().execute();

        // Start packet receiver on client
        mParser = new PacketParser();
        TimerTask receiverTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (mAppOn)
                    new PacketHandlerAsyncTask().execute();  // AsyncTask class
            }
        };
        mTimer = new Timer();
        mTimer.schedule(receiverTimerTask, 0, 15); // run task every 50 ms
    }

    /**
     * INNER AsyncTask Classes
     */
    public class PacketHandlerAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private DatagramPacket packet;
        private byte[] message;

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (!mChatON) {
                return false;
            }

            mLock.acquire();
            message = new byte[1000];
            packet = new DatagramPacket(message, message.length);
//            Log.d(LOG, "Listening...");

            try {
                mSocket.receive(packet);
//                Log.d(LOG, "Response from AC (packet size): " + packet.getLength());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mLock.isHeld())
                    mLock.release();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean validPacket) {
            if (!validPacket) {
                return;
            }

            mParser.parse(packet.getData());
//            Log.d(LOG, mParser.toString());

            if (mParser.speed > mTopSpeed) {
                mTopSpeed = mParser.speed;
            }

            mSpeedTV.setText(String.format(Locale.ENGLISH, "%.0f", mParser.speed));
            mTopSpeedTV.setText(String.format(Locale.ENGLISH, "%.0f", mTopSpeed));
            mLaptimeTV.setText(Lap.format(mParser.lapTime));
            mLastlapTV.setText(Lap.format(mParser.lastLap));
            mBestlapTV.setText(Lap.format(mParser.bestLap));


            if (mCurLap != mParser.lapCount) {
                // Insert new laptime in list
                mSessionLapList.add(new Lap(mParser.lapCount, mParser.lastLap));
                Lap[] lapArr = mSessionLapList.toArray(new Lap[mSessionLapList.size()]);
                SessionLapAdapter adapter = new SessionLapAdapter(getApplicationContext(), lapArr);
                adapter.setBestLap(mParser.bestLap);
                Log.d(LOG, "Lap: " + mParser.lastLap + " Best: " + mParser.bestLap + " Gap: " +
                        Lap.format(mParser.lastLap - mParser.bestLap));
                mSessionListView.setAdapter(adapter);

                // Update database
                

                mTopSpeed = 0;
                mCurLap = mParser.lapCount;
            }

//            Log.d(LOG, "LapCount: " + mParser.lapCount);

        }
    }

    public class HandshakeAsyncTask extends AsyncTask<Void, Void, Boolean> {
        // Type of update required from the server
        private static final int SUBSCRIBE_UPDATE = 1;
        private static final int SUBSCRIBE_SPOT = 2;

        private byte[] identifier, version, operationId, hand;
        private HShakeParser parser;

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.d(LOG, "doInBackground HandshakeAsyncTask");
            // Prepare handshake packet
            identifier = intToLittleEndian(1);
            version = intToLittleEndian(1);
            operationId = intToLittleEndian(0);

            hand = appendByteArray(identifier, version);
            hand = appendByteArray(hand, operationId);

            mLock.acquire();
            try {
                Log.d(LOG, "doInBackground after try in HandshakeAsyncTask");
                // Contact AC Server
                InetAddress server = InetAddress.getByName(AC_SERVER_IP);
                DatagramPacket packet = new DatagramPacket(hand, hand.length, server,
                        PORT);
                mSocket.send(packet);
                Log.d(LOG, "Packet sent. Waiting for reply from AC server...");

                // AC Server responds
                byte[] message = new byte[4096];
                packet = new DatagramPacket(message, message.length);
                mSocket.receive(packet);
                parser = new HShakeParser();
                parser.parse(packet.getData());
                Log.d(LOG, "Response from AC: " + parser);

                // Client confirms connection
                operationId = intToLittleEndian(SUBSCRIBE_UPDATE);
                hand = appendByteArray(identifier, version);
                hand = appendByteArray(hand, operationId);
                packet = new DatagramPacket(hand, hand.length, server,
                        PORT);
                mSocket.send(packet);
                Log.d(LOG, "Client has been added as a listener to AC server.");
                mChatON = true;
                return true;

            } catch (SocketException e) {
                Log.d(LOG, "Socket Error:", e);
            } catch (IOException e) {
                Log.d(LOG, "IO Error:", e);
            } finally {
                if (mLock.isHeld())
                    mLock.release();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean validPacket) {
            if (!validPacket) {
                return;
            }
            mTrackTV.setText(parser.trackName);
            mCarTV.setText(parser.carName);
        }
    }

    /**
     * Help methods
     */
    public static byte[] appendByteArray(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public byte[] intToLittleEndian(int data) {
        byte[] b = new byte[4];
        b[0] = (byte) data;
        b[1] = (byte) ((data >> 8) & 0xFF);
        b[2] = (byte) ((data >> 16) & 0xFF);
        b[3] = (byte) ((data >> 24) & 0xFF);
        return b;
    }

}
