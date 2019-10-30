package behaviour;

import agents.osAgent;
import config.DownstreamNeighbour;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;

public class TimeoutBehaviour2 extends WakerBehaviour {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    // Neighbour
    private DownstreamNeighbour neighbour;

    public TimeoutBehaviour2(Agent a, long timeout, DownstreamNeighbour neighbour) {
        super(a, timeout);
        this.neighbour = neighbour;
    }

    @Override
    protected void onWake() {
        osAgent outer = (osAgent)myAgent;
        // neighbour.removeReceiver();
        outer.getDownstreamNeighbours().remove(this.neighbour);
        outer.getDownstreamNeighbours().add(new DownstreamNeighbour(outer));


        outer.SendConfig();
    }
    
}