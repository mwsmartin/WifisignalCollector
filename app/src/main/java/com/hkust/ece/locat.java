package com.hkust.ece;

import android.widget.Toast;

import com.hkust.ece.wifi_signal_collector.MainActivity;

import java.util.Queue;

/**
 * Created by damonlp on 27/12/16.
 */
public class locat {
    public int[] current = new int[2];
    public locat previous;
    //public int movement;

    public locat(int x, int y, locat prev){
        this.previous = prev;
        this.current[0] = x;
        this.current[1] = y;

    }

}
