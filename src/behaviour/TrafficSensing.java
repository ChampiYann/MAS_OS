package behaviour;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public class TrafficSensing extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public TrafficSensing(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }

    @Override
    protected void onTick() {
        Object input = outer.getO2AObject();
        if (input != null) {
            boolean newCongestion = (Boolean)input;
            if (newCongestion == true && outer.getCongestion() == false) {
                outer.setCongestion(true);
                outer.addBehaviour(new CompilerBehaviour(outer));
                System.out.println("Congestion detected!");

            } else if (newCongestion == false && outer.getCongestion() == true) {
                outer.setCongestion(false);
                outer.addBehaviour(new CompilerBehaviour(outer));
                System.out.println("Congestion cleared!");

            }
        }
    }
}