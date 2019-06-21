import org.json.JSONObject;

import jade.core.AID;

class Configuration {

    final osAgent outer;

    AID getAID;
    String road;
    float location;
    String side;
    int lanes;

    public Configuration(osAgent outer, AID id, String r, float loc, String s, int la) {
        this.outer = outer;
        getAID = id;
        road = r;
        location = loc;
        side = s;
        lanes = la;
    }

    public Configuration(osAgent outer) {
        this.outer = outer;
        getAID = null;
        road = null;
        location = 0;
        side = null;
        lanes = 0;
    }

    public Configuration() {
        this.outer = null;
        getAID = null;
        road = null;
        location = 0;
        side = null;
        lanes = 0;
    }

    public void getConfigFromJSON(String input) {
        JSONObject content = new JSONObject(input);
        road = content.getString("road");
        location = content.getFloat("location");
        side = content.getString("side");
        getAID = new AID(content.getString("AID"), AID.ISGUID);
        lanes = content.getInt("lanes");
    }

    public String configToJSON() {
        JSONObject content = new JSONObject();
        content.put("road", road);
        content.put("location", location);
        content.put("side", side);
        content.put("AID", getAID.getName());
        content.put("lanes", lanes);
        return content.toString();
    }
}