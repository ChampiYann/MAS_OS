package behaviour;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import config.UpstreamNeighbour;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import measure.MSI;
import measure.Measure;

public class SenderBehaviour extends AchieveREInitiator {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // Neighbour
    private UpstreamNeighbour neighbour;

    public SenderBehaviour(Agent a, ACLMessage msg, UpstreamNeighbour neighbour) {
        super(a, msg);
        this.neighbour = neighbour;
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        osAgent outer = (osAgent)myAgent;
        
        JSONObject jsonContent = new JSONObject(inform.getContent());
        // // Get measures
        // JSONArray jsonMeasures = jsonContent.getJSONArray("measures");
        // Iterator<Object> jsonMeasuresIterator = jsonMeasures.iterator();
        // ArrayList<Measure> newMeasures = new ArrayList<Measure>();
        // while (jsonMeasuresIterator.hasNext()) {
        //     newMeasures.add(new Measure((JSONObject)jsonMeasuresIterator.next()));
        // }
        // Get MSI
        JSONArray jsonMsi = jsonContent.getJSONArray("msi");
        Iterator<Object> jsonMsiIterator = jsonMsi.iterator();
        ArrayList<MSI> newMsiArrayList = new ArrayList<MSI>();
        while (jsonMsiIterator.hasNext()) {
            newMsiArrayList.add(new MSI((JSONObject)jsonMsiIterator.next()));
        }

        // this.neighbour.setMeasures(newMeasures);
        this.neighbour.setMsi(newMsiArrayList.toArray(new MSI[outer.getLocal().getLanes()]));

        // reset timeout
        neighbour.resetTimeout();
        // calculate new MSI
        myAgent.addBehaviour(new CompilerBehaviour());
    }
}