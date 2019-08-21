package behaviour;

import java.util.Vector;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import measure.MSI;

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
            setDownstream(HBRequest);
            ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
            HBResponse.setOntology("HB");
            HBResponse.addReceiver(HBRequest.getSender());
            myAgent.send(HBResponse);
            outer.resetTimeDownstream();
        } else {
            if (System.currentTimeMillis()-outer.getTimeDownstream() > (long)osAgent.minute*2) {
                outer.SendConfig();
            }
        }
    }

    private void setDownstream(ACLMessage HBRequest) {
        Configuration tempConfig = new Configuration(outer);
        tempConfig.getConfigFromJSON(HBRequest.getContent());
        if (!Configuration.ConfigurationEqual(tempConfig, outer.getDownstream())) {
            outer.getDownstream().getConfigFromJSON(HBRequest.getContent());
            outer.setDownstreamMsi(new Vector<MSI>(outer.getDownstream().lanes));
            for (int i = 0; i < outer.getDownstreamMsi().capacity(); i++) {
                outer.getDownstreamMsi().add(new MSI());
            }
            outer.sendMeasure(outer.getDownstream(), osAgent.DISPLAY, MSI.MsiToJson(outer.getMsi()));
            outer.addBehaviour(new BrainBehaviour(outer));
            System.out.println("downstream neighbour for " + outer.getLocal().getAID.getLocalName() + " is " + outer.getDownstream().getAID.getLocalName());
        }
    }
}