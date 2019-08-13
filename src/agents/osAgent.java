package agents;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;

import behaviour.ConfigurationResponder;
import behaviour.HBReaction;
import behaviour.HBResponder;
import behaviour.HBSender;
import behaviour.HandleMeasure;
import behaviour.HandleMsi;
import behaviour.TrafficSensing;
import config.Configuration;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.WakerBehaviour;
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
    public static long minute = 400; // milliseconds 

    // Number of lanes controlled by the OS
    private int lanes;

    // AIDs for the neighbours of the OS and the central
    private Configuration upstream;
    private Configuration local;
    private Vector<Configuration> downstream;
    private AID central;

    // GUI
    // transient protected osGui myGui;

    // Measures
    // private Vector<Measure> measures = new Vector<Measure>();

    // Flags
    private Vector<Boolean> congestion;

    // Ontology
    public static final String MEASURE = "MEASURE";

    // Topic
    private AID topicConfiguration;
    private AID topicMeasure;

    // Retries
    private long timeUpstream = 0;
    private long timeDownstream = 0;

    // congestion file
    private BufferedReader congestionReader;

    // New variables
    Vector<MSI> msi;
    Vector<Measure> centralMeasures;

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

            try {
                congestionReader = new BufferedReader(new FileReader("config\\" + configuration + ".txt"));
            } catch (IOException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            // Declare central agent
            central = getAID("central");

            // Create new upstrem configuration
            upstream = new Configuration(this);
            upstream.location = Double.NEGATIVE_INFINITY;
            downstream = new Vector<Configuration>(2);
            for (int i = 0; i < downstream.capacity(); i++) {
                downstream.add(new Configuration(this));
                downstream.lastElement().location = Double.POSITIVE_INFINITY;
            }
            // Create new local configuration
            local = new Configuration(this);
            local.getAID = getAID();
            local.lanes = lanes;

            resetTimeUpstream();
            resetTimeDownstream();

            // Setup MSIs
            msi = new Vector<MSI>(local.lanes);
            for (int i = 0; i < msi.capacity(); i++) {
                msi.add(new MSI());
            }
            centralMeasures = new Vector<Measure>();
            congestion = new Vector<>(4);
            congestion.add(false);
            congestion.add(false);
            congestion.add(false);
            congestion.add(false);

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

            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                final AID topicCentral = topicHelper.createTopic("CENTRAL");
                topicHelper.register(topicCentral);
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicMeasure = topicHelper.createTopic("CONFIGURATION");
                topicHelper.register(topicMeasure);
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            MessageTemplate MeasureTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology(MEASURE));

            addBehaviour(new HandleMeasure(this,MeasureTemplate));

            addBehaviour(new HandleMsi(this,minute/4));

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
            
            Date wakeupDate = new Date((long) args[2]);
            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, wakeupDate) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, minute, getWakeupTime()));
                }
            });

            // Behaviour that periodically sends a heartbeat upstream
            addBehaviour(new HBSender(this, minute/2));

            // Behaviour that checks if a HB has been received back
            addBehaviour(new HBReaction(this, minute/4));

            // behaviour that responds to a HB
            addBehaviour(new HBResponder(this, minute/4));

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
     * Broadcast measure
     * @param content content of the message
     */
    public void sendMeasure (String content) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology(MEASURE);
        newMsg.setContent(content);
        newMsg.addReceiver(topicMeasure);
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    /**
     * Format and send local configuration
     */
    public void SendConfig () {
        ACLMessage configurationRequest = new ACLMessage(ACLMessage.REQUEST);
        configurationRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        configurationRequest.setOntology("CONFIGURATION");
        configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + minute*4));
        configurationRequest.addReceiver(topicConfiguration);
        configurationRequest.setContent(local.configToJSON());
        this.addBehaviour(new AchieveREInitiator(this, configurationRequest));
        // {
        //     @Override
        //     protected void handleInform(ACLMessage inform) {
        //         String messageContent = inform.getContent();
        //         downstream.getConfigFromJSON(messageContent);
        //         downstreamMsi = new Vector<MSI>(downstream.lanes);
        //         for (int i = 0; i < downstreamMsi.capacity(); i++) {
        //             downstreamMsi.add(new MSI());
        //         }
        //         System.out.println("downstream neighbour for " + local.getAID.getLocalName() + " is " + downstream.getAID.getLocalName());
        //     }
        // });
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
    public Vector<Configuration> getDownstream() {
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

    // /**
    //  * @return the downstreamMsi
    //  */
    // public Vector<MSI> getDownstreamMsi() {
    //     return downstreamMsi;
    // }

    // /**
    //  * @param downstreamMsi the downstreamMsi to set
    //  */
    // public void setDownstreamMsi(Vector<MSI> downstreamMsi) {
    //     this.downstreamMsi = downstreamMsi;
    // }

    // /**
    //  * @return the upstreamMsi
    //  */
    // public Vector<MSI> getUpstreamMsi() {
    //     return upstreamMsi;
    // }

    // /**
    //  * @param upstreamMsi the upstreamMsi to set
    //  */
    // public void setUpstreamMsi(Vector<MSI> upstreamMsi) {
    //     this.upstreamMsi = upstreamMsi;
    // }

    /**
     * @return the msi
     */
    public Vector<MSI> getMsi() {
        return msi;
    }

    /**
     * @param msi the msi to set
     */
    public void setMsi(Vector<MSI> msi) {
        this.msi = msi;
    }

    /**
     * @return the congestion
     */
    public Vector<Boolean> getCongestion() {
        return congestion;
    }

    /**
     * @param congestion the congestion to set
     */
    // public void setCongestion(boolean congestion) {
    //     this.congestion = congestion;
    // }

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
}