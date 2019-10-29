package behaviour;

import agents.osAgent;
import config.DownstreamNeighbour;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import measure.MSI;

public class ResponderBehaviour extends AchieveREResponder {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private DownstreamNeighbour neighbour;

    public ResponderBehaviour(Agent a, MessageTemplate mt, DownstreamNeighbour neighbour) {
        super(a, mt);
        this.neighbour = neighbour;
    }
    
    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        osAgent outer = (osAgent)myAgent;

        String msgContent = request.getContent();
        double number = Double.parseDouble(msgContent);
        // Initialize new MSI array
        MSI[] newMsi = new MSI[outer.getLocal().getLanes()];
        for (int i = 0; i < newMsi.length; i++) {
            newMsi[i] = new MSI((int)number);
        }
        neighbour.setMsi(newMsi);

        neighbour.resetTimeout();

        myAgent.addBehaviour(new CompilerBehaviour());

        ACLMessage result = request.createReply();
        result.setPerformative(ACLMessage.INFORM);
        result.setContent("content");
        result.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
        return result;
    }
}