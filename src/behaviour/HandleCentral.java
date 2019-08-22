package behaviour;

import java.util.Iterator;

import org.json.JSONArray;
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

        if (request.getOntology().equals("ADD") && flag == false) {
            JSONArray osListJSON = msgContent.getJSONArray("osList");
            Iterator<Object> osListIterator = osListJSON.iterator();
            while (osListIterator.hasNext()) {
                Double nextOs = (Double)osListIterator.next();
                if (nextOs == outer.getLocal().location) {
                    outer.getCentralMeasures().add(new CentralMeasure(outer, msgContent.getLong("ID"), msgContent.getJSONArray("lanes"),0));
                }
                for (int i = 0; i < outer.getDownstream().size(); i++) {
                    if (nextOs == outer.getDownstream().get(i).location) {
                        outer.getCentralMeasures().add(new CentralMeasure(outer, msgContent.getLong("ID"), msgContent.getJSONArray("lanes"),i+1));
                    }
                }
                if (nextOs == outer.getUpstream().location) {
                    outer.getCentralMeasures().add(new CentralMeasure(outer, msgContent.getLong("ID"), msgContent.getJSONArray("lanes"),-1));
                }
            }
        } else {
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