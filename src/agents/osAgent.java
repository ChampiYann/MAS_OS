package agents;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import behaviour.CompilerBehaviour;
import behaviour.ConfigurationResponder;
import behaviour.HBReaction;
import behaviour.HBResponder;
import behaviour.HBSender;
import behaviour.HandleMessage;
import behaviour.HandleMsi;
import behaviour.TrafficSensing;
import config.Configuration;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.Behaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import measure.MSI;
import measure.Measure;

public class osAgent extends Agent {

    private static final long serialVersionUID = 1L;

    // Simulation timing
    public static long minute = 600; // milliseconds

    // AIDs for the neighbours of the OS and the central
    private Configuration upstream;
    private Configuration local;
    private Configuration downstream;
    private AID central;

    // Measures
    private ArrayList<Measure> localMeasures;
    private ArrayList<Measure> upstreamMeasures;
    private ArrayList<Measure> downstreamMeasures;
    private ArrayList<Measure> centralMeasures;

    // Flags
    private boolean congestion;

    // Ontology
    public static final String MEASURE = "MEASURE";

    // Topic
    private AID topicConfiguration;
    private AID topicMeasure;
    private AID topicCentral;

    // Retries
    private long timeUpstream = 0;
    private long timeDownstream = 0;

    // New variables
    private MSI[] msi;

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
            int lanes = Integer.parseInt((String) args[0]);
            String configuration = (String)args[1];

            // Create new local configuration
            this.local = new Configuration(this);
            this.local.setAID(getAID());
            this.local.setLanes(lanes);

            // Setup configuration based on BPS
            // Extract road ID
            Pattern roadPattern = Pattern.compile("\\w{2}\\d{3}");
            Matcher roadMatcher = roadPattern.matcher(configuration);
            roadMatcher.find();
            this.local.setRoad(roadMatcher.group());
            // Extract km reading
            Pattern kmPattern = Pattern.compile("(?<=\\s)\\d{1,3}[,\\.]\\d");
            Matcher kmMatcher = kmPattern.matcher(configuration);
            kmMatcher.find();
            Pattern hmPattern = Pattern.compile("(?<=[+-])\\d{1,3}");
            Matcher hmMatcher = hmPattern.matcher(configuration);
            String kmDot = null;
            try {
                kmDot = kmMatcher.group().replaceAll(",", ".");
            } catch (Exception e) {
            }
            if (hmMatcher.find()) {
                this.local.setLocation(Double.parseDouble(kmDot) + Double.parseDouble(hmMatcher.group())/1000);
            } else {
                this.local.setLocation(Double.parseDouble(kmDot));
            }
            // Extract road side
            Pattern sidePattern = Pattern.compile("(?<=\\s)[LRlr]");
            Matcher sideMatcher = sidePattern.matcher(configuration);
            sideMatcher.find();
            this.local.setSide(sideMatcher.group());

            // Create new upstream configuration
            this.upstream = new Configuration(this);
            // Create new downstream configuration
            this.downstream = new Configuration(this);
            this.downstream.setLocation(Double.POSITIVE_INFINITY);

            // Declare central agent
            this.central = getAID("central");

            // Create empty set of local measures
            this.localMeasures = new ArrayList<Measure>();
            // Create empty set of upstream measures
            this.upstreamMeasures = new ArrayList<Measure>();
            // Create empty set of downstream measures
            this.downstreamMeasures = new ArrayList<Measure>();
            // Create empty set of central measures
            this.centralMeasures = new ArrayList<Measure>();

            // Setup MSIs
            this.msi = new MSI[local.getLanes()];
            for (int i = 0; i < msi.length; i++) {
                this.msi[i] = new MSI();
            }

            // Start with no congestion
            this.congestion = false;

            // Reset heartbeat timers
            resetTimeUpstream();
            resetTimeDownstream();

            // Behaviour that periodically sends a heartbeat upstream
            addBehaviour(new HBSender(this, minute/2));

            // behaviour that responds to a HB
            addBehaviour(new HBResponder(this, minute/4));

            // Behaviour that checks if a HB has been received back
            addBehaviour(new HBReaction(this, minute/4));

            // General template for a request
            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            // Configure broadcast for configuration
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                this.topicConfiguration = topicHelper.createTopic("CONFIGURATION");
                topicHelper.register(this.topicConfiguration);

                MessageTemplate ConfigTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchTopic(this.topicConfiguration));
            
                addBehaviour(new ConfigurationResponder(this, ConfigTemplate));
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            // Traffic sensing behaviour with data from launch class
            setEnabledO2ACommunication(true,0);
            Behaviour o2aBehaviour = new TrafficSensing(this, minute/4);
            addBehaviour(o2aBehaviour);
            setO2AManager(o2aBehaviour);

            // Configure reception of list of measures
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicMeasure = topicHelper.createTopic("MEASURE");
                topicHelper.register(topicMeasure);

                MessageTemplate MeasureTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology(MEASURE));

                addBehaviour(new HandleMessage(this,MeasureTemplate));
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            // Change displays
            addBehaviour(new CompilerBehaviour(this,minute/2));

            // Subscribe to central messages
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicCentral = topicHelper.createTopic("CENTRAL");
                topicHelper.register(topicCentral);
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            // Print message stating that the configuration was succefull
            System.out.println("OS " + getAID().getLocalName() + " configured on road " + local.getRoad() + " at km " + local.getLocation() +
                " on side " + local.getSide() + " with " + local.getLanes() + " lanes");

        } else {
            // Make agent terminate immediately
            System.out.println("Wrong configuration for " + getAID().getName());
            doDelete();
        }
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("OS " + getAID().getName() + " terminating.");
        for (int i = 0; i < this.msi.length; i++) {
            this.msi[i].setSymbol(MSI.BLANK);
        }
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("SYMBOLS");
        JSONArray matrixJson = new JSONArray();
        for (int i = 0; i < this.msi.length; i++) {
            matrixJson.put(this.msi[i].getSymbol());
        }
        newMsg.setContent(matrixJson.toString());
        newMsg.addReceiver(central);
        send(newMsg);
    }

    /**
     * Format and send local configuration
     */
    public void SendConfig () {
        ACLMessage configurationRequest = new ACLMessage(ACLMessage.REQUEST);
        configurationRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + minute*4));
        configurationRequest.addReceiver(this.topicConfiguration);
        JSONObject msgContent = this.local.configToJSON();
        configurationRequest.setContent(msgContent.toString());
        this.addBehaviour(new AchieveREInitiator(this, configurationRequest));
        resetTimeDownstream();
        resetTimeUpstream();
    }

    /**
     * Send current displaying symbols to central
     */
    public void sendCentralUpdate() {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("SYMBOLS");
        JSONArray matrixJson = new JSONArray();
        for (int i = 0; i < this.msi.length; i++) {
            matrixJson.put(this.msi[i].getSymbol());
        }
        newMsg.setContent(matrixJson.toString());
        newMsg.addReceiver(central);
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    // /**
    //  * Broadcast measure
    //  * @param content content of the message
    //  */
    // public void sendString (String content) {
    //     ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
    //     newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
    //     newMsg.setOntology(MEASURE);
    //     newMsg.setContent(content);
    //     newMsg.addReceiver(this.downstream.getAID());
    //     newMsg.addReceiver(this.upstream.getAID());
    //     this.addBehaviour(new AchieveREInitiator(this, newMsg));
    // }

    /**
     * send local measures to neighbours
     */
    public void sendMeasures() {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology(MEASURE);
        newMsg.addReceiver(this.downstream.getAID());
        newMsg.addReceiver(this.upstream.getAID());
        newMsg.setContent(new JSONArray(this.localMeasures).toString());
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    // public void sendCentralMeasure (String content) {
    //     ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
    //     newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
    //     newMsg.setOntology("ADD");
    //     newMsg.setContent(content);
    //     newMsg.addReceiver(topicCentral);
    //     this.addBehaviour(new AchieveREInitiator(this, newMsg));
    // }

    

    /**
     * @return the local
     */
    public Configuration getLocal() {
        return local;
    }

    /**
     * @return the downstream
     */
    public Configuration getDownstream() {
        return downstream;
    }

    /**
     * @return the upstream
     */
    public Configuration getUpstream() {
        return upstream;
    }

    /**
     * @param upstream the upstream to set
     */
    public void setUpstream(Configuration upstream) {
        this.upstream = upstream;
    }

    /**
     * @return the msi
     */
    public MSI[] getMsi() {
        return msi;
    }

    /**
     * @param msi the msi to set
     */
    public void setMsi(MSI[] msi) {
        this.msi = msi;
    }

    /**
     * @return the congestion
     */
    public boolean getCongestion() {
        return congestion;
    }

    /**
     * @return the timeUpstream
     */
    public long getTimeUpstream() {
        return timeUpstream;
    }

    /**
     * Reset the timeUpstream
     */
    public void resetTimeUpstream() {
        this.timeUpstream = System.currentTimeMillis();
    }

    /**
     * @return the timeDownstream
     */
    public long getTimeDownstream() {
        return timeDownstream;
    }

    /**
     * Reset the timeDownstream
     */
    public void resetTimeDownstream() {
        this.timeDownstream = System.currentTimeMillis();
    }

    /**
     * @return the centralMeasures
     */
    public ArrayList<Measure> getCentralMeasures() {
        return centralMeasures;
    }

    /**
     * @return the localMeasures
     */
    public ArrayList<Measure> getLocalMeasures() {
        return localMeasures;
    }

    /**
     * @param localMeasures the localMeasures to set
     */
    public void setLocalMeasures(ArrayList<Measure> localMeasures) {
        this.localMeasures = localMeasures;
    }

    /**
     * @return the upstreamMeasures
     */
    public ArrayList<Measure> getUpstreamMeasures() {
        return upstreamMeasures;
    }

    /**
     * @return the downstreamMeasures
     */
    public ArrayList<Measure> getDownstreamMeasures() {
        return downstreamMeasures;
    }

    /**
     * @param congestion the congestion to set
     */
    public void setCongestion(boolean congestion) {
        this.congestion = congestion;
    }

    /**
     * @param downstream the downstream to set
     */
    public void setDownstream(Configuration downstream) {
        this.downstream = downstream;
    }

    /**
     * @return the central
     */
    public AID getCentral() {
        return central;
    }
}