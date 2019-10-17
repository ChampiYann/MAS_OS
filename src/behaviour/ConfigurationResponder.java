package behaviour;

import java.util.ArrayList;
import java.util.Collections;

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
// import measure.MSI;

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
        // Configuration newConfig = new Configuration(request.getContent());
        JSONObject jsonContent = new JSONObject(request.getContent());

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
                // somewhere in the array
                allConfig.add(index, newConfig);
                allConfig.get(index).setConvID(System.currentTimeMillis());
                if (index < allConfig.size()/2) {
                    allConfig.remove(0);
                } else {
                    allConfig.remove(allConfig.size()-1);
                }
                outer.resetTime(index);
            }
        // } else if (index != allConfig.size()/2) {
        //     // In array
        //     outer.resetTime(index);
        } else {
            return null;
        }

        // System.out.println("upstream neighbour for " + outer.getLocal().getAID().getLocalName() + " is " + outer.getConfig().get(index).getAID().getLocalName());

        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);
        result.setContent(outer.getLocal().configToJSON().toString());
        return result;


        // if (outer.getLocal().location - newConfig.location < outer.getLocal().location - outer.getUpstream().location && outer.getLocal().location - newConfig.location > 0) {
        //     outer.setUpstream(newConfig);

        //     outer.setUpstreamMsi(new ArrayList<MSI>(outer.getUpstream().lanes));
        //     for (int i = 0; i < outer.getUpstream().lanes; i++) {
        //         outer.getUpstreamMsi().add(new MSI());
        //     }

        //     outer.resetTimeUpstream();

        //     outer.addBehaviour(new BrainBehaviour((osAgent)myAgent));

        //     System.out.println("upstream neighbour for " + outer.getLocal().getAID.getLocalName() + " is " + outer.getUpstream().getAID.getLocalName());

        //     outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, MSI.MsiToJson(outer.getMsi()));

        //     try {
        //         outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, outer.getCentralMeasures().get(0).toJSON().toString());
        //     } catch (IndexOutOfBoundsException e) {
        //         //No measures to send
        //     }

        //     ACLMessage result = request.createReply();
        //     result.setPerformative(ACLMessage.INFORM);
        //     result.setContent(outer.getLocal().configToJSON().toString());
        //     return result;
        // } else {
        //     // throw new FailureException("sub-optimal");
        //     return null;
        // }
    }
}