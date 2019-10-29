package behaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.json.JSONArray;
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
import measure.Measure;

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
        int indexRev = - index - 1;

        if ((index >= 0 & index <= osAgent.nsize-1) | (indexRev >= 0 & indexRev <= osAgent.nsize-1)) {
            // get measures
            JSONArray jsonMeasures = jsonContent.getJSONArray("measures");
            Iterator<Object> jsonMeasuresIterator = jsonMeasures.iterator();
            ArrayList<Measure> newMeasures = new ArrayList<Measure>();
            while (jsonMeasuresIterator.hasNext()) {
                newMeasures.add(new Measure((JSONObject)jsonMeasuresIterator.next()));
            }

            try {
                // outer.getMsi()[index] = new MSI((int)(newConfig.getLocation()*10));
                // outer.getDownstreamNeighbours().get(index).setConfig(newConfig);
                outer.getDownstreamNeighbours().get(index).resetTimeout();
                outer.getDownstreamNeighbours().get(index).setMeasures(newMeasures);
            } catch (ArrayIndexOutOfBoundsException e) {
                // outer.getMsi()[indexRev] = new MSI((int)(newConfig.getLocation()*10));
                outer.getDownstreamNeighbours().get(indexRev).setConfig(newConfig);
                outer.getDownstreamNeighbours().get(indexRev).resetTimeout();
                outer.getDownstreamNeighbours().get(indexRev).setMeasures(newMeasures);
            }

            // run new msi calculations
            myAgent.addBehaviour(new CompilerBehaviour());
            // return message
            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);
            // add measures as content
            jsonContent = new JSONObject();
            jsonContent.put("measures", new JSONArray(outer.getLocalMeasures().stream().filter(n -> n.getType() != Measure.REACTION).collect(Collectors.toList())));
            result.setContent(jsonContent.toString());
            result.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
            return result;
        } else {
            return null;
        }
    }
}