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

public class HBResponder extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HBResponder(Agent a, long period) {
        super(a, period);
        outer = (osAgent)a;
    }

    @Override
    protected void onTick() {
        MessageTemplate HBTemplate = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchOntology("HB"));
        ACLMessage HBRequest = myAgent.receive(HBTemplate);
        if (HBRequest != null) {
            JSONObject jsonContent = new JSONObject(HBRequest.getContent());
            outer.setDownstream(new Configuration(jsonContent.getJSONObject("configuration").toString()));

            outer.getDownstreamMsi().clear();
            JSONArray jsonArray = jsonContent.getJSONArray("msi");
            Iterator<Object> iteratorContent = jsonArray.iterator();
            while (iteratorContent.hasNext()) {
                JSONObject jsonMSI = (JSONObject)iteratorContent.next();
                outer.getDownstreamMsi().add(new MSI(jsonMSI.getInt("symbol")));
            }
            outer.addBehaviour(new BrainBehaviour(outer));

            ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
            HBResponse.setOntology("HB");
            HBResponse.addReceiver(HBRequest.getSender());

            jsonContent = new JSONObject();
            jsonContent.put("msi", outer.getMsi());
            HBResponse.setContent(jsonContent.toString());

            HBResponse.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
            myAgent.send(HBResponse);
            outer.resetTimeDownstream();
        } else {
            if (System.currentTimeMillis()-outer.getTimeDownstream() > osAgent.timeout) {
                outer.SendConfig();
            }
        }
    }
}