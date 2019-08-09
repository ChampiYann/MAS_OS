package behaviour;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class HBReaction extends TickerBehaviour {

    private static final long serialVersionUID = 1L;
    
    private osAgent outer;

    public HBReaction(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }
    
    @Override
    protected void onTick() {
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBResponse = myAgent.receive(HBTemplate);
        if (HBResponse == null) {
            if (System.currentTimeMillis()-outer.getTimeUpstream() > (long)osAgent.minute*2) {
                // System.out.println("Upstream down at " + local.getAID.getLocalName());
                outer.setUpstream(new Configuration());
            }
        } else {
            outer.resetTimeUpstream();
        }
    }
}