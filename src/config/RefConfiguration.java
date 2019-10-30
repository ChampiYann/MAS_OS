package config;

import jade.core.AID;

public class RefConfiguration extends Configuration {
    public String fileName;

    public RefConfiguration() {
        super();
        fileName = null;
    }

    public RefConfiguration(AID id, String r, float loc, String s, int la, String fn) {
        super(id, r, loc, s, la);
        fileName = fn;
    }
}