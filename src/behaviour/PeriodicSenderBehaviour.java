package behaviour;

import agents.osAgent;
import config.UpstreamNeighbour;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;

public class PeriodicSenderBehaviour extends TickerBehaviour {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // Neighbour
    private UpstreamNeighbour neighbour;

    public PeriodicSenderBehaviour(Agent a, long period, UpstreamNeighbour neighbour) {
        super(a, period);
        this.neighbour = neighbour;
    }

    @Override
    protected void onTick() {
        osAgent outer = (osAgent)myAgent;
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("HB");
        newMsg.setContent(Double.toString(outer.getLocal().getLocation()*10));
        newMsg.addReceiver(this.neighbour.getConfig().getAID());
        newMsg.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));

        myAgent.addBehaviour(new SenderBehaviour(myAgent, newMsg, neighbour));
    }
    
}