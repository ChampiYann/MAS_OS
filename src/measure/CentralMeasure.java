package measure;

import java.util.Vector;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;

public class CentralMeasure extends Measure {

    private int order;
    
    public CentralMeasure(long ID, Vector<Integer>lanes, int order) {
        this.ID = ID;
        this.lanes = lanes;
        this.order = order;
    }

    public CentralMeasure(osAgent outer, long ID, JSONArray lanes, int order, double start, double end) {
        this.ID = ID;
        this.order = order;
        this.lanes = new Vector<Integer>(outer.getLocal().lanes);
        this.start = start;
        this.end = end;

        // int applyTo = lanes.getInt(0) - 1;
        // int valueToApply = lanes.getInt(1);

        // for (int i = 0; i < this.lanes.capacity(); i++) {
        //     // if (i == applyTo) {
        //     //     this.lanes.add(valueToApply);
        //     // }
        //     // else {
        //     //     if (valueToApply == 0) {
        //     //         this.lanes.add(MSI.NF_70);
        //     //     } else {
        //     //         this.lanes.add(valueToApply + 2);
        //     //     }
        //     // }
        //     this.lanes.add(lanes.getInt(i));
        // }

        Iterator<Object> lanesIterator = lanes.iterator();
        while (lanesIterator.hasNext()) {
            this.lanes.add((Integer)lanesIterator.next());
        }
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    public JSONObject toJSON() {
        JSONObject content = new JSONObject();
        content.put("start", start);
        content.put("end", end);
        content.put("ID",ID);
        JSONArray lanesJSON = new JSONArray(lanes);
        content.put("lanes", lanesJSON);
        return content;
    }
}