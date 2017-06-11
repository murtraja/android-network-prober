package com.smb.murtraja.prober;

import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by murtraja on 10/6/17.
 */
public class IsReachableThread extends Thread {

    static String TAG = "IsReachableThread";
    private final static String MAC_RE = "^%s\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+\\w+$";
    private final static int BUF = 8 * 1024;
    int TIMEOUT = 2000;
    String mAddress;
    InteractionListener mListener;
    public IsReachableThread(String address, InteractionListener listener) {
        mAddress = address;
        mListener = listener;
    }

    @Override
    public void run() {
        //Log.d(TAG, "now starting thread "+mAddress);
        String host=mAddress;
        long t1 = System.currentTimeMillis() % 1000;
        try {
            if (InetAddress.getByName(host).isReachable(TIMEOUT)){
                long t2 = System.currentTimeMillis() % 1000;
                System.out.println(host + " is reachable ("+(t2-t1)+"ms)");
                String arp = getHardwareAddress(host);
                mListener.onInteraction(host+"->"+arp);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static String getHardwareAddress(String ip) {
        //https://github.com/rorist/android-network-discovery/blob/master/src/info/lamatricexiste/network/Network/HardwareAddress.java
        String hw = "";
        BufferedReader bufferedReader = null;
        try {
            if (ip != null) {
                String ptrn = String.format(MAC_RE, ip.replace(".", "\\."));
                Pattern pattern = Pattern.compile(ptrn);
                bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"), BUF);
                String line;
                Matcher matcher;
                while ((line = bufferedReader.readLine()) != null) {
                    matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hw = matcher.group(1);
                        break;
                    }
                }
            } else {
                Log.e(TAG, "ip is null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't open/read file ARP: " + e.getMessage());
            return hw;
        } finally {
            try {
                if(bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return hw;
    }
}