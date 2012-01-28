package com.larvalabs.sneaker;

/**
 * @author John Watkinson
 */
public class Constants {

    public static final int POINTS_DENOMINATOR = 1000;

    public static final int POINTS_STAR = 1000;

    public static final int POINTS_MAX = 4000;

    // Every hop, the accrued points for a post are multiplied by this value / 1000.
    public static final int POINTS_DECAY = 667;

    private static final int THRESHOLD_1 = 667;
    private static final int THRESHOLD_2 = 1333;

    public static int getPointResource(int score) {
        if (score > THRESHOLD_2) {
            return R.drawable.status_button_circle;
        } else if (score > THRESHOLD_1) {
            return R.drawable.status_button_circle_small;
        } else {
            return R.drawable.status_button_empty;
        }
    }
    
}
