package behaviour;

import java.util.Iterator;

import agents.osAgent;
import config.DownstreamNeighbour;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

public class DumpReceiverBehaviour extends AchieveREResponder {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public DumpReceiverBehaviour(Agent a, MessageTemplate mt) {
        super(a, mt);
        this.outer = (osAgent) a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        // find neighbour for which neighbour we received a dump message
        System.out.println("dump request received at " + outer.getLocalName());
        Iterator<DownstreamNeighbour> downstreamNeighbourIterator = outer.getDownstreamNeighbours().iterator();
        while (downstreamNeighbourIterator.hasNext()) {
            DownstreamNeighbour nextDownstreamNeighbour = downstreamNeighbourIterator.next();
            try {
                if (nextDownstreamNeighbour.getConfig().getAID().equals(request.getSender())) {
                    nextDownstreamNeighbour.removeTimeout();
                    System.out.println("timeout removed at " + outer.getLocalName());
                    outer.getDownstreamNeighbours().remove(nextDownstreamNeighbour);
                    outer.getDownstreamNeighbours().add(new DownstreamNeighbour(outer));
                    outer.SendConfig();
                }
            } catch (NullPointerException e) {
                
            }
        }
        // execute the code in the waker behaviour and delete the bahviour

        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);
        return result;
    }

}