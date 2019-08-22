package behaviour;

import java.util.Iterator;

import org.json.JSONObject;

import agents.osAgent;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import measure.CentralMeasure;

public class HandleCentral extends AchieveREResponder {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HandleCentral(Agent a, MessageTemplate mt) {
        super(a, mt);
		this.outer = (osAgent)a;
    }
    
    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        JSONObject msgContent = new JSONObject(request.getContent());
        Iterator<CentralMeasure> centralMeasureIterator = outer.getCentralMeasures().iterator();
        boolean flag = false;
        while (centralMeasureIterator.hasNext()) {
            if (centralMeasureIterator.next().getID() == msgContent.getLong("ID")) {
                flag = true;
            }
        }

        if (request.getOntology().equals("ADD")) {
            double start = msgContent.getDouble("start");
            double end = msgContent.getDouble("end");
            if ((end <= outer.getLocal().location && outer.getLocal().location <= start) ||
            (outer.getLocal().location < start && outer.getDownstream().firstElement().location > end) ||
            (outer.getLocal().location > end && outer.getUpstream().location < start)) {
                outer.getCentralMeasures().add(new CentralMeasure(outer, msgContent.getLong("ID"), msgContent.getJSONArray("lanes"),0,start,end));
            } else if (start >= outer.getUpstream().location && outer.getLocal().location > start) {
                outer.getCentralMeasures().add(new CentralMeasure(outer, msgContent.getLong("ID"), msgContent.getJSONArray("lanes"),-1,start,end));
            } else {
                for (int i = 0; i < outer.getDownstream().size(); i++) {
                    if (end <= outer.getDownstream().get(i).location && outer.getLocal().location < end) {
                        outer.getCentralMeasures().add(new CentralMeasure(outer, msgContent.getLong("ID"), msgContent.getJSONArray("lanes"),i+1,start,end));
                    }
                }
            }
           
        } else if (request.getOntology().equals("CANCEL")) {
            try {
                Iterator<CentralMeasure> measureIterator = outer.getCentralMeasures().iterator();
                while (measureIterator.hasNext()) {
                    CentralMeasure nextMeasure = measureIterator.next();
                    if (nextMeasure.getID() == msgContent.getLong("ID")) {
                        measureIterator.remove();
                    }
                }
            } catch (Exception e) {
                //TODO: handle exception
            }
        }
        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);
        return result;
    }
}