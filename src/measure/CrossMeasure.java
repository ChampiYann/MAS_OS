package measure;

import java.time.LocalTime;
import java.util.Random;

import jade.core.AID;

public class CrossMeasure extends Measure {

    private LocalTime startTime;
    private LocalTime endTime;
    
    public CrossMeasure(AID o, LocalTime st, LocalTime et, String r, float s, float e, boolean[] l) {
        type = CROSS;
        origin = o;
        size = 4;
        iteration = size;
        start = s;
        end = e;
        road = r;
        lanes = l;
        startTime = st;
        endTime = et;

        Random rand = new Random();
        int n = rand.nextInt(50);
        ID = System.currentTimeMillis() + n;
    }

    /**
     * @return the startTime
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * @return the endTime
     */
    public LocalTime getEndTime() {
        return endTime;
    }
}