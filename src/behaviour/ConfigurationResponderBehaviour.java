package behaviour;

import java.util.Collections;

import org.json.JSONObject;

import agents.osAgent;
import config.Configuration;
import config.UpstreamNeighbour;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
// import measure.MSI;

public class ConfigurationResponderBehaviour extends AchieveREResponder {

    private static final long serialVersionUID = 1L;
    private osAgent outer;

    public ConfigurationResponderBehaviour(Agent a, MessageTemplate mt) {
        super(a, mt);
        outer = (osAgent)a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }
    
    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        // Configuration newConfig = new Configuration(request.getContent());
        JSONObject jsonContent = new JSONObject(request.getContent());

        // check location of sender
        JSONObject jsonConfiguration = jsonContent.getJSONObject("configuration");
        Configuration newConfig = new Configuration(jsonConfiguration.toString());
        UpstreamNeighbour newNeighbour = new UpstreamNeighbour((osAgent)myAgent, newConfig);

        int index  = Collections.binarySearch(outer.getUpstreamNeighbours(), newNeighbour);
        index = -index-1;
        if (index > 0 & index <= osAgent.nsize & newConfig.compareTo(outer.getLocal()) < 0) {
            // outer.getUpstreamNeighbours().get(index-1).setConfig(newConfig);
            // outer.getUpstreamNeighbours().get(index-1).resetTimeout();
            // outer.getUpstreamNeighbours().get(index-1).resetTicker();
            outer.getUpstreamNeighbours().add(index,newNeighbour);
            outer.getUpstreamNeighbours().get(index).addBehaviour();
            outer.getUpstreamNeighbours().get(0).removeSender();
            outer.getUpstreamNeighbours().get(0).removeTimeout();
            outer.getUpstreamNeighbours().remove(0);

            // outer.getUpstreamNeighbours().get(index-1).addBehaviour();

            // Broadcast central measures

            // Reply to message
            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);
            return result;
        } else {
            return null;
        }
    }
}