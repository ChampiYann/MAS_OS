package behaviour;

import config.UpstreamNeighbour;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

public class SenderBehaviour extends AchieveREInitiator {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // Neighbour
    private UpstreamNeighbour neighbour;

    public SenderBehaviour(Agent a, ACLMessage msg, UpstreamNeighbour neighbour) {
        super(a, msg);
        this.neighbour = neighbour;
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        String msgContent = inform.getContent();
        // Do stuff with the content

        // reset timeout
        neighbour.resetTimeout();
        // calculate new MSI
        myAgent.addBehaviour(new CompilerBehaviour());
    }
}