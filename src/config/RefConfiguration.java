package config;

import agents.osAgent;
import jade.core.AID;

public class RefConfiguration extends Configuration {
    public String fileName;

    public RefConfiguration() {
        super();
        fileName = null;
    }

    public RefConfiguration(osAgent outer, AID id, String r, float loc, String s, int la, String fn) {
        super(outer, id, r, loc, s, la);
        fileName = fn;
    }
}