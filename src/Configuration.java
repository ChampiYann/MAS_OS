import org.json.JSONObject;

import jade.core.AID;

class Configuration {

    final osAgent outer;

    AID getAID;
    String road;
    float location;
    String side;

    public Configuration(osAgent outer, AID id, String r, float l, String s) {
        this.outer = outer;
        getAID = id;
        road = r;
        location = l;
        side = s;
    }

    public Configuration(osAgent outer) {
        this.outer = outer;
        getAID = null;
        road = null;
        location = 0;
        side = null;
    }

    public void getConfigFromJSON(String input) {
        JSONObject content = new JSONObject(input);
        road = content.getString("road");
        location = content.getFloat("location");
        side = content.getString("side");
    }
}