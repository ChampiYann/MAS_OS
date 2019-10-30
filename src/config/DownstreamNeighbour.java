package config;

import agents.osAgent;
// import behaviour.ResponderBehaviour;
import behaviour.TimeoutBehaviour2;
// import jade.lang.acl.ACLMessage;
// import jade.lang.acl.MessageTemplate;

public class DownstreamNeighbour extends Neighbour {

    // Behaviour
    // private ResponderBehaviour responderBehaviour;
    private TimeoutBehaviour2 timeoutBehaviour;
    // private MessageTemplate template;

    public DownstreamNeighbour(osAgent agent) {
        super(agent, Double.POSITIVE_INFINITY);

        this.timeoutBehaviour = new TimeoutBehaviour2(agent, osAgent.timeout, this);
        this.outer.addBehaviour(this.timeoutBehaviour);
    }
    
    public DownstreamNeighbour(osAgent agent, Configuration config) {
        super(agent, config);

        this.timeoutBehaviour = new TimeoutBehaviour2(agent, osAgent.timeout, this);
    }

    @Override
    public void resetTimeout() {
        this.timeoutBehaviour.reset(osAgent.timeout);
    }
    
    // public void removeReceiver() {
    //     this.outer.removeBehaviour(this.responderBehaviour);
    // }
}