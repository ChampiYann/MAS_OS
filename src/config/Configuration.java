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

    public Configuration(String input) {
        JSONObject content = new JSONObject(input);
        this.outer = null;
        this.getAID = new AID(content.getString("AID"), jade.core.AID.ISGUID);
        this.road = content.getString("road");
        this.location = content.getFloat("location");
        this.side = content.getString("side");
        this.lanes = content.getInt("lanes");
    }

    public JSONObject configToJSON() {
        JSONObject content = new JSONObject();
        content.put("road", this.road);
        content.put("location", this.location);
        content.put("side", this.side);
        content.put("AID", this.getAID.getName());
        content.put("lanes", this.lanes);
        return content;
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

    public static boolean ConfigurationEqual(Configuration c1, Configuration c2) {
        return c1.getAID.equals(c2.getAID);
    }

     /**
     * @return the aID
     */
    public AID getAID() {
        return this.getAID;
    }

    /**
     * @param aID the aID to set
     */
    public void setAID(AID aID) {
        this.getAID = aID;
    }

    /**
     * @return the lanes
     */
    public int getLanes() {
        return this.lanes;
    }

    /**
     * @param lanes the lanes to set
     */
    public void setLanes(int lanes) {
        this.lanes = lanes;
    }

    /**
     * @return the location
     */
    public double getLocation() {
        return this.location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(float location) {
        this.location = location;
    }

    /**
     * @return the road
     */
    public String getRoad() {
        return this.road;
    }

    /**
     * @param road the road to set
     */
    public void setRoad(String road) {
        this.road = road;
    }

    /**
     * @return the side
     */
    public String getSide() {
        return this.side;
    }

    /**
     * @param side the side to set
     */
    public void setSide(String side) {
        this.side = side;
    }
}