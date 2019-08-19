package behaviour;

import java.util.Iterator;
import java.util.Vector;

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
import measure.MSI;

public class ReceiveCentralDisplay extends AchieveREResponder {

    private static final long serialVersionUID = 1L;
    private osAgent outer;

    public ReceiveCentralDisplay(Agent a, MessageTemplate mt) {
        super(a, mt);
        outer = (osAgent)a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);

        String msgContent = request.getContent();
        JSONObject jsonContent = new JSONObject(msgContent);
        long ID = jsonContent.getLong("ID");
        if (request.getOntology().equals("ADD")) {
            JSONArray jsonVector = jsonContent.getJSONArray("msi");
            float start = jsonContent.getFloat("start");
            float end = jsonContent.getFloat("end");
            Iterator<Object> iteratorContent = jsonVector.iterator();
            Vector<MSI> tempVector = new Vector<MSI>();
            while (iteratorContent.hasNext()) {
                int value = (Integer)iteratorContent.next();
                tempVector.add(new MSI(value));
            }
            outer.getCentralMeasures().add(new CentralMeasure(outer, tempVector, start, end, ID));
        } else {
            outer.getCentralMeasures().removeIf(n -> (n.getID() == ID));
        }
        myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
        return result;
    }
}