package behaviour;

// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Collections;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
// import measure.MSI;
import measure.Measure;

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
        boolean update = false;

        while (HBResponse != null) {
            JSONObject jsonContent = new JSONObject(HBResponse.getContent());

            String convIDString = HBResponse.getConversationId();
            Long convID = Long.parseLong(convIDString);
            for (int i = 0; i < outer.getConfig().size(); i++) {
                if (outer.getConfig().get(i).getConvID() == convID) {
                    update = true;
                    JSONArray jsonMeasures = jsonContent.getJSONArray("measures");
                    Iterator<Object> jsonMeasuresIterator = jsonMeasures.iterator();
                    outer.getMeasures()[i].clear();
                    while (jsonMeasuresIterator.hasNext()) {
                        outer.getMeasures()[i].add(new Measure((JSONObject)jsonMeasuresIterator.next()));
                    }
                    outer.resetTime(i);
                }
            }
            HBResponse = myAgent.receive(HBTemplate);
        }

        if (update) {
            outer.addBehaviour(new CompilerBehaviour());
        }

        for (int i = 0; i < outer.getTimers().length/2; i++) {
            if (System.currentTimeMillis()-outer.getTimers()[i] > osAgent.timeout) {
                outer.getConfig().remove(i);
                outer.getConfig().add(0,new Configuration());
            }
        }



        // if (HBResponse != null) {
        //     JSONObject jsonContent = new JSONObject(HBResponse.getContent());
        //     outer.getUpstreamMsi().clear();
        //     JSONArray jsonArray = jsonContent.getJSONArray("msi");
        //     Iterator<Object> iteratorContent = jsonArray.iterator();
        //     while (iteratorContent.hasNext()) {
        //         JSONObject jsonMSI = (JSONObject)iteratorContent.next();
        //         outer.getUpstreamMsi().add(new MSI(jsonMSI.getInt("symbol")));
        //     }
        //     outer.addBehaviour(new BrainBehaviour(outer));

        //     outer.resetTimeUpstream();
        // } else {
        //     if (System.currentTimeMillis()-outer.getTimeUpstream() > (long)osAgent.minute*2) {
        //         // System.out.println("Upstream down at " + local.getAID.getLocalName());
        //         outer.setUpstream(new Configuration());
        //     }
        // }
    }
}