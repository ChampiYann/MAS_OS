import org.json.JSONObject;

import jade.core.AID;

public class Maatregel {

    static final int AIDet        = 0;
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

    public Maatregel(int t, AID o){
        type = t;
        origin = o;

        if (type == AIDet) {
            size = 3;
        } else if (type == CROSS) {
            size = 3;
        }
        iteration = size;
        start = 0;
        end = 0;

        ID = System.currentTimeMillis();
    }

    public Maatregel(int t, AID o, int it, float st, float e) {
        type = t;
        origin = o;

        if (type == AIDet) {
            size = 3;
        } else if (type == CROSS) {
            size = 3;
        }
        iteration = it;
        start = st;
        end = e;

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

    public JSONObject toJSON() {
        JSONObject content = new JSONObject();
        content.put("origin", origin);
        content.put("type", type);
        content.put("size", size);
        content.put("iteration", iteration);
        content.put("start", start);
        content.put("end", end);
        content.put("ID",ID);
        return content;
    }

    public void send() {
        
    }

    public void cancel() {

    }
}