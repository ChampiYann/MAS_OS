
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
                    matrix[i] = new MSI();
                } catch (Exception e) {
                    // TODO: handle exception
                    System.out.println("Exception in the creation of matrix");
                }
            }

            // Print message stating that the configuration was succefull
            System.out.println("OS " + getAID().getName() + " configured with " + lanes + " lanes and upstream agent "
                    + upstream.getLocalName());

            // Declare central agent
            central = getAID("central");

            // Set up the gui
            myGui = new osGui(this);
            myGui.setVisible(true);

            // Add query behaviour for downstream neighbour
            addBehaviour(new DownstreamCommunicationBehaviour(this, downstream, 4000));

            // Set message template to listen to when upstream query comes in
            MessageTemplate queryTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_QUERY),
                    MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));
            MessageTemplate upstreamTemplate = MessageTemplate.and(queryTemplate,
                    MessageTemplate.MatchSender(upstream));

            // Add listen behaviour to respond to upstream query
            addBehaviour(new UpstreamCommunicationBehaviour(this, upstreamTemplate));

            // Set message template for requests from central and sensors
            MessageTemplate requestTemplate = MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            // Add listen behaviour to repond to requests from central and sensors
            addBehaviour(new RequestBehaviour(this, requestTemplate));

            // Add cyclic behaviour that changes displayed measure
            addBehaviour(new UpdateMSI());

            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, 2000) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, 4000));
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

    /**
     * This class extends the ticker behaviour to query the MSIs from its downstream
     * neighbour
     */
    public class DownstreamCommunicationBehaviour extends TickerBehaviour {

        // This will be the downstream neighbour
        private AID receiver;

        private long T;

        // Constructor for this class
        public DownstreamCommunicationBehaviour(Agent a, AID downstream, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
            receiver = downstream;
            T = period;
        }

        // This action is executed on every tick of the ticker
        @Override
        protected void onTick() {
            // setup message to be sent
            ACLMessage msg = new ACLMessage(ACLMessage.QUERY_REF);
            msg.addReceiver(receiver);
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
            msg.setOntology(SIG);
            // We want to receive a reply in 10 secs
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 2 * T));

            // Add behaviour to send messages using the FIPA protocols
            myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
                // This function handles the response from the query
                protected void handleInform(ACLMessage inform) {
                    try {
                        // Parse content to a JSON object
                        JSONObject messageContent = new JSONObject(inform.getContent());
                        // Get the array names "symbols"
                        JSONArray messageSymbols = messageContent.getJSONArray("symbols");
                        // For every lane we read the asociated symbol downstream
                        for (int i = 0; i < lanes; i++) {
                            JSONObject content = new JSONObject();
                            if (messageSymbols.getInt(i) == Measure.ARROW_L) {
                                content.put("symbol", Measure.BLANK);
                            } else {
                                content.put("symbol", messageSymbols.getInt(i) + 1);
                            }
                            // Change the next state for the downstream desired state
                            try {
                                matrix[i].changeDesiredState(inform, content, 3);
                            } catch (JSONException e) {
                                //TODO: handle exception
                            }
                        }
                    } catch (JSONException e) {
                        System.out.println("Error while parsing JSON.");
                    }
                }

                // This function handles the "refuse" responses
                protected void handleRefuse(ACLMessage refuse) {
                    System.out.println(
                            "Agent " + refuse.getSender().getName() + " refused to perform the requested action");
                }

                // This function handles the "failure" and "does not exist" responses
                protected void handleFailure(ACLMessage failure) {
                    if (failure.getSender().equals(myAgent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver does not exist
                        System.out.println("Responder does not exist");
                    } else {
                        System.out.println(
                                "Agent " + failure.getSender().getName() + " failed to perform the requested action");
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
    }

    public class UpstreamCommunicationBehaviour extends AchieveREResponder {

        public UpstreamCommunicationBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);

            if (request.getSender().equals(upstream)) {
                if (request.getOntology().equals(SIG)) {
                    return agree;
                } else {
                    // We refuse to perform the action
                    System.out.println("Agent " + getLocalName() + ": Not understood");
                    throw new NotUnderstoodException("check-failed");
                }
            } else {
                throw new RefuseException("query-not-allowed");
            }
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
                throws FailureException {
            try {
                JSONArray json_msi = new JSONArray();
                for (int i = 0; i < lanes; i++) {
                    json_msi.put(matrix[i].getState().getSymbol());
                }
                JSONObject json_message = new JSONObject().put("symbols", json_msi);

                ACLMessage inform = request.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setContent(json_message.toString());
                return inform;
            } catch (Exception e) {
                System.out.println("Agent " + getLocalName() + ": Action failed");
                throw new FailureException("unexpected-error");
            }
        }
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

            if (request.getSender().equals(myAgent.getAID())) {
                if (request.getOntology().equals(CONG)) {
                    return agree;
                } else {
                    throw new NotUnderstoodException("request-not-understood");
                }
            } else if (request.getSender().equals(central)) {
                if (request.getOntology().equals(SIG)) {
                    return agree;
                } else {
                    throw new NotUnderstoodException("request-not-understood");
                }
            } else {
                throw new RefuseException("request-not-allowed");
            }
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
                throws FailureException {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);

            try {
                JSONObject messageContent = new JSONObject(request.getContent());
                if (request.getOntology().equals(CONG)) {
                    JSONArray messageCongestion = messageContent.getJSONArray(CONG);

                    for (int i = 0; i < messageCongestion.length(); i++) {
                        if (messageCongestion.getBoolean(i) == true) {
                            JSONObject content = new JSONObject();
                            content.put("symbol", Measure.NF_50);
                            matrix[i].changeDesiredState(request, content, 2);
                        } else {
                            JSONObject content = new JSONObject();
                            content.put("symbol", Measure.BLANK);
                            matrix[i].changeDesiredState(request, content, 2);
                        }
                    }
                } else {
                    JSONArray messageMeasures = messageContent.getJSONArray("measures");
                    for (int i = 0; i < messageMeasures.length(); i++) {
                        matrix[i].changeDesiredState(request, messageMeasures.getJSONObject(i), 1);
                    }
                }
                return inform;
            } catch (JSONException e) {
                // TODO: handle exception
                throw new FailureException("json-parsing-failure");
            }
        }
    }

    /**
     * This class has no end and keeps executing until destruction of the agent. It
     * cyclicly calls the updateState method for matrix
     */
    public class UpdateMSI extends CyclicBehaviour {

        // Only one function is defined for the action to be repeated
        @Override
        public void action() {
            for (int i = 0; i < lanes; i++) {
                matrix[i].updateState();
                myGui.update(matrix[i].getState().getSymbolString(),i);
            }
        }

    }

    public class TrafficSensing extends TickerBehaviour {

        private Random rand;
        private long T;

        public TrafficSensing(Agent a, long period) {
            super(a, period);
            rand = new Random();
            T = period;
        }

        @Override
        protected void onTick() {
            // setup message to be sent
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(myAgent.getAID());
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            msg.setOntology(CONG);
            // We want to receive a reply in 2 time the period
            msg.setReplyByDate(new Date(System.currentTimeMillis() + 2 * T));

            JSONObject messageContent = new JSONObject();
            JSONArray congestion = new JSONArray();

            for (int i = 0; i < lanes; i++) {
                if (rand.nextInt(100) >= 90) {
                    System.out.println("Congestion detected!");
                    congestion.put(true);
                } else {
                    congestion.put(false);
                }
            }
            messageContent.put(CONG, congestion);
            msg.setContent(messageContent.toString());

            // Add behaviour to send messages using the FIPA protocols
            myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
                // This function handles the response from the request
                protected void handleInform(ACLMessage inform) {
                    // System.out.println("Agent " + inform.getSender().getName() + " performed the request");
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
                        System.out.println("Congestion notification timeout expired");
                    }
                }
            });
        }

    }

    public class MSI {
        /**
         * This class represents the displays above the road that show the measures
         * applied. They are initialized as BLANK
         */

        private Measure currentState;
        private Measure centralState;
        private Measure selfState;
        private Measure downstreamState;

        private static final int CENTRAL = 1;
        private static final int SELF = 2;
        private static final int DOWNSTREAM = 3;

        public MSI() {
            try {
                currentState = new Measure();
                centralState = new Measure();
                selfState = new Measure();
                downstreamState = new Measure();
            } catch (Exception e) {
                System.out.println("Exception in the creation of MSI");
            }
        }

        public void changeDesiredState(ACLMessage msg, JSONObject content, int select) throws JSONException {
            long time = msg.getPostTimeStamp();
            AID sender = msg.getSender();
            int symbol = content.getInt("symbol");
            if (symbol >= -1 && symbol < 7) {

            } else {
                symbol = Measure.BLANK;
            }
                switch (select) {
                case CENTRAL:
                    centralState.update(symbol, sender, time);
                    break;
                case SELF:
                    selfState.update(symbol, sender, time);
                    break;
                case DOWNSTREAM:
                    downstreamState.update(symbol, sender, time);
                    break;
                default:
                    break;
                }
        }

        public void updateState() {
            if (centralState.isBlank()) {
                if (downstreamState.getSymbol() != Measure.ARROW_L && downstreamState.getSymbol() != Measure.ARROW_R) {
                    if (selfState.isBlank()) {
                        currentState = downstreamState;
                    } else {
                        currentState = selfState;
                    }
                } else {
                    currentState = downstreamState;
                }
            } else {
                currentState = centralState;
            }
        }

        public Measure getState() {
            return currentState;
        }

        public Measure getState(int select) {
            switch (select) {
            case CENTRAL:
                return centralState;
            case SELF:
                return selfState;
            case DOWNSTREAM:
                return downstreamState;
            default:
                return null;
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