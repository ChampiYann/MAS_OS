package agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

// import behaviour.CompilerBehaviour;
import behaviour.ConfigurationResponder;
import behaviour.HBReaction;
import behaviour.HBResponder;
import behaviour.HBSender;
import behaviour.HandleMessage;
import behaviour.TrafficSensing;
import config.Configuration;
import config.DownstreamNeighbour;
import config.Neighbour;
import config.UpstreamNeighbour;
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
    public static final long minute = 10000; // milliseconds

    public static final long timeout = minute/3;

    public static final long sendPeriod = minute/6;

    // AIDs for the neighbours of the OS and the central
    private ArrayList<Configuration> config;
    // private Configuration[] upstream;
    private Configuration local;
    // private Configuration[] downstream;
    private AID central;

    // Number of neighbours
    public static final int nsize = 1;

    private ArrayList<UpstreamNeighbour> upstreamNeighbours;
    private ArrayList<DownstreamNeighbour> downstreamNeighbours;

    // Measures
    private ArrayList<Measure>[] measures;
    private ArrayList<Measure> localMeasures;
    // private ArrayList<Measure> upstreamMeasures;
    // private ArrayList<Measure> downstreamMeasures;
    private ArrayList<Measure> centralMeasures;

    // Flags
    private boolean congestion;

    // Ontology
    public static final String MEASURE = "MEASURE";

    // Topic
    private AID topicConfiguration;
    // private AID topicMeasure;
    private AID topicCentral;

    // Retries
    private long[] timers;
    private long timeUpstream = 0;
    private long timeDownstream = 0;

    // New variables
    private MSI[] msi;

    // Behaviours
    private Behaviour HBSenderBehaviour;

    /**
     * This function sets up the agent by setting the number of lanes and neighbour
     * based on input arguments. Then declare the MSI's and add behaviours of that
     * agent.
     */
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! OS " + getAID().getName() + " is ready at " + System.currentTimeMillis() + ".");

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

            // Create empty list of neighbours
            // Upstream
            this.upstreamNeighbours = new ArrayList<UpstreamNeighbour>(nsize);
            for (int i = 0; i < nsize; i++) {
                this.upstreamNeighbours.add(new UpstreamNeighbour(this));
            }
            // Downstream
            this.downstreamNeighbours = new ArrayList<DownstreamNeighbour>(nsize);
            for (int i = 0; i < nsize; i++) {
                this.downstreamNeighbours.add(new DownstreamNeighbour(this));
            }

            // // Create empty config array
            // this.config = new ArrayList<Configuration>(nsize);
            // for (int i = 0; i < nsize; i++) {
            //     this.config.add(i, new Configuration(this));
            //     // this.config.add(this.local.getLanes()*3-i, new Configuration(this));
            //     if (i > nsize/2) {
            //         this.config.get(i).setLocation(Double.POSITIVE_INFINITY);
            //     }
            // }
            // this.config.set(nsize/2,this.local);

            // Declare central agent
            this.central = getAID("central");

            // Create empty set of central measures
            this.centralMeasures = new ArrayList<Measure>();
            // // Create empty set of arraylist of measures
            // this.measures = (ArrayList<Measure>[]) new ArrayList[nsize];
            // Arrays.setAll(this.measures, n -> {return new ArrayList<Measure>();});
            // Create empty set of local measures
            this.localMeasures = new ArrayList<Measure>();

            // Setup MSIs
            this.msi = new MSI[local.getLanes()];
            for (int i = 0; i < msi.length; i++) {
                this.msi[i] = new MSI();
            }

            // Start with no congestion
            this.congestion = false;

            // // Reset heartbeat timers
            // this.timers = new long[nsize];
            // Arrays.setAll(this.timers, n -> {return 0;});
            // resetTime();

            // // Behaviour that periodically sends a heartbeat upstream
            // HBSenderBehaviour = new HBSender(this, minute/6);
            // addBehaviour(HBSenderBehaviour);

            // // behaviour that responds to a HB
            // addBehaviour(new HBResponder(this, minute/12));

            // // Behaviour that checks if a HB has been received back
            // addBehaviour(new HBReaction(this, minute/12));

            // General template for a request
            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            // Traffic sensing behaviour with data from launch class
            setEnabledO2ACommunication(true,0);
            Behaviour o2aBehaviour = new TrafficSensing(this, minute/4);
            addBehaviour(o2aBehaviour);
            setO2AManager(o2aBehaviour);

            // Configure reception of list of measures
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicCentral = topicHelper.createTopic("CENTRAL");
                topicHelper.register(topicCentral);

                MessageTemplate MeasureTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchTopic(topicCentral));

                addBehaviour(new HandleMessage(this,MeasureTemplate));
            } catch (ServiceException e) {
                System.out.println("Wrong configuration for " + getAID().getName());
                doDelete();
            }

            // Configure broadcast for configuration
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
                topicConfiguration = topicHelper.createTopic("CONFIGURATION");
                topicHelper.register(topicConfiguration);

                // Configuration response
                MessageTemplate ConfigTemplate = MessageTemplate.and(requestTemplate,
                    MessageTemplate.MatchTopic(topicConfiguration));
                
                addBehaviour(new ConfigurationResponder(this, ConfigTemplate));
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
        // System.out.println("OS " + getAID().getName() + " terminating.");
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
        System.out.println("OS " + getAID().getName() + " terminating at " + System.currentTimeMillis() + ".");
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
        newMsg.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    /**
     * send message
     */
    public void sendMeasure(Configuration config, String ont, String content) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology(ont);
        newMsg.setContent(content);
        newMsg.addReceiver(config.getAID());
        newMsg.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
        this.addBehaviour(new AchieveREInitiator(this, newMsg));
    }

    /**
     * Format and send local configuration
     */
    public void SendConfig () {
            ACLMessage configurationRequest = new ACLMessage(ACLMessage.REQUEST);
            configurationRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            // configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + minute*4));
            configurationRequest.addReceiver(topicConfiguration);
            JSONObject jsonContent = new JSONObject();
            jsonContent.put("configuration", local.configToJSON());
            // jsonContent.put("measures", new JSONArray(outer.getLocalMeasures().stream().filter(n -> n.getType() != Measure.REACTION).collect(Collectors.toList())));
            // HBResponse.setContent(jsonContent.toString());
            configurationRequest.setContent(jsonContent.toString());
            configurationRequest.setOntology("CONFIGURATION");
            configurationRequest.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
            this.addBehaviour(new AchieveREInitiator(this, configurationRequest));
    }

    /**
     * @return the local
     */
    public Configuration getLocal() {
        return local;
    }

    // /**
    //  * @return the downstream
    //  */
    // public Configuration[] getDownstream() {
    //     return downstream;
    // }

    // /**
    //  * @return the upstream
    //  */
    // public Configuration[] getUpstream() {
    //     return upstream;
    // }

    // /**
    //  * @param upstream the upstream to set
    //  */
    // public void setUpstream(Configuration[] upstream) {
    //     this.upstream = upstream;
    // }

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

    public void resetTime() {
        Arrays.setAll(this.timers,n -> {return System.currentTimeMillis();});
    }

    public void resetTime(int index) {
        this.timers[index] = System.currentTimeMillis();
    }

    /**
     * @return the timers
     */
    public long[] getTimers() {
        return timers;
    }

    /**
     * 
     * @param index index of timer to request
     * @return the timer
     */
    public long getTimers(int index) {
        return timers[index];
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

    // /**
    //  * @return the upstreamMeasures
    //  */
    // public ArrayList<Measure> getUpstreamMeasures() {
    //     return upstreamMeasures;
    // }

    // /**
    //  * @return the downstreamMeasures
    //  */
    // public ArrayList<Measure> getDownstreamMeasures() {
    //     return downstreamMeasures;
    // }

    /**
     * @param congestion the congestion to set
     */
    public void setCongestion(boolean congestion) {
        this.congestion = congestion;
    }

    // /**
    //  * @param downstream the downstream to set
    //  */
    // public void setDownstream(Configuration[] downstream) {
    //     this.downstream = downstream;
    // }

    /**
     * @return the central
     */
    public AID getCentral() {
        return central;
    }

    /**
     * @return the measures
     */
    public ArrayList<Measure>[] getMeasures() {
        return measures;
    }

    /**
     * @return the config
     */
    public ArrayList<Configuration> getConfig() {
        return config;
    }

    /**
     * @return the topicMeasure
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

    /**
     * @return the upstreamNeighbours
     */
    public ArrayList<UpstreamNeighbour> getUpstreamNeighbours() {
        return upstreamNeighbours;
    }

    /**
     * @return the downstreamNeighbours
     */
    public ArrayList<DownstreamNeighbour> getDownstreamNeighbours() {
        return downstreamNeighbours;
    }
}