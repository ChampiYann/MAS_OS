package measure;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

public class Measure {

    public static final int AID = 1;
    public static final int CENTRAL = 2;

    protected long ID;
    protected int type;
    protected String road;
    protected MSI[] display;
    protected double start;
    protected double end;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    protected Measure() {}

    public Measure(LocalDateTime startTime, LocalDateTime endTime, String road, double start, double end, MSI[] lanes) {
        this.road = road;
        this.start = start;
        this.end = end;
        this.display = lanes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = 0;

        Random rand = new Random();
        int n = rand.nextInt(50);
        this.ID = System.currentTimeMillis() + n;
    }

    public Measure(int type, String road, double start, MSI[] lanes) {
        this.type = type;
        this.road = road;
        this.start = start;
        this.end = start;
        this.display = lanes;
        this.startTime = null;
        this.endTime = null;

        Random rand = new Random();
        int n = rand.nextInt(50);
        this.ID = System.currentTimeMillis() + n;
    }

    public Measure(long ID, int type, String road, double start, MSI[] lanes) {
        this.type = type;
        this.road = road;
        this.start = start;
        this.end = start;
        this.display = lanes;
        this.startTime = null;
        this.endTime = null;
        this.ID = ID;
    }

    public Measure(JSONObject content) {
        this.ID = content.getLong("ID");
        this.road = content.getString("road");
        this.start = content.getDouble("start");
        this.end = content.getDouble("end");
        this.type = content.getInt("type");
        this.display = new MSI[content.getJSONArray("display").length()];
        for (int i = 0; i < content.getJSONArray("display").length(); i++) {
            this.display[i] = new MSI(content.getJSONArray("display").getJSONObject(i));
        }
    }

    public JSONObject toJSON() {
        JSONObject content = new JSONObject();
        content.put("start", start);
        content.put("end", end);
        content.put("ID",ID);
        content.put("road", road);
        content.put("type",type);
        JSONArray jsonDisplay = new JSONArray(display);
        content.put("display", jsonDisplay);
        return content;
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
    public MSI[] getDisplay() {
        return display;
    }

	/**
     * @return the startTime
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * @return the endTime
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @return the end
     */
    public double getEnd() {
        return end;
    }

    /**
     * @return the start
     */
    public double getStart() {
        return start;
    }

    /**
     * @return the road
     */
    public String getRoad() {
        return road;
    }

    @Override
    public boolean equals(Object obj) {
        Measure s = (Measure) obj;
        return this.ID == s.ID && this.type == s.type && Arrays.equals(this.display,s.display);
    }
}