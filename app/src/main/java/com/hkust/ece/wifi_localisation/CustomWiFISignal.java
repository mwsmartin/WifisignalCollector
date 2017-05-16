package com.hkust.ece.wifi_localisation;

import com.hkust.ece.Utility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CustomWiFISignal {
    public String SSID;

    public String BSSID;
    public int level;
    public int frequency;

    public CustomWiFISignal(String SSID, String BSSID, int level, int frequency) {
        this.SSID = SSID;
        this.BSSID = BSSID;
        this.level = level;
        this.frequency = frequency;
    }

    public CustomWiFISignal(String jsonString) {
        // e.g. {"BSSID":"ec:c8:82:7a:df:60","level":-69,"freq":2412,"SSID":"sMobileNet"}
        try {
            JSONObject tmp = new JSONObject(jsonString);
            this.SSID = tmp.getString("SSID");
            this.BSSID = tmp.getString("BSSID");
            this.level = tmp.getInt("level");
            this.frequency = tmp.getInt("freq");
        } catch (JSONException e) {
            System.out.println(e.toString());
        }
    }

    public CustomWiFISignal(JSONObject obj) {
        // e.g. {"BSSID":"ec:c8:82:7a:df:60","level":-69,"freq":2412,"SSID":"sMobileNet"}
        try {
            this.SSID = obj.getString("SSID");
            this.BSSID = obj.getString("BSSID");
            this.level = obj.getInt("level");
            this.frequency = obj.getInt("freq");
        } catch (JSONException e) {
            System.out.println(e.toString());
        }
    }

    public static List<CustomWiFISignal> getSignalListByJSONString(String jsonString) {
        List<CustomWiFISignal> tmp = new ArrayList<CustomWiFISignal>();
        try {
            JSONArray b = new JSONArray(jsonString);
            for (int i = 0; i < b.length(); i++)
                tmp.add(new CustomWiFISignal(b.getJSONObject(i)));
        } catch (JSONException e) {
            System.out.println(e.toString());
        }
        return tmp;
    }

    public static List<CustomWiFISignal> getSignalListByRawString(String rawString) {
        List<CustomWiFISignal> tmp = new ArrayList<CustomWiFISignal>();
        String[] items = rawString.split(Pattern.quote("CWLAP:("));
        for (String i : items) {
            String[] a = i.split(Pattern.quote(",\""));
            if (a.length != 3)
                continue;

            String[] b = a[1].split(Pattern.quote("\","));
            if (b.length != 2)
                continue;

            String[] c = a[2].split(Pattern.quote("\","));
            if (c.length != 2)
                continue;

            String[] d = c[1].split(Pattern.quote(","));
            if (d.length != 2)
                continue;


            //tmp.add(new CustomWiFISignal(b[0], c[0], Integer.parseInt(b[1]), Integer.parseInt(d[0])));
            tmp.add(new CustomWiFISignal(b[0], c[0], Integer.parseInt(b[1]), Utility.convertChannelToFrequency(Integer.parseInt(d[0]))));

        }
        return tmp;
    }
}
