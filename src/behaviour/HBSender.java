package behaviour;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class HBSender extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HBSender(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }
    
    @Override
    protected void onTick() {
        osAgent myOsAgent = (osAgent) myAgent;
        ACLMessage HBRequest = new ACLMessage(ACLMessage.REQUEST);
        HBRequest.setOntology("HB");
        HBRequest.addReceiver(myOsAgent.getUpstream().getAID());
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("configuration", outer.getLocal().configToJSON());
        jsonContent.put("measures",new JSONArray(outer.getLocalMeasures()));
        HBRequest.setContent(jsonContent.toString());
        myOsAgent.send(HBRequest);
    }
}