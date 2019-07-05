package measure;

import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;

public class Measure {

    public static final int AIDet      = 0;
    public static final int CROSS      = 1;

    private static final String[] Measures = new String[2];
    static {
        Measures[AIDet] = "AID";
        Measures[CROSS] = "CROSS";
    }


    protected AID origin;
    protected int type;
    protected int size;
    protected int iteration;
    protected float start;
    protected float end;
    protected long ID;
    protected String road;
    protected boolean[] lanes;

    protected Measure() { }

    public Measure(JSONObject content) {
        type = content.getInt("type");
        origin = new AID(content.getString("origin"),AID.ISGUID);
        size = content.getInt("size");
        start = content.getFloat("start");
        end = content.getFloat("end");
        ID = content.getLong("ID");
        iteration = content.getInt("iteration");
        road = content.getString("road");
        JSONArray lanesJSON = content.getJSONArray("lanes");
        lanes = new boolean[lanesJSON.length()];
        for (int i = 0; i < lanesJSON.length(); i++) {
            lanes[i] = lanesJSON.getBoolean(i);
        }
    }

    public int getType() {
        return type;
    }

    public int getIteration() {
        return iteration;
    }

    public AID getOrigin() {
        return origin;
    }

    public long getID() {
        return ID;
    }

    public boolean getLane(int i) { //throws outofbounds
        return lanes[i];
    }

    public JSONObject toJSON() {
        JSONObject content = new JSONObject();
        content.put("origin", origin.getName());
        content.put("type", type);
        content.put("size", size);
        content.put("iteration", iteration);
        content.put("start", start);
        content.put("end", end);
        content.put("ID",ID);
        content.put("road", road);
        JSONArray lanesJSON = new JSONArray(lanes);
        content.put("lanes", lanesJSON);
        return content;
    }
}