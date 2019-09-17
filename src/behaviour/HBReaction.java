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
import measure.MSI;

public class HBReaction extends TickerBehaviour {

    private static final long serialVersionUID = 1L;
    
    private osAgent outer;

    public HBReaction(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }
    
    @Override
    protected void onTick() {
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBResponse = myAgent.receive(HBTemplate);
        if (HBResponse != null) {
            JSONObject jsonContent = new JSONObject(HBResponse.getContent());
            outer.getUpstreamMsi().clear();
            JSONArray jsonArray = jsonContent.getJSONArray("msi");
            Iterator<Object> iteratorContent = jsonArray.iterator();
            while (iteratorContent.hasNext()) {
                JSONObject jsonMSI = (JSONObject)iteratorContent.next();
                outer.getUpstreamMsi().add(new MSI(jsonMSI.getInt("symbol")));
            }
            outer.addBehaviour(new BrainBehaviour(outer));

            outer.resetTimeUpstream();
        } else {
            if (System.currentTimeMillis()-outer.getTimeUpstream() > (long)osAgent.minute*2) {
                // System.out.println("Upstream down at " + local.getAID.getLocalName());
                outer.setUpstream(new Configuration());
            }
        }
    }
}