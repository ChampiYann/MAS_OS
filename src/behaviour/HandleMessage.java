package behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import measure.Measure;

public class HandleMessage extends AchieveREResponder {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HandleMessage(Agent a, MessageTemplate mt) {
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

        JSONObject jsonContent = new JSONObject(request.getContent());

        if (request.getSender().equals(outer.getCentral())) {
            JSONArray jsonArray = jsonContent.getJSONArray("measures");
            Iterator<Object> jsonContentIterator = jsonArray.iterator();
            outer.getCentralMeasures().clear();
            while (jsonContentIterator.hasNext()) {
                outer.getCentralMeasures().add(new Measure((JSONObject)jsonContentIterator.next()));
            }
            // myAgent.addBehaviour(new CompilerBehaviour());
            return result;
        }

        // check location of sender
        JSONObject jsonConfiguration = jsonContent.getJSONObject("configuration");
        Configuration newConfig = new Configuration(jsonConfiguration.toString());

        ArrayList<Configuration> allConfig = outer.getConfig();
        int index = Collections.binarySearch(allConfig, newConfig);
        if (index < 0) {
            // Not in array
            index = -index -1;
            // check where in array
            if (index == 0 | index == allConfig.size()) {
                // first or last element
                return null;
            } else {
                // somewhere in th array
                allConfig.add(index, newConfig);
                if (index < allConfig.size()/2) {
                    allConfig.remove(0);
                } else {
                    allConfig.remove(allConfig.size()-1);
                }
                outer.resetTime(index);
            }
        } else if (index != allConfig.size()/2) {
            // In array
            outer.resetTime(index);
        } else {
            return null;
        }

        JSONArray jsonMeasures = jsonContent.getJSONArray("measures");
        Iterator<Object> jsonMeasuresIterator = jsonMeasures.iterator();
        outer.getMeasures()[index].clear();
        while (jsonMeasuresIterator.hasNext()) {
            outer.getMeasures()[index].add(new Measure((JSONObject)jsonMeasuresIterator.next()));
        }
        return result;

        // if (request.getSender().equals(outer.getDownstream().getAID())) {
        //     Iterator<Object> jsonContentIterator = jsonContent.iterator();
        //     outer.getDownstreamMeasures().clear();
        //     while (jsonContentIterator.hasNext()) {
        //         outer.getDownstreamMeasures().add(new Measure((JSONObject)jsonContentIterator.next()));
        //     }
        //     // myAgent.addBehaviour(new CompilerBehaviour());
        //     return result;
        // } else if (request.getSender().equals(outer.getUpstream().getAID())) {
        //     Iterator<Object> jsonContentIterator = jsonContent.iterator();
        //     outer.getUpstreamMeasures().clear();
        //     while (jsonContentIterator.hasNext()) {
        //         outer.getUpstreamMeasures().add(new Measure((JSONObject)jsonContentIterator.next()));
        //     }
        //     // myAgent.addBehaviour(new CompilerBehaviour());
        //     return result;
        // } else {
        //     return null;
        // }
    }
}