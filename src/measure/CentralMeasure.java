package measure;

import java.util.Vector;

import agents.osAgent;

public class CentralMeasure {

    // private osAgent outer;
    private float start;
    private float end;
    private Vector<MSI> msi;
    private long ID;

    public CentralMeasure(osAgent outer, Vector<MSI> sym, float start, float end, long ID) {
        // this.outer = outer;
        this.start = start;
        this.end =  end;
        this.msi = sym;
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
        return msi;
    }
}