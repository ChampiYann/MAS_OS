package behaviour;

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
        JSONObject jsonContent = new JSONObject(request.getContent());
        JSONArray jsonCongestion = jsonContent.getJSONArray("congestion");
        jsonContent.remove("congestion"); 
        newConfig.getConfigFromJSON(jsonContent.toString());
        if (newConfig.location < outer.getLocal().location &&
        newConfig.location > outer.getUpstream().location) {
            outer.getUpstream().location = newConfig.location;
            outer.getUpstream().road = newConfig.road;
            outer.getUpstream().getAID = newConfig.getAID;
            outer.getUpstream().side = newConfig.side;
            outer.getUpstream().lanes = newConfig.lanes;

            outer.resetTimeUpstream();

            System.out.println("up " + outer.getLocal().location + ": " + outer.getUpstream().location);

            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent(outer.getLocal().configToJSON().toString());
            return result;
        } else if (newConfig.location > outer.getLocal().location &&
        newConfig.location < outer.getDownstream().firstElement().location) {
        // !Configuration.ConfigurationEqual(newConfig, outer.getDownstream().firstElement())) {
            // outer.getDownstream().removeElement(outer.getDownstream().lastElement());
            outer.getDownstream().set(0,newConfig);
            // outer.getDownstream().sort(Configuration.kmCompare);

            outer.getCongestion().set(1, jsonCongestion.getBoolean(0));
            outer.getCongestion().set(2, jsonCongestion.getBoolean(1));

            outer.resetTimeDownstream();
            System.out.println("down " + outer.getLocal().location + ": " + outer.getDownstream().firstElement().location + ", " + outer.getDownstream().lastElement().location);

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