package behaviour;

import org.json.JSONArray;

import agents.centralAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

public class SymbolListener extends AchieveREResponder {

    private static final long serialVersionUID = 1L;
    
    private centralAgent outer;

    public SymbolListener(Agent a, MessageTemplate mt) {
        super(a, mt);
        this.outer = (centralAgent)a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        ACLMessage msg = request.createReply();

        AID sender = request.getSender();
        JSONArray matrixJson = new JSONArray(request.getContent());
        int[] symbols = new int[3];
        for (int i = 0; i < 3; i++) {
            symbols[i] = matrixJson.getInt(i);
        }
        outer.getMyGui().update(sender, symbols);

        msg.setPerformative(ACLMessage.INFORM);
        return msg;
    }
}