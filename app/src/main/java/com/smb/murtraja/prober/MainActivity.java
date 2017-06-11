package com.smb.murtraja.prober;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class MainActivity extends Activity implements InteractionListener {

    /*
    This activity spawns 255 threads to send ICMP requests to clients
    Need a mechanism that informs me when all those 255 threads have
    returned successfully. in the current setting, this is not achievable
    because it requires joining all threads, which is a time consuming
    operation, so will have to spawn a new thread just to call join
    on other threads, so it is better to create a master thread
    and abstract the low level stuff from it.

    the job of the master thread will then be to inform the activity
    about the IP address it has found (and maybe simultaneously return
    MAC addresses also with it?)
     */

    Handler mHandler = new Handler();
    ArrayList<String> mReachableAddress = new ArrayList<>();
    ArrayList<Thread> mReachableAddressThread = new ArrayList<Thread>();
    Semaphore mUILock = new Semaphore(1);
    TextView mStatusTextView;
    Button mStartProbeButton;
    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusTextView = (TextView) findViewById(R.id.tv_status);
        mStartProbeButton = (Button) findViewById(R.id.btn_start_probe);
        mStartProbeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkHosts("192.168.0");
            }
        });
    }
    public void checkHosts(String subnet) {
        for (int i=1;i<255;i++){
            Thread thread = new IsReachableThread(subnet + "." + i, this);
            thread.start();
            mReachableAddressThread.add(thread);
        }
    }

    @Override
    public void onInteraction(final String data) {
        Log.d(TAG, "onInteraction");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onInteractionUIThread(data);
            }
        });
    }

    private void onInteractionUIThread(String data) {
        //Log.d(TAG, "onInteractionUIThread");
        updateUI(data);
    }

    private void updateUI(String data) {
        try {
            mUILock.acquire();
            mStatusTextView.append("\n"+data);
            mUILock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void joinAllThreads() {
        for (Thread t : mReachableAddressThread) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
