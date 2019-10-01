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

public class HBReaction extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    public HBReaction(Agent a, long period) {
        super(a, period);
    }
    
    @Override
    protected void onTick() {
        osAgent myOsAgent = (osAgent) myAgent;
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBResponse = myAgent.receive(HBTemplate);
        if (HBResponse != null) {
            JSONObject jsonContent = new JSONObject(HBResponse.getContent());
            JSONArray jsonArray = jsonContent.getJSONArray("measures");
            Iterator<Object> jsonContentIterator = jsonArray.iterator();
            myOsAgent.getUpstreamMeasures().clear();
            while (jsonContentIterator.hasNext()) {
                myOsAgent.getUpstreamMeasures().add(new Measure((JSONObject)jsonContentIterator.next()));
            }
            myOsAgent.addBehaviour(new CompilerBehaviour(myOsAgent));

            myOsAgent.resetTimeUpstream();
        } else {
            if (System.currentTimeMillis()-myOsAgent.getTimeUpstream() > (long)osAgent.minute*2) {
                myOsAgent.setUpstream(new Configuration());
            }
        }
    }
}