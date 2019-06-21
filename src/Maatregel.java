import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;

public class Maatregel {

    static final int AIDet      = 0;
    static final int CROSS      = 1;

    private static final String[] Maatregels = new String[2];
    static {
        Maatregels[AIDet] = "AID";
        Maatregels[CROSS] = "CROSS";
    }


    private AID origin;
    private int type;
    private int size;
    private int iteration;
    private float start;
    private float end;
    private long ID;
    private String road;
    private boolean[] lanes;

    public Maatregel(int t, Configuration config) {
        type = t;
        origin = config.getAID;

        if (type == AIDet) {
            size = 3;
        } else if (type == CROSS) {
            size = 4;
        }
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

    public Maatregel(int t, AID o, int it, float st, float e, String r, boolean[] l) {
        type = t;
        origin = o;

        if (type == AIDet) {
            size = 3;
        } else if (type == CROSS) {
            size = 4;
        }
        iteration = size;
        start = st;
        end = e;
        road = r;
        lanes = l;

        ID = System.currentTimeMillis();
    }

    public Maatregel(JSONObject content) {
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
        // for (int i = 0; i < lanes.length; i++) {
        //     lanesJSON.put(lanes[i]);
        // }
        content.put("lanes", lanesJSON);
        return content;
    }
}