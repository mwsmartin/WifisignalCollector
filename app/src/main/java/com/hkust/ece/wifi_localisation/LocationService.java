package com.hkust.ece.wifi_localisation;

import com.locus_component.Indiv_Result;
import com.locus_component.Master_record;
import com.locus_core.Locus_core;
import utility.Location;

import java.io.File;
import java.util.List;

public class LocationService {

    private Locus_core locus;
    private LocationHandler loc_service;

    public LocationService(File path, String name) {
        System.out.println("new LocationService(" + path.toString() + "/" + name + ")");

        this.loc_service = new LocationHandler();

        this.locus = new Locus_core();
        this.locus.set_cal_factor((float) 1.5);
        this.locus.set_floor_decision_factor((float) 10, 4);
        this.locus.init_database(path, name);
    }

    public Location getLocationByWifiSignals(List<CustomWiFISignal> signals) {
        Location final_location = null;


        for (CustomWiFISignal signal : signals) {
            //System.out.println("SSID = " + signal.SSID + " BSSID = " + signal.BSSID + " FREQ = " + signal.frequency + " LEVEL = " + signal.level);
            System.out.println("results.add(new CustomWiFISignal(\"" + signal.SSID + "\", \"" + signal.BSSID + "\", " + signal.level + ", " + signal.frequency + "));");
        }

        // Get floor decision at first
        int decision_floor = findMostPossibleFloor(signals);
        if (decision_floor != -99) {
            System.out.println("Current floor:" + decision_floor);
            final_location = getFinalLocation(signals, decision_floor);

            System.out.println("X: " + final_location.get_x() + " Y: " + final_location.get_y() + " Z: " + final_location.get_floor());
        } else {
            System.out.println("No floor is located!");
        }

        return final_location;
    }

    private Location getFinalLocation(List<CustomWiFISignal> results, int decision_floor) {
        Indiv_Result tmp_result;
        int order = 0;
        float highest_pro = (float) 0.0;
        Location final_location = null;

        this.locus.clear_record();
        this.loc_service.clear();

        for (CustomWiFISignal result : results) {
            result.level = this.locus.calibration_adjust(result.frequency, result.level);
            this.locus.add_record(result.SSID, result.BSSID, result.frequency, result.level);
        }

        //  Localization
        String current_place = this.locus.do_localization();

        //Output Result
        if (current_place != null) {
            System.out.println("Current place:" + current_place);

            //Get the position with the highest probability
            tmp_result = this.locus.get_results(order);
            if (tmp_result != null) {
                highest_pro = tmp_result.get_sum_up();
            }
            //Set marker on all possible locations with different colours
            while (tmp_result != null) {
                //Add locations with high probabilities to the candidate pool for accurate location calculation
                if ((order < 8) || (tmp_result.get_sum_up() > highest_pro / 2)) {
                    this.loc_service.addLocation(tmp_result.get_name(), tmp_result.get_floor(),
                            tmp_result.get_x(), tmp_result.get_y(), tmp_result.get_sum_up());
                }

                order++;
                tmp_result = this.locus.get_results(order);
            }

            final_location = this.loc_service.findExactLocation(decision_floor);
            if (final_location == null) {
                System.out.println("No final location found!");
                tmp_result = this.locus.get_results(0);
                final_location = new Location(tmp_result.get_name(), tmp_result.get_floor(), tmp_result.get_x(), tmp_result.get_y());
            }
        } else {
            System.out.println("Localization failed");
        }

        return final_location;
    }

    private int findMostPossibleFloor(List<CustomWiFISignal> results) {
        float max_signal = -99;
        int floor_detection_num = 5;
        double floor_detection_factor = 1.5;        //50%: i.e. Max Lv = -50, every signal lv > -75dBm
        double[] floor_probability = new double[10];
        double[] floor_probability_sum = new double[10];

        int[] cnt = new int[10];    //Only handle Floor 0-9 <<- need to update
        int id = 0;

        //Clear Locus library internal status
        this.locus.clear_record();

        //Step 1: Look for high signal level only to do the localization

        //Find max signal lv
        for (CustomWiFISignal result : results) {
            if (result.level > max_signal) {
                max_signal = result.level;
            }
        }

        //Only consider signal with > 50% of max_signal
        int iii = 0;
        for (CustomWiFISignal result : results) {
            if (result.level > (max_signal * floor_detection_factor)) //50%: i.e. Max Lv = -50, every signal lv > -75dBm
            {
                result.level = this.locus.calibration_adjust(result.frequency, result.level);
                this.locus.add_record(result.SSID, result.BSSID, result.frequency, result.level);
                iii++;
            }
        }
        //Step 2: Do localization using Locus library

        if (this.locus.do_localization() == null) {
            System.out.println("Result Floor :" + "  " + this.locus.get_result_floor());
            return -99;
        }

        //Step 3: Getting result, and decide the floor

        Master_record tmp_record = new Master_record();

        //Method 1: Filter out top 5 highest probability locations for each floor
        Indiv_Result result = this.locus.get_results(id);
        while (result != null) {
            if (cnt[result.get_floor()] < floor_detection_num)        //Only handle top 5 probaility of each floor
            {
                floor_probability_sum[result.get_floor()] += result.get_sum_up();
                cnt[result.get_floor()]++;
            }
            id++;
            result = this.locus.get_results(id);
        }
        for (int i = 0; i < 10; i++) {
            floor_probability[i] = floor_probability_sum[i] / floor_detection_num;
        }

        //Check max floor number
        double tmp = 0;
        int max_floor = -99;
        for (int i = 0; i < 10; i++) {
            if (tmp < floor_probability[i]) {
                max_floor = i;
                tmp = floor_probability[i];
            }
        }

        return max_floor;
    }


}
