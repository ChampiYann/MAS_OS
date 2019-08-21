package measure;

import java.util.Iterator;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;

public class CentralMeasure {

    // private osAgent outer;
    private float start;
    private float end;
    private Vector<MSI> centralMsi;
    private long ID;

    public CentralMeasure(osAgent outer, Vector<MSI> sym, float start, float end, long ID) {
        this.start = start;
        this.end =  end;
        this.centralMsi = sym;
        this.ID = ID;
    }

    /**
     * @return the iD
     */
    public long getID() {
        return ID;
    }

    /**
     * @return the end
     */
    public float getEnd() {
        return end;
    }

    /**
     * @return the start
     */
    public float getStart() {
        return start;
    }

    /**
     * @return the msi
     */
    public Vector<MSI> getMsi() {
        return centralMsi;
    }

    public JSONObject toJSON() {
        JSONObject content = new JSONObject();
        content.put("start", start);
        content.put("end", end);
        content.put("ID", ID);
        Iterator<MSI> inputIterator = centralMsi.iterator();
        JSONArray outputArray = new JSONArray();
        while(inputIterator.hasNext()) {
            outputArray.put(inputIterator.next().getSymbol());
        }
        content.put("msi", outputArray);
        return content;
    }
}