package behaviour;

import java.util.ArrayList;
// import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

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
        boolean update = false;

        while (HBRequest != null) {
            JSONObject jsonContent = new JSONObject(HBRequest.getContent());
            // check location of sender
            JSONObject jsonConfiguration = jsonContent.getJSONObject("configuration");
            Configuration newConfig = new Configuration(jsonConfiguration.toString());

            boolean inRange = true;
            ArrayList<Configuration> allConfig = outer.getConfig();
            int index = Collections.binarySearch(allConfig, newConfig);
            if (index < 0) {
                // Not in array
                index = -index -1;
                // check where in array
                if (index == 0 | index == allConfig.size()) {
                    // first or last element, ditch it
                    inRange = false;
                } else {
                    // somewhere in the array
                    allConfig.add(index, newConfig);
                    
                    if (index < allConfig.size()/2) {
                        allConfig.remove(0);
                    } else {
                        allConfig.remove(allConfig.size()-1);
                    }
                    // outer.resetTime(index);
                }
            } else if (index != allConfig.size()/2) {
                // In array
                // outer.resetTime(index);
            } else {
                inRange = false;
            }

            if (inRange) {
                update = true;
                JSONArray jsonMeasures = jsonContent.getJSONArray("measures");
                Iterator<Object> jsonMeasuresIterator = jsonMeasures.iterator();
                outer.getMeasures()[index].clear();
                while (jsonMeasuresIterator.hasNext()) {
                    outer.getMeasures()[index].add(new Measure((JSONObject)jsonMeasuresIterator.next()));
                }
                outer.resetTime(index);


                ACLMessage HBResponse = HBRequest.createReply();
                HBResponse.setPerformative(ACLMessage.INFORM);
                HBResponse.setOntology("HB");
                // HBResponse.addReceiver(HBRequest.getSender());

                jsonContent = new JSONObject();
                // jsonContent.put("configuration", outer.getLocal().configToJSON());
                jsonContent.put("measures", new JSONArray(outer.getLocalMeasures().stream().filter(n -> n.getType() != Measure.REACTION).collect(Collectors.toList())));
                HBResponse.setContent(jsonContent.toString());

                myAgent.send(HBResponse);
            }

            HBRequest = myAgent.receive(HBTemplate);
        }

        if (update) {
            outer.addBehaviour(new CompilerBehaviour());
        }

        ArrayList<Integer> timeout = new ArrayList<Integer>();
        for (int i = outer.getTimers().length/2 + 1; i < outer.getTimers().length - 1; i++) {
            if (System.currentTimeMillis() - outer.getTimers(i) > (long)osAgent.minute*2) {
                timeout.add(i);
            }
        }
        outer.SendConfig(timeout);








        // if (HBRequest != null) {
        //     JSONObject jsonContent = new JSONObject(HBRequest.getContent());
        //     outer.setDownstream(new Configuration(jsonContent.getJSONObject("configuration").toString()));

        //     outer.getDownstreamMsi().clear();
        //     JSONArray jsonArray = jsonContent.getJSONArray("msi");
        //     Iterator<Object> iteratorContent = jsonArray.iterator();
        //     while (iteratorContent.hasNext()) {
        //         JSONObject jsonMSI = (JSONObject)iteratorContent.next();
        //         outer.getDownstreamMsi().add(new MSI(jsonMSI.getInt("symbol")));
        //     }
        //     outer.addBehaviour(new BrainBehaviour(outer));

        //     ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
        //     HBResponse.setOntology("HB");
        //     HBResponse.addReceiver(HBRequest.getSender());

        //     jsonContent = new JSONObject();
        //     jsonContent.put("msi", outer.getMsi());
        //     HBResponse.setContent(jsonContent.toString());

        //     myAgent.send(HBResponse);
        //     outer.resetTimeDownstream();
        // } else {
        //     if (System.currentTimeMillis()-outer.getTimeDownstream() > (long)osAgent.minute*2) {
        //         outer.SendConfig();
        //     }
        // }
    }
}