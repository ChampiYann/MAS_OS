package behaviour;

import java.util.NoSuchElementException;
import java.util.Vector;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import measure.MSI;

public class ConfigurationResponder extends AchieveREResponder {

    private static final long serialVersionUID = 1L;
    private osAgent outer;

    public ConfigurationResponder(Agent a, MessageTemplate mt) {
        super(a, mt);
        outer = (osAgent)a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }
    
    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        Configuration newConfig = new Configuration();

        newConfig.getConfigFromJSON(request.getContent());
        if (outer.getLocal().location - newConfig.location < outer.getLocal().location - outer.getUpstream().location && outer.getLocal().location - newConfig.location > 0) {
            outer.getUpstream().location = newConfig.location;
            outer.getUpstream().road = newConfig.road;
            outer.getUpstream().getAID = newConfig.getAID;
            outer.getUpstream().side = newConfig.side;
            outer.getUpstream().lanes = newConfig.lanes;

            outer.setUpstreamMsi(new Vector<MSI>(outer.getUpstream().lanes));
            for (int i = 0; i < outer.getUpstreamMsi().capacity(); i++) {
                outer.getUpstreamMsi().add(new MSI());
            }

            outer.resetTimeUpstream();

            outer.addBehaviour(new BrainBehaviour((osAgent)myAgent));

            System.out.println("upstream neighbour for " + outer.getLocal().getAID.getLocalName() + " is " + outer.getUpstream().getAID.getLocalName());

            outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, MSI.MsiToJson(outer.getMsi()));

            try {
                outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, outer.getCentralMeasures().firstElement().toJSON().toString());
            } catch (NoSuchElementException e) {
                //No measures to send
            }

            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent(outer.getLocal().configToJSON());
            return result;
        } else {
            // throw new FailureException("sub-optimal");
            return null;
        }
    }
}