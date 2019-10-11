package behaviour;

import java.time.LocalDateTime;
import java.util.Vector;

import agents.centralAgent;
import jade.core.behaviours.CyclicBehaviour;

public class InputHandlerBehaviour extends CyclicBehaviour {

    public static final int CONGESTION = 0;
    public static final int MSI        = 1;
    public static final int LOG        = 2;

    private static final long serialVersionUID = 1L;
    private centralAgent outer;

    public InputHandlerBehaviour(centralAgent outer) {
        super(outer);
        this.outer = outer;
    }

    @Override
    public void action() {
        Object input = myAgent.getO2AObject();
        if (input != null) {
            Vector<Object> inputVector = (Vector<Object>) input;
            LocalDateTime dateTime = (LocalDateTime) inputVector.firstElement();
            outer.updateGuiTime(dateTime);
            int type = (Integer) inputVector.get(1);
            if (type == CONGESTION) {
                float location = (Float) inputVector.get(2);
                boolean congestion = (Boolean) inputVector.get(3);
                outer.updateGuiCongestion(location,congestion);
            } else if (type == MSI) {
                float location = (Float) inputVector.get(2);
                String[] symbols = (String[]) inputVector.get(3);
                outer.updateGuiMsi(location, symbols);
            } else if (type == LOG) {
                outer.guiLog();
            }
        }
    }
}