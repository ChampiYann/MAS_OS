package config;

import agents.osAgent;
import behaviour.PeriodicSenderBehaviour;
import behaviour.TimeoutBehaviour;

public class UpstreamNeighbour extends Neighbour {

    // Behaviour
    private PeriodicSenderBehaviour periodicSenderBehaviour;
    private TimeoutBehaviour timeoutBehaviour;

    public UpstreamNeighbour(osAgent agent) {
        super(agent,Double.NEGATIVE_INFINITY);
        this.periodicSenderBehaviour = new PeriodicSenderBehaviour(agent,osAgent.sendPeriod,this);
        this.timeoutBehaviour = new TimeoutBehaviour(agent, osAgent.timeout, this);
    }

    public UpstreamNeighbour(osAgent agent, Configuration config) {
        super(agent, config);
        this.periodicSenderBehaviour = new PeriodicSenderBehaviour(agent,osAgent.sendPeriod,this);
        this.timeoutBehaviour = new TimeoutBehaviour(agent, osAgent.timeout, this);
    }

    public void addBehaviour() {
        this.outer.addBehaviour(this.periodicSenderBehaviour);
        this.outer.addBehaviour(this.timeoutBehaviour);
    }

    @Override
    public void resetTimeout() {
        this.timeoutBehaviour.reset();
    }

    public void removeSender() {
        this.outer.removeBehaviour(this.periodicSenderBehaviour);
    }
    
}