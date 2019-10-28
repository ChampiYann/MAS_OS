package behaviour;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import measure.Measure;

public class HBResponder extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    public HBResponder(Agent a, long period) {
        super(a, period);
    }

    @Override
    protected void onTick() {
        osAgent myOsAgent = (osAgent) myAgent;
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBRequest = myOsAgent.receive(HBTemplate);
        if (HBRequest != null) {
            JSONObject jsonContent = new JSONObject(HBRequest.getContent());
            myOsAgent.setDownstream(new Configuration(jsonContent.getJSONObject("configuration").toString()));
            // myOsAgent.setDownstream(new Configuration(HBRequest.getContent()));

            JSONArray jsonArray = jsonContent.getJSONArray("measures");
            Iterator<Object> jsonContentIterator = jsonArray.iterator();
            myOsAgent.getDownstreamMeasures().clear();
            while (jsonContentIterator.hasNext()) {
                myOsAgent.getDownstreamMeasures().add(new Measure((JSONObject)jsonContentIterator.next()));
            }
            myOsAgent.addBehaviour(new CompilerBehaviour(myOsAgent));

            ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
            HBResponse.setOntology("HB");
            HBResponse.addReceiver(HBRequest.getSender());
            
            jsonContent = new JSONObject();
            jsonContent.put("measures",new JSONArray(myOsAgent.getLocalMeasures()));
            HBResponse.setContent(jsonContent.toString());
            
            HBResponse.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
            myAgent.send(HBResponse);
            myOsAgent.resetTimeDownstream();
        } else {
            if (System.currentTimeMillis()-myOsAgent.getTimeDownstream() > osAgent.timeout) {
                myOsAgent.SendConfig();
            }
        }
    }
}