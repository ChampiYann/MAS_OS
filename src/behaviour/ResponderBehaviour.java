package behaviour;

import java.net.Authenticator.RequestorType;
import java.util.Collections;

import org.json.JSONObject;

import agents.osAgent;
import config.Configuration;
import config.DownstreamNeighbour;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import measure.MSI;

public class ResponderBehaviour extends AchieveREResponder {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public ResponderBehaviour(Agent a, MessageTemplate mt) {
        super(a, mt);
    }
    
    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        osAgent outer = (osAgent)myAgent;

        JSONObject jsonContent = new JSONObject(request.getContent());
        JSONObject jsonConfiguration = jsonContent.getJSONObject("configuration");
        Configuration newConfig = new Configuration(jsonConfiguration.toString());
        DownstreamNeighbour newNeighbour = new DownstreamNeighbour((osAgent)myAgent, newConfig);
        // get index of presumed location
        int index = Collections.binarySearch(outer.getDownstreamNeighbours(), newNeighbour);

        if (index >= 0 & index <= osAgent.nsize-1) {
            // Initialize new MSI array
            outer.getMsi()[index] = new MSI((int)(newConfig.getLocation()*10));
            outer.getDownstreamNeighbours().get(index).resetTimeout();
        } else {
            index = -index -1;
            if (index >= 0 & index <= osAgent.nsize-1) {
                outer.getMsi()[index] = new MSI((int)(newConfig.getLocation()*10));
                outer.getDownstreamNeighbours().get(index).setConfig(newConfig);
                outer.getDownstreamNeighbours().get(index).resetTimeout();
            }
        }

        myAgent.addBehaviour(new CompilerBehaviour());

        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);
        result.setContent("content");
        result.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
        return result;
    }
}