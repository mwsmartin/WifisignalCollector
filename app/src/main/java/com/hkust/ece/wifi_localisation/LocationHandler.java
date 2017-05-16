package com.hkust.ece.wifi_localisation;

import utility.Location;

import java.util.ArrayList;
import java.util.List;

public class LocationHandler {
    private class PossibleLocation extends Location {
        private double probability;

        public PossibleLocation(String name, int floor, double x, double y, double probability) {
            super(name, floor, x, y);
            this.probability = probability;
        }

        public double getProbability() {
            return this.probability;
        }
    }

    private List<PossibleLocation> places = new ArrayList<PossibleLocation>();
    private List<Double> x = new ArrayList<Double>();
    private List<Double> y = new ArrayList<Double>();
    private List<Double> var = new ArrayList<Double>();


    void addLocation(String name, int floor, double x, double y, double probability) {
        this.places.add(new PossibleLocation(name, floor, x, y, probability));
    }

    void clear() {
        this.places.clear();
        this.x.clear();
        this.y.clear();
        this.var.clear();
    }

    Location findExactLocation(int cal_floor) {
        double sum_x, sum_y, mean_x, mean_y, var_x, var_y, sum_prob;

        if (this.places.size() < 7) {
            return null;
        }

        //Find the candidates on the decided floor
        for (int i = 0; i < this.places.size(); i++) {
            if (this.places.get(i).get_floor() != cal_floor) {
                this.places.remove(i);
            }
        }

        if (this.places.size() > 4) {
            // Find the total probability
            double temp_sum_prob = 0.0;
            double temp_var_x = 0.0;
            double temp_var_y = 0.0;
            double temp_sum_x = 0.0;
            double temp_sum_y = 0.0;
            double temp_mean_x;
            double temp_mean_y;


            for (PossibleLocation place : this.places) {
                temp_sum_prob += place.getProbability();
            }

            for (PossibleLocation place : this.places) {
                temp_sum_x += place.get_x();
                temp_sum_y += place.get_y();
            }

            temp_mean_x = temp_sum_x / (this.places.size());
            temp_mean_y = temp_sum_y / (this.places.size());

            for (PossibleLocation place : this.places) {
                temp_var_x += Math.pow(place.get_x() - temp_mean_x, 2);
                temp_var_y += Math.pow(place.get_y() - temp_mean_y, 2);
            }


            //Find all cases mean value
            for (int i = 0; i < this.places.size(); i++) {
                sum_x = sum_y = var_x = var_y = 0;
                //Calculate the mean of other location
                for (int j = 0; j < this.places.size(); j++) {
                    if (j != i)        //Except 'i'
                    {
                        sum_x += this.places.get(j).get_x();
                        sum_y += this.places.get(j).get_y();
                    }
                }
                mean_x = sum_x / (this.places.size() - 1);
                mean_y = sum_y / (this.places.size() - 1);
                this.x.add(mean_x);
                this.y.add(mean_y);
                //Calculate the variance
                for (int j = 0; j < this.places.size(); j++) {
                    if (j != i)        //Except 'i'
                    {
                        var_x += Math.pow(this.places.get(j).get_x() - mean_x, 2);
                        var_y += Math.pow(this.places.get(j).get_y() - mean_y, 2);
                    }
                }
                this.var.add((var_x + var_y) / (temp_var_x + temp_var_y) * this.places.get(i).getProbability() / temp_sum_prob);
            }

            //Find the min of the variance and return its mean location
            double min_value = this.var.get(0);
            int min_id = 0;
            for (int i = 1; i < this.var.size(); i++) {
                if (this.var.get(i) < min_value) {
                    min_value = this.var.get(i);
                    min_id = i;
                }
            }
            this.places.remove(min_id);
            this.var.clear();
        }

        // Find the probability weighted location
        sum_x = sum_y = sum_prob = 0;
        for (PossibleLocation place : this.places) {
            sum_x += place.get_x() * place.getProbability();
            sum_y += place.get_y() * place.getProbability();
            sum_prob += place.getProbability();
        }
        mean_x = sum_x / sum_prob;
        mean_y = sum_y / sum_prob;
        this.x.add(mean_x);
        this.y.add(mean_y);

        return new Location("Final", cal_floor, mean_x, mean_y);
    }
}
