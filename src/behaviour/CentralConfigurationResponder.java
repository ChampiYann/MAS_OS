package behaviour;

import java.util.Iterator;

import agents.centralAgent;
import config.Configuration;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

public class CentralConfigurationResponder extends AchieveREResponder {

    private static final long serialVersionUID = 1L;
    
    private centralAgent outer;

    public CentralConfigurationResponder(Agent a, MessageTemplate mt) {
        super(a, mt);
        outer = (centralAgent)a;
    }

    @Override
    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
        return null;
    }
    
    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
        Configuration newConfig = new Configuration();

        newConfig.getConfigFromJSON(request.getContent());
        Iterator<Configuration> iterator = outer.getOS().iterator();
        boolean exists = false;
        while (iterator.hasNext()) {
            Configuration config = iterator.next();
            if (config.getAID.equals(newConfig.getAID)) {
                exists = true;
            }
        }
        if (!exists) {
            outer.getOS().add(newConfig);
            outer.getMyGui().addPortal();
        }
        return null;
    }
}