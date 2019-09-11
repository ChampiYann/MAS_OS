package behaviour;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class HBResponder extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    public HBResponder(Agent a, long period) {
        super(a, period);
    }

    @Override
    protected void onTick() {
        osAgent myOsAgent = (osAgent) myAgent;
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBRequest = myOsAgent.receive(HBTemplate);
        if (HBRequest != null) {
            myOsAgent.setDownstream(new Configuration(HBRequest.getContent()));
            ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
            HBResponse.setOntology("HB");
            HBResponse.addReceiver(HBRequest.getSender());
            myAgent.send(HBResponse);
            myOsAgent.resetTimeDownstream();
        } else {
            if (System.currentTimeMillis()-myOsAgent.getTimeDownstream() > (long)osAgent.minute*2) {
                myOsAgent.SendConfig();
            }
        }
    }
}