package behaviour;

import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import measure.Measure;

public class HBSender extends TickerBehaviour {

    private static final long serialVersionUID = 1L;
    
    private osAgent outer;

    public HBSender(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }
    
    @Override
    protected void onTick() {
        ACLMessage HBRequest;
        for (int i = 0; i < outer.getConfig().size()/2; i++) {
            HBRequest = new ACLMessage(ACLMessage.REQUEST);
            HBRequest.setOntology("HB");
        
            HBRequest.addReceiver(outer.getConfig().get(i).getAID());
            HBRequest.setConversationId(Long.toString(outer.getConfig().get(i).getConvID()));

            JSONObject jsonContent = new JSONObject();
            jsonContent.put("configuration", outer.getLocal().configToJSON());
            jsonContent.put("measures", new JSONArray(outer.getLocalMeasures().stream().filter(n -> n.getType() != Measure.REACTION).collect(Collectors.toList())));
            HBRequest.setContent(jsonContent.toString());
            HBRequest.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
            myAgent.send(HBRequest);
        }
    }
}