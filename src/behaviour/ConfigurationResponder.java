package behaviour;

import java.util.ArrayList;

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
        Configuration newConfig = new Configuration(request.getContent());
        if (outer.getLocal().location - newConfig.location < outer.getLocal().location - outer.getUpstream().location && outer.getLocal().location - newConfig.location > 0) {
            // Update config
            outer.setUpstream(newConfig);

            // Reset associated MSI
            outer.setUpstreamMsi(new ArrayList<MSI>(outer.getUpstream().lanes));
            for (int i = 0; i < outer.getUpstream().lanes; i++) {
                outer.getUpstreamMsi().add(new MSI());
            }

            // Reset associated timer
            outer.resetTimeUpstream();

            // Restart sender behaviour
            outer.getHBSenderBehaviour().restart();

            // Recalculate MSI
            outer.addBehaviour(new BrainBehaviour((osAgent)myAgent));

            // Print statement
            // System.out.println("upstream neighbour for " + outer.getLocal().getAID.getLocalName() + " is " + outer.getUpstream().getAID.getLocalName());

            // Send current MSI
            // outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, MSI.MsiToJson(outer.getMsi()));

            // Send applicable central measures
            outer.getCentralMeasures().stream().forEach(n -> {
                outer.sendMeasure(new Configuration(outer, outer.getTopicCentral(), null, 0, null, 0) , "ADD", n.toJSON().toString());
            });

            // Reply to message
            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent(outer.getLocal().configToJSON().toString());
            return result;
        } else {
            // throw new FailureException("sub-optimal");
            return null;
        }
    }
}