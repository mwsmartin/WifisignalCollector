package com.hkust.ece;

/**
 * Created by damonlp on 26/12/16.
 */

import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.widget.Toast;

import com.hkust.ece.wifi_localisation.LocationService;
import com.hkust.ece.wifi_signal_collector.MainActivity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import utility.Location;

public class route {
    //private WifiManager wifiManager;
    //private LocationService locationService;
    public int target_x = 24;
    public int target_y = 100;
    public int nextLocation;

    public route(int ds){
        getTarget(ds);
         //wifiManager.startScan();
    }

    public String calRoute(){
        locat result = null;

        if(MainActivity.current_x == target_x && MainActivity.current_y == target_y){
            Toast.makeText(MainActivity.mContext, "Arrived", Toast.LENGTH_SHORT).show();
            return "Arrived";
        }else{

            int[]start = dsToArray((int) MainActivity.current_y);
            int[]end = dsToArray(target_y);

            locat point = new locat(start[0], start[1], null);

            boolean reach = false;
            LinkedList<locat> path = new LinkedList<locat>();

            path.add(point);

            while(!reach){
                locat curPt = path.poll();
                if(curPt.current[0] == end[0] && curPt.current[1] == end[1]){
                    reach = true;
                    result = curPt;
                    break;
                }
                //turn left
                if(isValid(curPt.current[0],curPt.current[1]-1)){
                    locat point2 = new locat(curPt.current[0], curPt.current[1]-1, curPt);
                    path.add(point2);
                }
                //turn right
                if(isValid(curPt.current[0],curPt.current[1]+1)){
                    locat point3 = new locat(curPt.current[0], curPt.current[1]+1, curPt);
                    path.add(point3);
                }
                //turn up
                if(isValid(curPt.current[0]-1,curPt.current[1])){
                    locat point4 = new locat(curPt.current[0]-1, curPt.current[1], curPt);
                    path.add(point4);
                }
                //turn down
                if(isValid(curPt.current[0]+1,curPt.current[1])){
                    locat point5 = new locat(curPt.current[0]+1, curPt.current[1], curPt);
                    path.add(point5);
                }
            }
        }

        String resultPath = Integer.toString(MainActivity.maze[result.current[0]][result.current[1]]);
        while(result.previous != null){
            result = result.previous;

            resultPath = Integer.toString(MainActivity.maze[result.current[0]][result.current[1]]) + ">" + resultPath;
        }
        String[] results = resultPath.split(">");

        Toast.makeText(MainActivity.mContext, resultPath, Toast.LENGTH_LONG).show();
        System.out.println(resultPath);

        return results[1];
    }

    public boolean isValid(int x, int y){
        /*
                 0   1   2
            0: [100 200 300]
            1: [ 0  400  0 ]
            2: [ 0  500  0 ]
            maze[x][y]
         */

        boolean valid = false;
        if(x < MainActivity.maze.length && x >= 0){
            if(y < MainActivity.maze[0].length && y >= 0){
                if(MainActivity.maze[x][y] != 0){
                    valid = true;
                }
            }
        }
        return valid;
    }

    public void getTarget(int ds){
        switch(ds){
            case 2439:
                this.target_x = 24;
                this.target_y = 500;
                break;
            case 2438:
                this.target_x = 24;
                this.target_y = 400;
                break;
            case 2431:
            case 2432:
            case 2433:
            case 2434:
                this.target_x = 24;
                this.target_y = 200;
                break;
            case 2429:
            case 2430:
            case 2436:
            case 2437:
                this.target_x = 24;
                this.target_y = 100;
                break;
        }
    }

    public int[] dsToArray(int y){
        int[]end = {0,0};
        switch(y){
            case 500:
                end[0] = 2;
                end[1] = 1;
                break;
            case 400:
                end[0] = 1;
                end[1] = 1;
                break;
            case 200:
                end[0] = 0;
                end[1] = 1;
                break;
            case 100:
                end[0] = 0;
                end[1] = 0;
                break;
        }
        return end;
    }
}
