package com.hkust.ece;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Utility {
    protected Utility() {
    }

    public static String getStandardTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }
    public static String getCodedTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }

    public static int convertChannelToFrequency(int channel) {
        int frequency = 0;
        int[] channel_tb = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 183, 184, 185, 187, 188, 189, 192, 196, 34, 36, 38, 40, 42, 44, 46, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165};
        int[] fre_tb = new int[]{2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 2467, 2472, 2484, 4915, 4920, 4925, 4935, 4940, 4945, 4960, 4980, 5170, 5180, 5190, 5200, 5210, 5220, 5230, 5240, 5260, 5280, 5300, 5320, 5500, 5520, 5540, 5560, 5580, 5600, 5620, 5640, 5660, 5680, 5700, 5745, 5765, 5785, 5805, 5825};

        for (int i = 0; i < 50; ++i) {
            if (channel_tb[i] == channel) {
                frequency = fre_tb[i];
                break;
            }
        }

        return frequency;
    }

    public static int convertFrequencyToChannel(int frequency) {
        int channel = 0;
        int[] channel_tb = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 183, 184, 185, 187, 188, 189, 192, 196, 34, 36, 38, 40, 42, 44, 46, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165};
        int[] fre_tb = new int[]{2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 2467, 2472, 2484, 4915, 4920, 4925, 4935, 4940, 4945, 4960, 4980, 5170, 5180, 5190, 5200, 5210, 5220, 5230, 5240, 5260, 5280, 5300, 5320, 5500, 5520, 5540, 5560, 5580, 5600, 5620, 5640, 5660, 5680, 5700, 5745, 5765, 5785, 5805, 5825};

        for (int i = 0; i < 50; ++i) {
            if (fre_tb[i] == frequency) {
                channel = channel_tb[i];
                break;
            }
        }

        return channel;
    }

    public static boolean isChannel(int number) {
        int[] channel_tb = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 183, 184, 185, 187, 188, 189, 192, 196, 34, 36, 38, 40, 42, 44, 46, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165};

        for (int c : channel_tb)
            if (number == c)
                return true;
        return false;
    }
}
