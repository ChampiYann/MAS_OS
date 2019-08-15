package behaviour;

import org.json.JSONObject;

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
                // outer.setUpstream(new Configuration());
                // outer.getUpstream().location = Double.NEGATIVE_INFINITY;
                outer.getDownstream().remove(0);
                outer.getDownstream().add(new Configuration());
                outer.getDownstream().lastElement().location = Double.POSITIVE_INFINITY;
                // outer.SendConfig();
            }
        } else {
            if (HBResponse.getContent() != null) {
                Configuration newConfig = new Configuration();
                JSONObject jsonContent = new JSONObject(HBResponse.getContent());
                jsonContent.remove("congestion"); 
                newConfig.getConfigFromJSON(jsonContent.toString());
                outer.getDownstream().set(1, newConfig);
                // System.out.println("down " + outer.getLocal().location + ": " + outer.getDownstream().firstElement().location + ", " + outer.getDownstream().lastElement().location);
            }
            outer.resetTimeUpstream();
        }
    }
}