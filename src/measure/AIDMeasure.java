package measure;

import config.Configuration;

public class AIDMeasure extends Measure {

    public AIDMeasure(Configuration config) {
        type = AIDet;
        origin = config.getAID;

        size = 3;
        iteration = size;
        start = config.location;
        end = config.location - (float)0.001;
        road = config.road;
        lanes = new boolean[config.lanes];
        for (int i = 0; i < config.lanes; i++) {
            lanes[i] = false;
        }

        ID = System.currentTimeMillis();
    }
}