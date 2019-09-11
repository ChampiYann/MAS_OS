package config;

import jade.core.AID;

import java.util.Comparator;

import org.json.JSONObject;

import agents.osAgent;

public class Configuration {

    final osAgent outer;

    private AID AID;
    private String road;
    private double location;
    private String side;
    private int lanes;

    public Configuration(osAgent outer, AID id, String r, double loc, String s, int la) {
        this.outer = outer;
        this.AID = id;
        this.road = r;
        this.location = loc;
        this.side = s;
        this.lanes = la;
    }

    public Configuration(osAgent outer) {
        this.outer = outer;
        this.AID = null;
        this.road = null;
        this.location = 0;
        this.side = null;
        this.lanes = 0;
    }

    public Configuration() {
        this.outer = null;
        this.AID = null;
        this.road = null;
        this.location = 0;
        this.side = null;
        this.lanes = 0;
    }

    public Configuration(String input) {
        JSONObject content = new JSONObject(input);
        this.outer = null;
        this.AID = new AID(content.getString("AID"), jade.core.AID.ISGUID);
        this.road = content.getString("road");
        this.location = content.getDouble("location");
        this.side = content.getString("side");
        this.lanes = content.getInt("lanes");
    }

    public JSONObject configToJSON() {
        JSONObject content = new JSONObject();
        content.put("road", this.road);
        content.put("location", this.location);
        content.put("side", this.side);
        content.put("AID", this.AID.getName());
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
        return c1.AID.equals(c2.AID);
    }

    /**
     * @return the aID
     */
    public AID getAID() {
        return this.AID;
    }

    /**
     * @param aID the aID to set
     */
    public void setAID(AID aID) {
        this.AID = aID;
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
    public void setLocation(double location) {
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