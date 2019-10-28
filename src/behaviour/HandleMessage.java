package behaviour;

import org.json.JSONObject;

import agents.osAgent;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import measure.Measure;

public class HandleMessage extends AchieveREResponder {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HandleMessage(Agent a, MessageTemplate mt) {
        super(a, mt);
		this.outer = (osAgent)a;
    }
    
    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);

        // JSONArray jsonContent = new JSONArray(request.getContent());
        // if (request.getSender().equals(outer.getCentral())) {
        //     Iterator<Object> jsonContentIterator = jsonContent.iterator();
        //     outer.getCentralMeasures().clear();
        //     while (jsonContentIterator.hasNext()) {
        //         outer.getCentralMeasures().add(new Measure((JSONObject)jsonContentIterator.next()));
        //     }
        //     myAgent.addBehaviour(new CompilerBehaviour(myAgent));
        //     return result;
        // } else {
        //     return null;
        // }

        String msgContent = request.getContent();
        JSONObject jsonContent = new JSONObject(msgContent);
        long ID = jsonContent.getLong("ID");
        if (request.getOntology().equals("ADD") & outer.getCentralMeasures().stream().noneMatch(n -> n.getID() == ID)) {
            outer.getCentralMeasures().add(new Measure(jsonContent));
        } else if (request.getOntology().equals("CANCEL")) {
            outer.getCentralMeasures().removeIf(n -> (n.getID() == ID));
        }
        myAgent.addBehaviour(new CompilerBehaviour((osAgent)myAgent));
        return result;
    }
}