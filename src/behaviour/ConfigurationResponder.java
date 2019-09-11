package behaviour;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

public class ConfigurationResponder extends AchieveREResponder {

    private static final long serialVersionUID = 1L;
    private osAgent outer;

    public ConfigurationResponder(Agent a, MessageTemplate mt) {
        super(a, mt);
        this.outer = (osAgent)a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }
    
    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);

        Configuration newConfig = new Configuration(request.getContent());
        if (newConfig.getLocation() < outer.getLocal().getLocation() && newConfig.getLocation() > outer.getUpstream().getLocation()) {

            outer.setUpstream(newConfig);
            outer.resetTimeUpstream();

            System.out.println("upstream neighbour for " + outer.getLocal().getAID().getLocalName() + " is " + outer.getUpstream().getAID().getLocalName());
            return result;
        // } else if (newConfig.getLocation() > outer.getLocal().getLocation() && newConfig.getLocation() < outer.getDownstream().getLocation()) {

        //     outer.setDownstream(newConfig);
        //     outer.resetTimeDownstream();

        //     System.out.println("downstream neighbour for " + outer.getLocal().getAID().getLocalName() + " is " + outer.getDownstream().getAID().getLocalName());
        //     return result;
        } else {
            return null;
        }
    }
}