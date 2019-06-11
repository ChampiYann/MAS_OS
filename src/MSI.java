import org.json.JSONException;
import org.json.JSONObject;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;

class MSI {
    /**
     * This class represents the displays above the road that show the measures
     * applied. They are initialized as BLANK
     */

    final osAgent outer;

    private Measure currentState;
    private Measure centralState;
    private Measure selfState;
    private Measure downstreamState;

    private static final int CENTRAL = 1;
    private static final int SELF = 2;
    private static final int DOWNSTREAM = 3;

    public MSI(osAgent outer) {
        this.outer = outer;
        try {
            currentState = new Measure();
            centralState = new Measure();
            selfState = new Measure();
            downstreamState = new Measure();
        } catch (Exception e) {
            System.out.println("Exception in the creation of MSI");
        }
    }

    public void changeDesiredState(ACLMessage msg, JSONObject content, int select) throws RefuseException {
        long time = msg.getPostTimeStamp();
        AID sender = msg.getSender();
        int symbol = content.getInt("symbol");
        if (symbol >= -1 && symbol < 7) {

        } else {
            symbol = Measure.BLANK;
        }
        if (currentState.getSymbol() > symbol && time > currentState.getTime()) {
            centralState.update(symbol, sender, time);
        } else {
            throw new RefuseException("cannot-update");
        }
    }

    public void updateState() {
        currentState = centralState;
    }

    public Measure getState() {
        return currentState;
    }

    public Measure getState(int select) {
        switch (select) {
        case CENTRAL:
            return centralState;
        case SELF:
            return selfState;
        case DOWNSTREAM:
            return downstreamState;
        default:
            return null;
        }
    }
}