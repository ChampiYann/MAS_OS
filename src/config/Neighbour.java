package config;

import java.util.ArrayList;

import agents.osAgent;
import measure.MSI;
import measure.Measure;

public class Neighbour implements Comparable<Neighbour>{

    protected osAgent outer;

    // Configuration
    protected Configuration config;

    // Conversation ID
    private long convID;

    // Measures
    private ArrayList<Measure> measures;

    // MSI
    private MSI[] msi;

    // Timeout
    private long timeout;

    public Neighbour(osAgent agent) {
        this.outer = agent;

        this.config = new Configuration(null, null, 0, null, 0);

        this.convID = System.currentTimeMillis();

        this.measures = new ArrayList<Measure>();

        this.msi = new MSI[agent.getLocal().getLanes()];
        for (int i = 0; i < agent.getLocal().getLanes(); i ++) {
            this.msi[i] = new MSI();
        }

        this.timeout = 0;
    }

    public Neighbour(osAgent agent, Double location) {
        this.outer = agent;

        this.config = new Configuration(null, null, location, null, 0);

        this.convID = System.currentTimeMillis();

        this.measures = new ArrayList<Measure>();

        this.msi = new MSI[agent.getLocal().getLanes()];
        for (int i = 0; i < agent.getLocal().getLanes(); i ++) {
            this.msi[i] = new MSI();
        }

        this.timeout = 0;
    }

    public Neighbour(osAgent agent, Configuration config) {
        this.outer = agent;

        this.config = config;

        this.convID = System.currentTimeMillis();

        this.measures = new ArrayList<Measure>();

        this.msi = new MSI[agent.getLocal().getLanes()];
        for (int i = 0; i < agent.getLocal().getLanes(); i ++) {
            this.msi[i] = new MSI();
        }

        this.timeout = 0;
    }

    @Override
    public int compareTo(Neighbour o) {
        return this.config.compareTo(o.config);
    }

    @Override
    public boolean equals(Object obj) {
        Neighbour c = (Neighbour) obj;
        return this.config.equals(c.config);
    }

    public void resetTimeout() {
        this.timeout = System.currentTimeMillis();
    }

    /**
     * @return the convID
     */
    public long getConvID() {
        return convID;
    }

    /**
     * @return the measures
     */
    public ArrayList<Measure> getMeasures() {
        return measures;
    }

    /**
     * @return the msi
     */
    public MSI[] getMsi() {
        return msi;
    }

    /**
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param convID the convID to set
     */
    public void setConvID(long convID) {
        this.convID = convID;
    }

    /**
     * @param measures the measures to set
     */
    public void setMeasures(ArrayList<Measure> measures) {
        this.measures = measures;
    }

    /**
     * @param msi the msi to set
     */
    public void setMsi(MSI[] msi) {
        this.msi = msi;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the config
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(Configuration config) {
        this.config = config;
    }
}