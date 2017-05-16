package com.hkust.ece.wifi_database;

import au.com.bytecode.opencsv.CSVReader;

import com.hkust.ece.Utility;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class DatabaseGenerator {
    private static DatabaseGenerator instance = null;

    protected DatabaseGenerator() {
    }

    // Lazy Initialization (If required then only)
    public static DatabaseGenerator getInstance() {
        if (instance == null) {
            // Thread Safe. Might be costly operation in some case
            synchronized (DatabaseGenerator.class) {
                if (instance == null) {
                    instance = new DatabaseGenerator();
                }
            }
        }
        return instance;
    }

    public class Location {
        String place;
        int floor;
        double x;
        double y;

        public Location(String place, int floor, double x, double y) {
            this.place = place;
            this.floor = floor;
            this.x = x;
            this.y = y;
        }
    }

    public class LocationStat {
        int loc_id;
        float u1;
        float u2;

        public LocationStat(int loc_id, float u1, float u2) {
            this.loc_id = loc_id;
            this.u1 = u1;
            this.u2 = u2;
        }
    }

    public class AccessPoint {
        String mac_addr;
        String ssid;
        int channel;
        int power;
        List<LocationStat> location_stat;

        public AccessPoint(String mac_addr, String ssid, int channel, int power) {
            this.mac_addr = mac_addr;
            this.ssid = ssid;
            this.channel = channel;
            this.power = power;
            this.location_stat = new ArrayList<LocationStat>();
        }
    }


    public void createDatabaseFile(final File folder, String path_to_save) throws IOException {

        File output_file = new File(path_to_save, "WiFiDatabase_new.dbb");
        DataOutputStream oo = new DataOutputStream(new FileOutputStream(output_file));

        List<Location> locations = new ArrayList<Location>();
        Hashtable<String, AccessPoint> aps = new Hashtable<String, AccessPoint>();

        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory() && fileEntry.getName().contains(".csv")) {
                String filename = fileEntry.getName();
                String location_tmp = filename.replace(".csv", "");
                String[] location_tmp2 = location_tmp.split("_");

                // Each csv file will be added to the List<Location> locations.
                int currentIndex = locations.size();
                locations.add(new Location(filename, Integer.parseInt(location_tmp2[0]), Double.parseDouble(location_tmp2[1]), Double.parseDouble(location_tmp2[2])));

                CSVReader reader = null;

                reader = new CSVReader(new FileReader(folder.getAbsolutePath() + "/" + fileEntry.getName()), ',');
                String[] nextLine;
                //Read one line at a time, i.e. each row in the csv file
                while ((nextLine = reader.readNext()) != null) {
                    if (nextLine.length < 4) continue;

                    String mac_addr = nextLine[0];
                    String _channel = nextLine[1].substring(1);
                    String ssid = nextLine[2].substring(1);

                    int channel = Integer.parseInt(_channel);
                    if (!Utility.isChannel(channel))
                        channel = Utility.convertFrequencyToChannel(channel);

                    int[] power = new int[nextLine.length - 3];

                    for (int i = 3; i < nextLine.length; i++) {
                        String v = nextLine[i].substring(1);
                        v = (v.equals("")) ? "0" : v;

                        power[i - 3] = Integer.parseInt(v);
                    }

                    float u1 = ux_calculate(power, 0, 1);
                    float u2 = ux_calculate(power, u1, 2);

                    if (u1 < -100.0f)
                        continue;
                    if (u2 < 0.5f)
                        u2 = 0.5f;

                    String key = mac_addr + channel;
                    if (!aps.containsKey(key)) {
                        aps.put(key, new AccessPoint(mac_addr, ssid, channel, 0));
                    }

                    aps.get(key).location_stat.add(new LocationStat(currentIndex, u1, u2));

                }

                reader.close();
            }
        }

        int loc_num = locations.size();
        oo.writeInt(loc_num);

        System.out.println("loc_num: " + loc_num);

        int tmp_len;
        int e1;
        byte[] var28;
        for (Location tmp : locations) {
            //Write total length of place
            tmp_len = tmp.place.length();
            oo.writeByte(tmp_len);

            //Write place
            var28 = tmp.place.getBytes();
            oo.write(var28, 0, tmp_len);

            int floor = tmp.floor;
            oo.writeInt(floor);
            double x = tmp.x;
            oo.writeDouble(x);
            double y = tmp.y;
            oo.writeDouble(y);
        }

        int ap_num = aps.size();
        oo.writeInt(ap_num);

        System.out.println("ap_num: " + ap_num);

        for (String key : aps.keySet()) {
            AccessPoint tmp = aps.get(key);

            //Write total length of ssid
            tmp_len = tmp.ssid.length();
            oo.writeByte(tmp_len);

            //Write ssid
            var28 = tmp.ssid.getBytes();
            oo.write(var28, 0, tmp_len);

            //Write channel
            int channel = tmp.channel;
            oo.writeInt(channel);

            //Write power
            int power = tmp.power;
            oo.writeInt(power);

            //Write mac_address
            var28 = tmp.mac_addr.getBytes();
            oo.write(var28, 0, 17);

            //Write size of location stats
            oo.writeInt(tmp.location_stat.size());

            for (int j = 0; j < tmp.location_stat.size(); ++j) {
                LocationStat xxx = tmp.location_stat.get(j);

                int loc_id = xxx.loc_id;
                oo.writeInt(loc_id);

                float u1 = xxx.u1;
                oo.writeFloat(u1);

                float u2 = xxx.u2;
                oo.writeFloat(u2);

                oo.write(new byte[16], 0, 16);
            }
        }

        oo.flush();
    }

    public void checkDatabaseFile(File path, String name) {
        int ap_num = 0;
        File input_file = new File(path, name);
        File log_file = new File(path, name + "_log");

        try {
            DataInputStream e = new DataInputStream(new FileInputStream(input_file));
            DataOutputStream log = new DataOutputStream(new FileOutputStream(log_file));

            try {
                int loc_num = e.readInt();
                log.writeBytes("loc_num: " + loc_num + "\n");

                byte tmp_len;
                int e1;
                byte[] var28;
                for (e1 = 0; e1 < loc_num; ++e1) {
                    tmp_len = e.readByte();

                    var28 = new byte[tmp_len];

                    e.read(var28, 0, tmp_len);

                    String place = new String(var28);
                    int floor = e.readInt();
                    double x = e.readDouble();
                    double y = e.readDouble();

                    log.writeBytes("Place: " + place + " floor: " + floor + " x: " + x + " y: " + y + "\n");
                }

                ap_num = e.readInt();
                log.writeBytes("ap_num: " + ap_num + "\n");

                for (e1 = 0; e1 < ap_num; ++e1) {
                    tmp_len = e.readByte();
                    var28 = new byte[tmp_len];

                    e.read(var28, 0, tmp_len);

                    String ssid = new String(var28);
                    int channel = e.readInt();
                    int power = e.readInt();
                    var28 = new byte[17];

                    e.read(var28, 0, 17);

                    String mac_addr = new String(var28);
                    loc_num = e.readInt();
                    log.writeBytes("ssid: " + ssid + " mac_addr: " + mac_addr + " channel: " + channel + " power: " + power + "\n");

                    for (int j = 0; j < loc_num; ++j) {
                        int loc_id = e.readInt();
                        float u1 = e.readFloat();
                        float u2 = e.readFloat();
                        e.skipBytes(16);
                        log.writeBytes("loc_id: " + loc_id + " u1: " + u1 + " u2: " + u2 + "\n");

                    }

                }
            } catch (IOException var26) {
                var26.printStackTrace();
            }

            log.flush();
        } catch (IOException var27) {
            var27.printStackTrace();
        }

    }

    private float ux_calculate(int[] power, float Ex, int x) {
        int i, j;
        float sum = 0;
        float result, diff, diffp;
        int valid_data_num = 0;

        for (i = 0; i < power.length; i++) {
            if (power[i] != 0) {
                diffp = diff = (float) power[i] - Ex;
                for (j = 0; j < (x - 1); j++) {
                    diffp *= diff;
                }
                sum += diffp;
                valid_data_num++;
            }
        }
        result = sum / (float) valid_data_num;
        return result;
    }


}