
/**
 * Configuration:
 * Based on a configuration file, a configuration agent configures all the nessecarry agents
 * 
 * Behaviours:
 * - Measure vehicle traffic
 *      every 60 seconds, a new average of the traffic state is given
 * - Display signalling
 *      based on traffic state and neighbouring signalling, the display can change
 * - Communicate with neighbouring os
 *      on change of neighbour they let the others know of the new change
 * - 
 */

import java.util.Date;
import java.util.Random;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.*;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.tools.sniffer.Message;

public class osAgent extends GuiAgent {

    // Number of lanes controlled by the OS
    private int lanes;

    // AIDs for the neighbours of the OS and the central
    private AID upstream;
    private AID downstream;
    private AID central;

    // MSIs for this OS
    private MSI matrix[];

    // Ontology
    private static final String CONG = "CONGESTION";
    private static final String SIG = "SIGNALLING";

    // GUI
    transient protected osGui myGui;

    // Behaviour
    private Behaviour Request;

    // Communication
    private int responseFlag = 0;

    // Message templates
    private MessageTemplate requestTemplate;
    private MessageTemplate responseTemplate;

    // Message
    private ACLMessage receivedRequest;
    private ACLMessage sentRequest;

    /**
     * This function sets up the agent by setting the number of lanes and neighbour
     * based on input arguments. Then declare the MSI's and add behaviours of that
     * agent.
     */
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! OS " + getAID().getName() + " is ready.");

        // Get arguments (number of lanes, upstream neighbour, downstream neighbour)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            lanes = Integer.parseInt((String) args[0]);
            upstream = getAID((String) args[1]);
            downstream = getAID((String) args[2]);

            // Setup MSIs
            matrix = new MSI[lanes];
            for (int i = 0; i < lanes; i++) {
                try {
                    matrix[i] = new MSI(this);
                } catch (Exception e) {
                    // TODO: handle exception
                    System.out.println("Exception in the creation of matrix");
                }
            }

            // Print message stating that the configuration was succefull
            System.out.println("OS " + getAID().getName() + " configured with " + lanes + " lanes, upstream agent "
                    + upstream.getLocalName() + " and downstream agent " + downstream.getLocalName());

            // Declare central agent
            central = getAID("central");

            // Set up the gui
            myGui = new osGui(this);
            myGui.setVisible(true);

            this.requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            FSMBehaviour fsm = new FSMBehaviour(this);
            fsm.registerFirstState(new waitMsg(), "wait for request");
            fsm.registerState(new sendRequest(), "send request");
            fsm.registerState(new waitResponse(), "wait for response");
            fsm.registerState(new respondAgree(), "agree request");
            fsm.registerState(new respondRefuse(), "refuse request");

            fsm.registerDefaultTransition("wait for request", "wait for request");
            fsm.registerTransition("wait for request", "send request", 1);
            fsm.registerTransition("wait for request", "refuse request", 2);
            fsm.registerTransition("wait for request", "agree request", 3);
            fsm.registerTransition("send request", "wait for response", 1);
            fsm.registerTransition("wait for response", "wait for response", 0);
            fsm.registerTransition("wait for response", "agree request", 1);
            fsm.registerTransition("wait for response", "refuse request", 2);
            fsm.registerTransition("agree request", "wait for request", 1);
            fsm.registerTransition("refuse request", "wait for request", 1);

            addBehaviour(fsm);

            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, 10000) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, 10000));
                }
            });

            addBehaviour(new updateGui());

        } else {
            // Make agent terminate immediately
            System.out.println("Wrong configuration for " + getAID().getName());
            doDelete();
        }
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("OS " + getAID().getName() + " terminating.");
    }

    public class waitMsg extends OneShotBehaviour {

        private int exitValue;

        @Override
        public void action() {
            exitValue = 0;
            receivedRequest = receive(requestTemplate);
            if (receivedRequest != null) {
                System.out.println("request received at "+ getLocalName() + " from " + receivedRequest.getSender().getLocalName());
                System.out.println("cue size at " + getLocalName() + " is " + getCurQueueSize());
                JSONObject messageContent = new JSONObject(receivedRequest.getContent());
                int lane = messageContent.getInt("lane");
                try {
                    matrix[lane].changeDesiredState(receivedRequest, messageContent, 1);
                    System.out.println("desired state changed at " + getLocalName());
                    if (matrix[lane].getState(1).getSymbol() == Measure.BLANK) {
                        exitValue = 3;
                    } else {
                        exitValue = 1;
                    }
                } catch (RefuseException e) {
                    exitValue = 2;
                }
            }
        }

        @Override
        public int onEnd() {
            return exitValue;
        }
    }

    public class sendRequest extends OneShotBehaviour {

        private int exitValue;

        @Override
        public void action() {
            exitValue = 0;
            System.out.println("sending request upstream at " + getLocalName() + " to " + upstream.getLocalName());
            sentRequest = new ACLMessage(ACLMessage.REQUEST);
            sentRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            sentRequest.addReceiver(upstream);
            sentRequest.setReplyWith("request"+System.currentTimeMillis());

            JSONObject messageContent = new JSONObject(receivedRequest.getContent());
            int lane = messageContent.getInt("lane");
            messageContent.put("symbol",matrix[lane].getState(1).getSymbol() + 1);

            sentRequest.setContent(messageContent.toString());

            send(sentRequest);

            responseTemplate = MessageTemplate.and(MessageTemplate.MatchInReplyTo(sentRequest.getReplyWith()),
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST));

            exitValue = 1;
        }

        @Override
        public int onEnd() {
            return exitValue;
        }
    }

    public class waitResponse extends OneShotBehaviour {

        private int exitValue;

        @Override
        public void action() {
            exitValue = 0;
            ACLMessage responseMsg = receive(responseTemplate);
            if (responseMsg != null) {
                System.out.println("received response at " + getLocalName() + " from " + responseMsg.getSender().getLocalName());
                if (responseMsg.getPerformative() == ACLMessage.AGREE) {
                    matrix[1].updateState();
                    exitValue = 1;
                } else {
                    exitValue = 2;
                }
            }
        }
        
        @Override
        public int onEnd() {
            return exitValue;
        }
    }

    public class respondAgree extends OneShotBehaviour {

        private int exitValue;

        @Override
        public void action() {
            exitValue = 0;
            System.out.println("sending AGREE at " + getLocalName() + " to " + receivedRequest.getSender().getLocalName());
            ACLMessage msg = new ACLMessage(ACLMessage.AGREE);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            msg.addReceiver(receivedRequest.getSender());
            msg.setInReplyTo(receivedRequest.getReplyWith());
            
            send(msg);

            exitValue = 1;
        }
        
        @Override
        public int onEnd() {
            return exitValue;
        }
    }

    public class respondRefuse extends OneShotBehaviour {

        private int exitValue;

        @Override
        public void action() {
            exitValue = 0;
            System.out.println("sending REFUSE at " + getLocalName() + " to " + receivedRequest.getSender().getLocalName());
            ACLMessage msg = new ACLMessage(ACLMessage.REFUSE);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            msg.addReceiver(receivedRequest.getSender());
            msg.setInReplyTo(receivedRequest.getReplyWith());
            
            send(msg);

            exitValue = 1;
        }
        
        @Override
        public int onEnd() {
            return exitValue;
        }
    }

    public class TrafficSensing extends TickerBehaviour {

        private Random rand;

        public TrafficSensing(Agent a, long period) {
            super(a, period);
            rand = new Random();
        }

        @Override
        protected void onTick() {
            // for (int i = 0; i < lanes; i++) {
                if (rand.nextInt(100) >= 80) {
                    System.out.println("Congestion detected!");
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    msg.addReceiver(myAgent.getAID());

                    JSONObject messageContent = new JSONObject();
                    messageContent.put("lane",1);
                    messageContent.put("symbol",Measure.NF_50);

                    msg.setContent(messageContent.toString());

                    send(msg);
                } else {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    msg.addReceiver(myAgent.getAID());

                    JSONObject messageContent = new JSONObject();
                    messageContent.put("lane",1);
                    messageContent.put("symbol",Measure.BLANK);

                    msg.setContent(messageContent.toString());

                    send(msg);
                }
            // }
        }
    }

    public class updateGui extends CyclicBehaviour {
        @Override
        public void action() {
            for (int i = 0; i < 3; i++) {
                myGui.update(matrix[i].getState().getSymbolString(),i);
            }
        }
    }

    @Override
    protected void onGuiEvent(GuiEvent ev) {

    }

    public int getNumLanes () {
        return lanes;
    }
}