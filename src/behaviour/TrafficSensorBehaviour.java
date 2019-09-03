package behaviour;

import agents.osAgent;
import jade.core.behaviours.OneShotBehaviour;

public class TrafficSensorBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;
    private Object input;

    public TrafficSensorBehaviour (osAgent outer, Object input) {
        super(outer);
        this.outer = outer;
        this.input = input;
    }

    @Override
    public void action() {
        // Object input = outer.getO2AObject();
        // if (input != null) {
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
        // }
    }

}