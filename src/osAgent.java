
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
import org.omg.CORBA.Request;

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
    // transient protected osGui myGui;

    // Behaviour
    private Behaviour Request;

    // Communication
    private int responseFlag = 0;

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
            // myGui = new osGui(this);
            // myGui.setVisible(true);

            // Add query behaviour for downstream neighbour
            // addBehaviour(new DownstreamCommunicationBehaviour(this, downstream, 4000));

            // Set message template to listen to when upstream query comes in
            MessageTemplate queryTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY),
                    MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));
            MessageTemplate upstreamTemplate = MessageTemplate.and(queryTemplate,
                    MessageTemplate.MatchSender(upstream));

            // Add listen behaviour to respond to upstream query
            // addBehaviour(new UpstreamCommunicationBehaviour(this, upstreamTemplate));

            // Set message template for requests from central and sensors
            MessageTemplate requestTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST_WHEN),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST_WHEN));

            // Add listen behaviour to repond to requests from central and sensors
            addBehaviour(new RequestBehaviour(this, requestTemplate));

            // Add cyclic behaviour that changes displayed measure
            // addBehaviour(new UpdateMSI());

            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, 1000) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, 2000));
                }
            });

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

    public class RequestBehaviour extends AchieveREResponder {

        public RequestBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            System.out.println("received message from "+request.getSender().getLocalName());

            if (request.getSender().equals(myAgent.getAID())) {
                JSONObject messageContent = new JSONObject(request.getContent());
                int lane = messageContent.getInt("lane");
                try {
                    matrix[lane].changeDesiredState(request, messageContent, 1);   
                } catch (RefuseException e) {
                    throw new RefuseException(e.getMessage());
                }
                // check if we can display somrthing upstream
                myAgent.addBehaviour(new UpstreamRequestBehaviour(lane, Measure.F_50));
                while (responseFlag == 0) {}
                switch(responseFlag) {
                    case 1:
                        responseFlag = 0;
                        return agree;
                    case 2:
                        responseFlag = 0;
                        throw new RefuseException("message");
                    default:
                        responseFlag = 0;
                        throw new NotUnderstoodException("unknown-error");
                }
            } else if (request.getSender().equals(downstream)) {
                JSONObject messageContent = new JSONObject(request.getContent());
                int lane = messageContent.getInt("lane");
                int symbol = messageContent.getInt("symbol");
                // check if we can apply the desired thing on this lane
                try {
                    matrix[lane].changeDesiredState(request, messageContent, 1);   
                } catch (RefuseException e) {
                    throw new RefuseException(e.getMessage());
                }
                // check if we can display something upstream if needed
                if (symbol < Measure.BLANK) {
                    myAgent.addBehaviour(new UpstreamRequestBehaviour(lane, symbol+1));
                    while (responseFlag == 0) {}
                    switch(responseFlag) {
                        case 1:
                            responseFlag = 0;
                            return agree;
                        case 2:
                            responseFlag = 0;
                            throw new RefuseException("message");
                        default:
                            responseFlag = 0;
                            throw new NotUnderstoodException("unknown-error");
                    }   
                } else {
                    return agree;
                }
            } else {
                throw new RefuseException("request-not-allowed");
            }
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);

            try {
                matrix[1].updateState();
                // myGui.update(matrix[1].getState().getSymbolString(),1);
                return inform;
            } catch (JSONException e) {
                // TODO: handle exception
                throw new FailureException("json-parsing-failure");
            }
        }
    }

    public class TrafficSensing extends TickerBehaviour {

        private Random rand;
        private long T;
        private SelfRequestBehaviour congestion;

        public TrafficSensing(Agent a, long period) {
            super(a, period);
            rand = new Random();
            T = period;
        }

        @Override
        protected void onTick() {
            // for (int i = 0; i < lanes; i++) {
                if (rand.nextInt(100) >= 50) {
                    System.out.println("Congestion detected!");
                    congestion = new SelfRequestBehaviour(1,Measure.NF_50);
                    myAgent.addBehaviour(congestion);
                } else {
                    
                }
            // }
        }
    }

    /**
     * UpstreamRequestBehaviour(symbol, lane)
     * UpstreamRequestBehaviour(message content)
     */

    public class UpstreamRequestBehaviour extends SimpleBehaviour {

        ACLMessage msg;

        public UpstreamRequestBehaviour (int lane, int symbol) {
            msg = new ACLMessage(ACLMessage.REQUEST_WHEN);
            msg.addReceiver(upstream);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST_WHEN);
            msg.setOntology(SIG);
            // We want to receive a reply in 2 time the period
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 1000));

            JSONObject messageContent = new JSONObject();
            messageContent.put("lane", lane);
            messageContent.put("symbol", symbol);

            msg.setContent(messageContent.toString());

            System.out.println("sending message to "+upstream.getLocalName());
        }

        @Override
        public void action() {
            myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
                @Override
                protected void handleAgree(ACLMessage agree) {
                    responseFlag = 1;
                }

                @Override
                protected void handleInform(ACLMessage inform) {
                    super.handleInform(inform);
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    responseFlag = 2;
                }

                @Override
                protected void handleFailure(ACLMessage failure) {
                    super.handleFailure(failure);
                }

                @Override
                protected void handleAllResultNotifications(Vector resultNotifications) {
                    if (resultNotifications.size() < 1) {
                        System.out.println("Timeout expired");
                        responseFlag = 2;
                    }
                }
            });
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    public class SelfRequestBehaviour extends SimpleBehaviour {

        ACLMessage msg;
        AID receiver;

        public SelfRequestBehaviour (int lane, int symbol) {
            msg = new ACLMessage(ACLMessage.REQUEST_WHEN);
            receiver = getAID();
            msg.addReceiver(receiver);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST_WHEN);
            msg.setOntology(SIG);
            // We want to receive a reply in 2 time the period
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 1000));

            JSONObject messageContent = new JSONObject();
            messageContent.put("lane", lane);
            messageContent.put("symbol", symbol);

            msg.setContent(messageContent.toString());

            System.out.println("sending message to self: "+getLocalName());
        }

        @Override
        public void action() {
            myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
                @Override
                protected void handleAgree(ACLMessage agree) {
                    responseFlag = 1;
                }

                @Override
                protected void handleInform(ACLMessage inform) {
                    super.handleInform(inform);
                }

                @Override
                protected void handleRefuse(ACLMessage refuse) {
                    responseFlag = 2;
                }

                @Override
                protected void handleFailure(ACLMessage failure) {
                    super.handleFailure(failure);
                }

                @Override
                protected void handleAllResultNotifications(Vector resultNotifications) {
                    if (resultNotifications.size() < 1) {
                        System.out.println("Timeout expired");
                        responseFlag = 2;
                    }
                }
            });
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    @Override
    protected void onGuiEvent(GuiEvent ev) {

    }

    public int getNumLanes () {
        return lanes;
    }
}