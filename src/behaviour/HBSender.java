package behaviour;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class HBSender extends TickerBehaviour {

    private static final long serialVersionUID = 1L;
    
    private osAgent outer;

    public HBSender(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }
    
    @Override
    protected void onTick() {
        ACLMessage HBRequest = new ACLMessage(ACLMessage.REQUEST);
        HBRequest.setOntology("HB");
        HBRequest.addReceiver(outer.getUpstream().getAID);
        HBRequest.setContent(outer.getLocal().configToJSON());

        myAgent.send(HBRequest);
    }

}