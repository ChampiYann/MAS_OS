package agents;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

import behaviour.ConfigurationResponder;
import behaviour.EnvironmentInputBehaviour;
import behaviour.HBReaction;
import behaviour.HBResponder;
import behaviour.HBSender;
import behaviour.ReceiveCentralDisplay;
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
import measure.CentralMeasure;
import measure.MSI;

public class osAgent extends Agent {

    private static final long serialVersionUID = 1L;

    // Simulation timing
    public static long minute = 1000; // milliseconds 

    public static final long timeout = minute/3;

    // Number of lanes controlled by the OS
    private int lanes;

    // AIDs for the neighbours of the OS and the central
    private Configuration upstream;
    private Configuration local;
    private Configuration downstream;
    private AID central;

    // GUI
    // transient protected osGui myGui;

    // Measures
    // private ArrayList<Measure> measures = new ArrayList<Measure>();

    // Behaviours
    private Behaviour HBSenderBehaviour;

    // Flags
    private boolean congestion = false;

    // Ontology
    public static final String DISPLAY = "DISPLAY";

    // Topic
    private AID topicConfiguration;
    private AID topicCentral;

    // Retries
    private long timeUpstream = 0;
    private long timeDownstream = 0;

    // congestion file
    private BufferedReader congestionReader;

    // New variables
    ArrayList<MSI> downstreamMsi;
    ArrayList<MSI> upstreamMsi;
    ArrayList<MSI> msi;
    // ArrayList<Measure> centralMeasures;
    ArrayList<CentralMeasure> centralMeasures;

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
            String configuration = (String)args[1];

            setEnabledO2ACommunication(true,0);
            Behaviour o2aBehaviour = new EnvironmentInputBehaviour(this,minute/4);
            addBehaviour(o2aBehaviour);
            setO2AManager(o2aBehaviour);

            // Declare central agent
            central = getAID("central");

            // Create new upstrem configuration
            upstream = new Configuration(this);
            downstream = new Configuration(this);

            // Create new local configuration
            local = new Configuration(this);
            local.getAID = getAID();
            local.lanes = lanes;
            resetTimeUpstream();
            resetTimeDownstream();

            // Setup MSIs
            msi = new ArrayList<MSI>(local.lanes);
            for (int i = 0; i < local.lanes; i++) {
                msi.add(new MSI());
            }
            downstreamMsi = new ArrayList<MSI>();
            downstreamMsi.addAll(Arrays.asList(new MSI[] { new MSI(), new MSI(), new MSI() }));
            upstreamMsi = new ArrayList<MSI>();
            upstreamMsi.addAll(Arrays.asList(new MSI[] { new MSI(), new MSI(), new MSI() }));
            centralMeasures = new ArrayList<CentralMeasure>();

            // Set up the gui
            // myGui = new osGui(this);
            // myGui.setVisible(true);

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
            String kmDot = null;
            try {
                kmDot = kmMatcher.group().replaceAll(",", ".");
            } catch (Exception e) {
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

            // Set message queue size
            // setQueueSize(10);

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicCentral = topicHelper.createTopic("CENTRAL");
                topicHelper.register(topicCentral);

                MessageTemplate centralTemplate = MessageTemplate.and(requestTemplate,
                    MessageTemplate.MatchTopic(topicCentral));
                addBehaviour(new ReceiveCentralDisplay(this,centralTemplate));
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

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
                MessageTemplate.MatchTopic(topicConfiguration));
            
            addBehaviour(new ConfigurationResponder(this, ConfigTemplate));

            // Behaviour that periodically sends a heartbeat upstream
            HBSenderBehaviour = new HBSender(this, minute/6);
            addBehaviour(HBSenderBehaviour);

            // Behaviour that checks if a HB has been received back
            addBehaviour(new HBReaction(this, minute/12));

            // behaviour that responds to a HB
            addBehaviour(new HBResponder(this, minute/12));

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
        Iterator<MSI> msiIterator = msi.iterator();
        while (msiIterator.hasNext()) {
            msiIterator.next().setSymbol(MSI.BLANK);
        }
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("SYMBOLS");
        JSONArray matrixJson = new JSONArray();
        msiIterator = msi.iterator();
        while (msiIterator.hasNext()) {
            matrixJson.put(msiIterator.next().getSymbol());
        }
        newMsg.setContent(matrixJson.toString());
        newMsg.addReceiver(central);
        send(newMsg);
        // myGui.dispose();
    }

    public void sendCentralUpdate() {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("SYMBOLS");
        JSONArray matrixJson = new JSONArray();
        Iterator<MSI> msiIterator = msi.iterator();
        while (msiIterator.hasNext()) {
            matrixJson.put(msiIterator.next().getSymbol());
        }
        newMsg.setContent(matrixJson.toString());
        newMsg.addReceiver(central);
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    /**
     * Send measure to an agent for which we have a configuration
     * @param config configuration of the receiver
     * @param ont ontology of the message
     * @param content content of the message
     */
    public void sendMeasure (Configuration config, String ont, String content) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology(ont);
        newMsg.setContent(content);
        newMsg.addReceiver(config.getAID);
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    /**
     * Format and send local configuration
     */
    public void SendConfig () {
        ACLMessage configurationRequest = new ACLMessage(ACLMessage.REQUEST);
        configurationRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + minute*4));
        configurationRequest.addReceiver(topicConfiguration);
        configurationRequest.setContent(local.configToJSON().toString());
        this.addBehaviour(new AchieveREInitiator(this, configurationRequest));
        resetTimeDownstream();
    }

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
     * @return the downstreamMsi
     */
    public ArrayList<MSI> getDownstreamMsi() {
        return downstreamMsi;
    }

    /**
     * @param downstreamMsi the downstreamMsi to set
     */
    public void setDownstreamMsi(ArrayList<MSI> downstreamMsi) {
        this.downstreamMsi = downstreamMsi;
    }

    /**
     * @return the upstreamMsi
     */
    public ArrayList<MSI> getUpstreamMsi() {
        return upstreamMsi;
    }

    /**
     * @param upstreamMsi the upstreamMsi to set
     */
    public void setUpstreamMsi(ArrayList<MSI> upstreamMsi) {
        this.upstreamMsi = upstreamMsi;
    }

    /**
     * @return the msi
     */
    public ArrayList<MSI> getMsi() {
        return msi;
    }

    /**
     * @param msi the msi to set
     */
    public void setMsi(ArrayList<MSI> msi) {
        this.msi = msi;
    }

    /**
     * @return the congestion
     */
    public boolean getCongestion() {
        return congestion;
    }

    /**
     * @param congestion the congestion to set
     */
    public void setCongestion(boolean congestion) {
        this.congestion = congestion;
    }

    /**
     * @return the congestionReader
     */
    public BufferedReader getCongestionReader() {
        return congestionReader;
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
     * @return the centralMsi
     */
    public ArrayList<CentralMeasure> getCentralMeasures() {
        return centralMeasures;
    }

    /**
     * @param downstream the downstream to set
     */
    public void setDownstream(Configuration downstream) {
        this.downstream = downstream;
    }

    /**
     * @return the topicCentral
     */
    public AID getTopicCentral() {
        return topicCentral;
    }

    /**
     * @return the hBSenderBehaviour
     */
    public Behaviour getHBSenderBehaviour() {
        return HBSenderBehaviour;
    }
}