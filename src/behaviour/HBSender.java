package behaviour;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class HBSender extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    public HBSender(Agent a, long period) {
        super(a, period);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void onTick() {
        osAgent myOsAgent = (osAgent) myAgent;
        ACLMessage HBRequest = new ACLMessage(ACLMessage.REQUEST);
        HBRequest.setOntology("HB");
        HBRequest.addReceiver(myOsAgent.getUpstream().getAID());
        HBRequest.setContent(myOsAgent.getLocal().configToJSON().toString());
        myOsAgent.send(HBRequest);
    }
}