package behaviour;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import agents.osAgent;
import config.UpstreamNeighbour;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import measure.Measure;

public class PeriodicSenderBehaviour extends TickerBehaviour {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // Neighbour
    private UpstreamNeighbour neighbour;

    public PeriodicSenderBehaviour(Agent a, long period, UpstreamNeighbour neighbour) {
        super(a, period);
        this.neighbour = neighbour;
    }

    @Override
    protected void onTick() {
        osAgent outer = (osAgent)myAgent;
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("HB");
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("configuration", outer.getLocal().configToJSON());
        jsonContent.put("measures", new JSONArray(outer.getLocalMeasures().stream().filter(n -> n.getType() != Measure.REACTION).collect(Collectors.toList())));
        newMsg.setContent(jsonContent.toString());
        // try {
        //     outer.getBwWriter().write(newMsg.getContent() + "\n");
        //     outer.getBwWriter().flush();
        // } catch (IOException e) {
        //     //TODO: handle exception
        // }
        newMsg.addReceiver(this.neighbour.getConfig().getAID());
        newMsg.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
        newMsg.setReplyByDate(new Date(System.currentTimeMillis()+osAgent.timeout));

        // System.out.println(outer.getUpstreamNeighbours().get(0).getConfig().getLocation() + " "
        //     + outer.getUpstreamNeighbours().get(1).getConfig().getLocation() + " "
        //     + outer.getUpstreamNeighbours().get(2).getConfig().getLocation() + " "
        //     + outer.getLocal().getLocation() + " "
        //     + outer.getDownstreamNeighbours().get(0).getConfig().getLocation() + " "
        //     + outer.getDownstreamNeighbours().get(1).getConfig().getLocation() + " "
        //     + outer.getDownstreamNeighbours().get(2).getConfig().getLocation() + " ");

        myAgent.addBehaviour(new SenderBehaviour(myAgent, newMsg, neighbour));
    }
    
}