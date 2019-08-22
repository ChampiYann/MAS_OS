package measure;

import java.time.LocalTime;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;

public class Measure {

    protected AID origin;
    protected long ID;
    protected String road;
    protected Vector<Integer> lanes;
    protected double start;
    protected double end;

    protected Measure() {}

    public Measure(JSONObject content) {
        origin = new AID(content.getString("origin"),AID.ISGUID);
        ID = content.getLong("ID");
        road = content.getString("road");
        start = content.getDouble("start");
        end = content.getDouble("end");
        lanes = new Vector<Integer>();
        Iterator<Object> lanesIterator = content.getJSONArray("lanes").iterator();
        while (lanesIterator.hasNext()) {
            lanes.add((Integer)lanesIterator.next());
        }
    }

    /**
     * @return the iD
     */
    public long getID() {
        return ID;
    }

    /**
     * @return the lanes
     */
    public Vector<Integer> getLanes() {
        return lanes;
    }

    public JSONObject toJSON() {
        JSONObject content = new JSONObject();
        content.put("origin", origin.getName());
        content.put("start", start);
        content.put("end", end);
        content.put("ID",ID);
        content.put("road", road);
        JSONArray lanesJSON = new JSONArray(lanes);
        content.put("lanes", lanesJSON);
        return content;
    }

    private LocalTime startTime;
    private LocalTime endTime;

    public Measure(AID aid, LocalTime startTime, LocalTime endTime, String road, double start, double end, Vector<Integer> lanes) {
        this.origin = aid;
        this.road = road;
        this.start = start;
        this.end = end;
        this.lanes = lanes;
        this.startTime = startTime;
        this.endTime = endTime;

        Random rand = new Random();
        int n = rand.nextInt(50);
        this.ID = System.currentTimeMillis() + n;
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