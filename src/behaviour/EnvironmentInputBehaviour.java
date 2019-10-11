package behaviour;

import agents.osAgent;
// import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;

public class EnvironmentInputBehaviour extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public EnvironmentInputBehaviour (osAgent outer, long period) {
        super(outer,period);
        this.outer = outer;
    }

    @Override
    protected void onTick() {
        Object input = outer.getO2AObject();
        if (input != null) {
            boolean newCongestion = (Boolean)input;

            if (newCongestion == true && outer.getCongestion() == false) {
                outer.setCongestion(true);
                myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
                System.out.println("Congestion detected!");

            } else if (newCongestion == false && outer.getCongestion() == true) {
                outer.setCongestion(false);
                myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
                System.out.println("Congestion cleared!");
            }
        }
    }

    // @Override
    // public void action() {
    //     Object input = outer.getO2AObject();
    //     if (input != null) {
    //         myAgent.addBehaviour(new TrafficSensorBehaviour(outer,input));
    //     }
    // }

}