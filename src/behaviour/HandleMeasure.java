package behaviour;

import java.util.Iterator;

import agents.osAgent;
import config.Configuration;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

public class HandleMeasure extends AchieveREResponder {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HandleMeasure(Agent a, MessageTemplate mt) {
        super(a, mt);
		this.outer = (osAgent)a;
    }
    
    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        boolean trigger = false;
        Iterator<Configuration> configIterator = outer.getDownstream().iterator();
        int i = 1;
        while (configIterator.hasNext()) {
            try {
                if (configIterator.next().getAID.equals(request.getSender())) {
                    outer.getCongestion().set(i, Boolean.parseBoolean(request.getContent()));
                    trigger = true;
                    break;
                }
            } catch (Exception e) {
                //TODO: handle exception
            }
            i++;
        }
        if (request.getSender().equals(outer.getUpstream().getAID)) {
            outer.getCongestion().set(3, Boolean.parseBoolean(request.getContent()));
            trigger = true;
        }
        if (trigger == true) {
            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            return reply;
        } else {
            return null;
        }
    }
}