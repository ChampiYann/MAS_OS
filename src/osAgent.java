
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.*;
import jade.core.messaging.TopicManagementHelper;
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
    private Configuration upstream;
    private Configuration local;
    private Configuration downstream;
    private AID central;

    // MSIs for this OS
    private MSI matrix[];

    // GUI
    transient protected osGui myGui;

    // Measures
    private Vector<Maatregel> measures = new Vector<Maatregel>();

    // Flags
    private boolean congestion = false;

    // Ontology
    private String CANCEL = "CANCEL";
    private String ADD = "ADD";

    // Topic
    private AID topicConfiguration;

    // Retries
    private long timeUpstream = 0;
    private long timeDownstream = 0;

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
            String configuration =(String)args[1];

            // Setup MSIs
            matrix = new MSI[lanes];
            for (int i = 0; i < lanes; i++) {
                try {
                    matrix[i] = new MSI(this,i);
                } catch (Exception e) {
                    // TODO: handle exception
                    System.out.println("Exception in the creation of matrix");
                }
            }

            // Declare central agent
            central = getAID("central");

            // Create new upstrem configuration
            upstream = new Configuration(this);

            downstream = new Configuration(this);

            // Create new local configuration
            local = new Configuration(this);
            local.getAID = getAID();
            local.lanes = lanes;
            timeUpstream = System.currentTimeMillis();
            timeDownstream = System.currentTimeMillis();

            // Set up the gui
            myGui = new osGui(this);
            myGui.setVisible(true);

            // Setup configuration based on BPS
            // Extract road ID
            Pattern roadPattern = Pattern.compile("\\w{2}\\d{3}");
            Matcher roadMatcher = roadPattern.matcher(configuration);
            roadMatcher.find();
            local.road = roadMatcher.group();
            // Extract km reading
            Pattern kmPattern = Pattern.compile("(?<=\\s)\\d{1,3}[,\\.]\\d");
            Matcher kmMatcher = kmPattern.matcher(configuration);
            kmMatcher.find();
            Pattern hmPattern = Pattern.compile("(?<=[+-])\\d{1,3}");
            Matcher hmMatcher = hmPattern.matcher(configuration);
            String kmDot;
            try {
                kmDot = kmMatcher.group().replaceAll(",", ".");
            } catch (Exception e) {
                kmDot = kmMatcher.group();
            }
            if (hmMatcher.find()) {
                local.location = Float.parseFloat(kmDot) + Float.parseFloat(hmMatcher.group())/1000;
            } else {
                local.location = Float.parseFloat(kmDot);
            }
            // Extract road side
            Pattern sidePattern = Pattern.compile("(?<=\\s)[LRlr]");
            Matcher sideMatcher = sidePattern.matcher(configuration);
            sideMatcher.find();
            local.side = sideMatcher.group();

            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                final AID topicCentral = topicHelper.createTopic("CENTRAL");
                topicHelper.register(topicCentral);
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            MessageTemplate AddTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology(ADD));
            MessageTemplate CancelTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology(CANCEL));

            addBehaviour(new AddResponder(this, AddTemplate));

            addBehaviour(new CancelResponder(this, CancelTemplate));

            // Configure broadcast for configuration
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicConfiguration = topicHelper.createTopic("CONFIGURATION");
                topicHelper.register(topicConfiguration);
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            // Configuration response
            MessageTemplate ConfigTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("CONFIGURATION"));
            
            addBehaviour(new ConfigurationResponder(this, ConfigTemplate));
            

            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, 10000) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, 10000));
                }
            });

            // Update the GUI
            addBehaviour(new updateGui(this, 200));

            // Behaviour that periodically sends a heartbeat upstream
            addBehaviour(new TickerBehaviour(this,1000){
                @Override
                protected void onTick() {
                    ACLMessage HBRequest = new ACLMessage(ACLMessage.REQUEST);
                    HBRequest.setOntology("HB");
                    HBRequest.addReceiver(upstream.getAID);

                    myAgent.send(HBRequest);
                }
            });

            // Behaviour that checks if a HB has been received back
            addBehaviour(new TickerBehaviour(this,200) {
                @Override
                protected void onTick() {
                    MessageTemplate HBTemplate = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology("HB"));
                    ACLMessage HBResponse = myAgent.receive(HBTemplate);
                    if (HBResponse == null) {
                        if (System.currentTimeMillis()-timeUpstream > (long)2000) {
                            System.out.println("Upstream down at " + local.getAID.getLocalName());
                            upstream = new Configuration();
                        }
                    } else {
                        timeUpstream = System.currentTimeMillis();
                    }
                }
            });

            // behaviour that responds to a HB
            addBehaviour(new TickerBehaviour(this, 200) {
                @Override
                protected void onTick() {
                    MessageTemplate HBTemplate = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchOntology("HB"));
                    ACLMessage HBRequest = myAgent.receive(HBTemplate);
                    if (HBRequest != null) {
                        ACLMessage HBResponse = new ACLMessage(ACLMessage.INFORM);
                        HBResponse.setOntology("HB");
                        HBResponse.addReceiver(HBRequest.getSender());
                        myAgent.send(HBResponse);
                        timeDownstream = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis()-timeDownstream > (long)2000) {
                            System.out.println("Downstream down at " + local.getAID.getLocalName());
                            SendConfig();
                            for (int i = 0; i < measures.size(); i++) {
                                try {
                                    if (getMaatregel(local.getAID) == i) {} else {
                                        sendMeasure (local, CANCEL, measures.get(i).toJSON().toString());
                                    }
                                } catch (NoMaatregel e) {
                                    sendMeasure (local, CANCEL, measures.get(i).toJSON().toString());
                                }
                            }
                        }
                    }
                }
            });

            // Print message stating that the configuration was succefull
            System.out.println("OS " + getAID().getLocalName() + " configured on road " + local.road + " at km " + local.location +
                " on side " + local.side + " with " + lanes + " lanes");

        } else {
            // Make agent terminate immediately
            System.out.println("Wrong configuration for " + getAID().getName());
            doDelete();
        }
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("OS " + getAID().getName() + " terminating.");
        myGui.dispose();
    }

    public class ConfigurationResponder extends AchieveREResponder {

        
        public ConfigurationResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            return null;
        }
        
        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            Configuration newConfig = new Configuration();

            newConfig.getConfigFromJSON(request.getContent());
            if (local.location - newConfig.location < local.location - upstream.location && local.location - newConfig.location > 0) {
                upstream.location = newConfig.location;
                upstream.road = newConfig.road;
                upstream.getAID = newConfig.getAID;
                upstream.side = newConfig.side;

                timeUpstream = System.currentTimeMillis();

                System.out.println("upstream neighbour for " + local.getAID.getLocalName() + " is " + upstream.getAID.getLocalName());

                for (int i = 0; i < measures.size(); i++) {
                    sendMeasure(upstream, ADD, measures.get(i).toJSON().toString());
                }

                ACLMessage result = request.createReply();
                result.setPerformative(ACLMessage.INFORM);
                result.setContent(local.configToJSON());
                return result;
            } else {
                throw new FailureException("sub-optimal");
            }
        }
    }

    public class CancelResponder extends AchieveREResponder {

        public CancelResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            return null;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage msg = request.createReply();

            JSONObject msgContent = new JSONObject(request.getContent());
            try {
                int mr = getMaatregel(msgContent.getLong("ID"));
                Maatregel mt = measures.get(mr);
                sendMeasure (upstream, CANCEL, mt.toJSON().toString());
                measures.remove(mr);
                msg.setPerformative(ACLMessage.INFORM);
                return msg;
            } catch (NoMaatregel e) {
                throw new FailureException("no-measure-found");
            }
        }
    }

    public class AddResponder extends AchieveREResponder {

        public AddResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            return null;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage msg = request.createReply();

            JSONObject msgContent = new JSONObject(request.getContent());
            int it = msgContent.getInt("iteration");
            if ((
                (msgContent.getFloat("end") < local.location &&
                local.location <= msgContent.getFloat("start")) ||
                (upstream.location < msgContent.getFloat("start") && 
                msgContent.getFloat("start") < local.location &&
                msgContent.getFloat("end") < local.location)
            ) && 
            msgContent.getString("road").equals(local.road)) {
                measures.add(new Maatregel(msgContent));
                sendMeasure (upstream, ADD, msgContent.toString());
            } else if (it - 1 != 0 && !request.getSender().equals(central) && !request.getSender().equals(getAID())) {
                msgContent.put("iteration", it - 1);
                measures.add(new Maatregel(msgContent));
                sendMeasure (upstream, ADD, msgContent.toString());
            }
            msg.setPerformative(ACLMessage.INFORM);
            return msg;
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
            if (rand.nextInt(100) >= 80) {
                if (congestion == false) {
                    congestion = true;
                    System.out.println("Congestion detected!");

                    // Create measure
                    Maatregel mt = new Maatregel(Maatregel.AIDet,local);
                    sendMeasure (local, ADD, mt.toJSON().toString());
                } else {
                    congestion = false;
                    System.out.println("Congestion cleared!");

                    // Cancel measure
                    try {
                        int mr = getMaatregel(Maatregel.AIDet,local.getAID);
                        Maatregel mt = measures.get(mr);
                        sendMeasure (local, CANCEL, mt.toJSON().toString());;
                    } catch (NoMaatregel e) {
                        //TODO: handle exception
                    }
                }
            } else {}
        }
    }

    public class updateGui extends TickerBehaviour {

        public updateGui(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onTick() {
            for (int i = 0; i < lanes; i++) {
                matrix[i].updateState();
                myGui.update(matrix[i].getState().getSymbolString(),i);
            }
        }
    }

    @Override
    protected void onGuiEvent (GuiEvent ev) {

    }

    public void sendMeasure (Configuration config, String ont, String content) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology(ont);
        newMsg.setContent(content);
        newMsg.addReceiver(config.getAID);
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    public void SendConfig () {
        ACLMessage configurationRequest = new ACLMessage(ACLMessage.REQUEST);
        configurationRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        configurationRequest.setOntology("CONFIGURATION");
        configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
        configurationRequest.addReceiver(topicConfiguration);
        configurationRequest.setContent(local.configToJSON());
        this.addBehaviour(new AchieveREInitiator(this, configurationRequest) {
            @Override
            protected void handleInform(ACLMessage inform) {
                String messageContent = inform.getContent();
                downstream.getConfigFromJSON(messageContent);
                System.out.println("downstream neighbour for " + local.getAID.getLocalName() + " is " + downstream.getAID.getLocalName());
            }
        });
        timeDownstream = System.currentTimeMillis();
    }   

    public int getMaatregel (int t, AID o) throws NoMaatregel {

        for (int i = 0; i < measures.size(); i++) {
            Maatregel mr = measures.get(i);
            if (mr.getType() == t && mr.getOrigin().equals(o)) {
                return i;
            }
        }
        throw new NoMaatregel();
    }

    public int getMaatregel (AID o) throws NoMaatregel {

        for (int i = 0; i < measures.size(); i++) {
            Maatregel mr = measures.get(i);
            if (mr.getOrigin().equals(o)) {
                return i;
            }
        }
        throw new NoMaatregel();
    }

    public int getMaatregel (long id) throws NoMaatregel {

        for (int i = 0; i < measures.size(); i++) {
            Maatregel mr = measures.get(i);
            if (mr.getID() == id) {
                return i;
            }
        }
        throw new NoMaatregel();
    }

    public int getNumLanes () {
        return lanes;
    }

    public Vector<Maatregel> getMeasures() {
        return measures;
    }
}