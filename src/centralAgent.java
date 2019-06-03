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

    // Ontology
    private static final String SIG = "SIGNALLING";

    // Symbols
    JSONObject crossSymbol = new JSONObject();
    JSONObject blankSymbol = new JSONObject();

    @Override
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        crossSymbol.put("symbol", Measure.X);
        blankSymbol.put("symbol", Measure.BLANK);

        // Add behaviour de close lanes
        addBehaviour(new CloseLane(this, 20000));
    }

    @Override
    protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Central terminating.");
    }

    public class CloseLane extends TickerBehaviour {

        AID[] receivers = new AID[3];

        long T;

        public CloseLane(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
            T = period;
            receivers[0] = getAID("agent1");
            receivers[1] = getAID("agent2");
            receivers[2] = getAID("agent3");
        }

        @Override
        protected void onTick() {
            // setup message to be sent
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            for (int i = 0; i < 3; i++) {
                msg.addReceiver(receivers[i]);
            }
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            msg.setOntology(SIG);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 2000));
            
            JSONObject messageContent = new JSONObject();
            JSONArray arrayContent = new JSONArray();
            arrayContent.put(0,blankSymbol);
            arrayContent.put(1,blankSymbol);
            arrayContent.put(2,crossSymbol);

            messageContent.put("measures", arrayContent);
            msg.setContent(messageContent.toString());

            myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
                protected void handleInform(ACLMessage inform) {
                    System.out.println("Agent " + inform.getSender().getName() + " put up " + arrayContent.toString());
                }

                // This function handles the "refuse" responses
                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
                }

                // This function handles the "failure" and "does not exist" responses
                protected void handleFailure(ACLMessage failure) {
                    if (failure.getSender().equals(myAgent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver does not exist
                        System.out.println("Responder does not exist");
                    } else {
                        System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                    }
                }

                // This function handles the "timeout" responses
                protected void handleAllResultNotifications(Vector notifications) {
                    if (notifications.size() < 1) {
                        System.out.println("Timeout expired");
                    }
                }
            });

            myAgent.addBehaviour(new WakerBehaviour(myAgent, 10000) {
                @Override
                public void onWake() {

                    arrayContent.put(0,blankSymbol);
                    arrayContent.put(1,blankSymbol);
                    arrayContent.put(2,blankSymbol);

                    messageContent.put("measures", arrayContent);
                    msg.setContent(messageContent.toString());

                    myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
                        protected void handleInform(ACLMessage inform) {
                            System.out.println("Agent " + inform.getSender().getName() + " put up " + arrayContent.toString());
                        }

                        // This function handles the "refuse" responses
                        protected void handleRefuse(ACLMessage refuse) {
                            System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
                        }

                        // This function handles the "failure" and "does not exist" responses
                        protected void handleFailure(ACLMessage failure) {
                            if (failure.getSender().equals(myAgent.getAMS())) {
                                // FAILURE notification from the JADE runtime: the receiver does not exist
                                System.out.println("Responder does not exist");
                            } else {
                                System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                            }
                        }

                        // This function handles the "timeout" responses
                        protected void handleAllResultNotifications(Vector notifications) {
                            if (notifications.size() < 1) {
                                System.out.println("Timeout expired");
                            }
                        }
                    });
                }
            });
        }
    }
}