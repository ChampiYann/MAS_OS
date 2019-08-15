package behaviour;

import org.json.JSONObject;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class HBResponder extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HBResponder(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }

    @Override
    protected void onTick() {
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBRequest = myAgent.receive(HBTemplate);
        if (HBRequest != null) {
            ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
            HBResponse.setOntology("HB");
            HBResponse.addReceiver(HBRequest.getSender());
            if (outer.getDownstream().firstElement().getAID != null) {
                HBResponse.setContent(outer.getDownstream().firstElement().configToJSON().toString());
            }
            JSONObject jsonContent = new JSONObject(HBRequest.getContent());
            jsonContent.remove("congestion"); 
            outer.getUpstream().getConfigFromJSON(jsonContent.toString());
            myAgent.send(HBResponse);
            outer.resetTimeDownstream();
        } else {
            if (System.currentTimeMillis()-outer.getTimeDownstream() > (long)osAgent.minute*2) {
                // System.out.println("down at " + outer.getLocal().location);
                outer.SendConfig();
            }
        }
    }
}