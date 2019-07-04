package config;

import java.util.Comparator;

import org.json.JSONObject;

import agents.osAgent;
import jade.core.AID;

public class Configuration {

    final osAgent outer;

    public AID getAID;
    public String road;
    public float location;
    public String side;
    public int lanes;

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

    public static Comparator<Configuration> kmCompare = new Comparator<Configuration>() {
        @Override
        public int compare(Configuration e1, Configuration e2) {
            if(e1.location < e2.location) {
                return -1;
            } else if (e1.location > e2.location) {
                return 1;
            } else {
                return 0;
            }
        }
    };
}