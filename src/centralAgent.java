import java.util.Date;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

public class centralAgent extends Agent {

    @Override
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        addBehaviour(new WakerBehaviour(this, 5000) {
            @Override
            protected void onWake() {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(getAID("agent1"));
                msg.setSender(getAID("agent0"));
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                msg.setReplyWith("req"+System.currentTimeMillis());

                JSONObject messageContent = new JSONObject();
                messageContent.put("lane", 1);
                messageContent.put("symbol", 3);

                msg.setContent(messageContent.toString());

                send(msg);
            }
        });
    }

    @Override
    protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Central terminating.");
    }
}