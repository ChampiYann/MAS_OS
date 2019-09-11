package behaviour;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class HBReaction extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    public HBReaction(Agent a, long period) {
        super(a, period);
    }
    
    @Override
    protected void onTick() {
        osAgent myOsAgent = (osAgent) myAgent;
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBResponse = myAgent.receive(HBTemplate);
        if (HBResponse != null) {
            myOsAgent.resetTimeUpstream();
        } else {
            if (System.currentTimeMillis()-myOsAgent.getTimeUpstream() > (long)osAgent.minute*2) {
                myOsAgent.setUpstream(new Configuration());
            }
        }
    }
}