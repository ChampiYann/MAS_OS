package behaviour;

import agents.osAgent;
import config.UpstreamNeighbour;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;

public class TimeoutBehaviour extends WakerBehaviour {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    
    // Neighbour
    private UpstreamNeighbour neighbour;

    public TimeoutBehaviour(Agent a, long timeout, UpstreamNeighbour neighbour) {
        super(a, timeout);
        this.neighbour = neighbour;
    }

    @Override
    protected void onWake() {
        osAgent outer = (osAgent)myAgent;
        neighbour.removeSender();
        outer.getUpstreamNeighbours().remove(neighbour);
        outer.getUpstreamNeighbours().add(0,new UpstreamNeighbour(outer));
    }
    
}