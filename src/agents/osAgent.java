package agents;

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import behaviour.BrainBehaviour;
import config.Configuration;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import measure.AIDMeasure;
import measure.MSI;
import measure.Measure;
import measure.NoMeasure;

public class osAgent extends Agent {

    // Simulation timing
    public static long minute = 400; // milliseconds 

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
    private Vector<Measure> measures = new Vector<Measure>();

    // Flags
    private boolean congestion = false;

    // Ontology
    public static final String DISPLAY = "DISPLAY";

    // Topic
    private AID topicConfiguration;

    // Retries
    private long timeUpstream = 0;
    private long timeDownstream = 0;

    // congestion file
    private BufferedReader congestionReader;

    // New variables
    Vector<MSI> downstreamMsi;
    Vector<MSI> upstreamMsi;
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

            downstream = new Configuration(this);

            // Create new local configuration
            local = new Configuration(this);
            local.getAID = getAID();
            local.lanes = lanes;
            timeUpstream = System.currentTimeMillis();
            timeDownstream = System.currentTimeMillis();

            // Setup MSIs
            msi = new Vector<MSI>(local.lanes);
            for (int i = 0; i < msi.capacity(); i++) {
                msi.add(new MSI());
            }
            msi.addAll(Collections.nCopies(local.lanes,new MSI()));
            centralMeasures = new Vector<Measure>();

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

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            MessageTemplate DisplayTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology(DISPLAY));

            addBehaviour(new ReceiveSensorData(this,DisplayTemplate));

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

            // Update the GUI
            // addBehaviour(new UpdateMSI(this, minute/4));

            // Behaviour that periodically sends a heartbeat upstream
            addBehaviour(new TickerBehaviour(this,minute/2){
                @Override
                protected void onTick() {
                    ACLMessage HBRequest = new ACLMessage(ACLMessage.REQUEST);
                    HBRequest.setOntology("HB");
                    HBRequest.addReceiver(upstream.getAID);
                    HBRequest.setContent(local.configToJSON());

                    myAgent.send(HBRequest);
                }
            });

            // Behaviour that checks if a HB has been received back
            addBehaviour(new TickerBehaviour(this,minute/4) {
                @Override
                protected void onTick() {
                    MessageTemplate HBTemplate = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchOntology("HB"));
                    ACLMessage HBResponse = myAgent.receive(HBTemplate);
                    if (HBResponse == null) {
                        if (System.currentTimeMillis()-timeUpstream > (long)minute*2) {
                            // System.out.println("Upstream down at " + local.getAID.getLocalName());
                            upstream = new Configuration();
                        }
                    } else {
                        timeUpstream = System.currentTimeMillis();
                    }
                }
            });

            // behaviour that responds to a HB
            addBehaviour(new TickerBehaviour(this, minute/4) {
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
                        downstream.getConfigFromJSON(HBRequest.getContent());
                        myAgent.send(HBResponse);
                        timeDownstream = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis()-timeDownstream > (long)minute*2) {
                            // System.out.println("Downstream down at " + local.getAID.getLocalName());
                            SendConfig();
                            // for (int i = 0; i < measures.size(); i++) {
                            //     try {
                            //         if (getMeasure(local.getAID) == i) {} else {
                            //             sendMeasure (local, CANCEL, measures.get(i).toJSON().toString());
                            //         }
                            //     } catch (NoMeasure e) {
                            //         sendMeasure (local, CANCEL, measures.get(i).toJSON().toString());
                            //     }
                            // }
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
        // myGui.dispose();
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
                upstream.lanes = newConfig.lanes;

                upstreamMsi = new Vector<MSI>(upstream.lanes);
                for (int i = 0; i < upstreamMsi.capacity(); i++) {
                    upstreamMsi.add(new MSI());
                }

                timeUpstream = System.currentTimeMillis();

                System.out.println("upstream neighbour for " + local.getAID.getLocalName() + " is " + upstream.getAID.getLocalName());

                ACLMessage result = request.createReply();
                result.setPerformative(ACLMessage.INFORM);
                result.setContent(local.configToJSON());
                return result;
            } else {
                // throw new FailureException("sub-optimal");
                return null;
            }
        }
    }

    public class TrafficSensing extends TickerBehaviour {

        private long T;
        private long simLastTime;

        public TrafficSensing(Agent a, long period, long wakeUpTime) {
            super(a, period);
            T = period;
            simLastTime = wakeUpTime;
            // time = LocalTime.of(1, 0, 0);
        }

        @Override
        protected void onTick() {
            long simCurrentTime = System.currentTimeMillis();
            long elapsedTime = simCurrentTime - simLastTime;
            long steps = elapsedTime/T;
            simLastTime += steps*T;
            while (steps > 0) {
                boolean newCongestion = false;
                String newLine = null;
                try {
                    newLine = congestionReader.readLine();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                String[] values = newLine.split(",");
                String[] elements = values[1].split(" ");
                for (int i = 0; i < elements.length; i++) {
                    if (Integer.parseInt(elements[i]) == 1) {
                        newCongestion = true;
                    }
                }
                if (newCongestion == true && congestion == false) {
                    congestion = true;
                    myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
                    System.out.println("Congestion detected!");

                } else if (newCongestion == false && congestion == true) {
                    congestion = false;
                    myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
                    System.out.println("Congestion cleared!");

                }
                steps -= 1;
            }
        }
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
        configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + minute*4));
        configurationRequest.addReceiver(topicConfiguration);
        configurationRequest.setContent(local.configToJSON());
        this.addBehaviour(new AchieveREInitiator(this, configurationRequest) {
            @Override
            protected void handleInform(ACLMessage inform) {
                String messageContent = inform.getContent();
                downstream.getConfigFromJSON(messageContent);
                downstreamMsi = new Vector<MSI>(downstream.lanes);
                for (int i = 0; i < downstreamMsi.capacity(); i++) {
                    downstreamMsi.add(new MSI());
                }
                System.out.println("downstream neighbour for " + local.getAID.getLocalName() + " is " + downstream.getAID.getLocalName());

                myAgent.removeBehaviour(this);
            }
        });
        timeDownstream = System.currentTimeMillis();
    }

    public class ReceiveSensorData extends AchieveREResponder {

        public ReceiveSensorData(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            return null;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);

            String msgContent = request.getContent();
            AID sender = request.getSender();
            JSONArray jsonContent = new JSONArray(msgContent);
            Iterator<Object> iteratorContent = jsonContent.iterator();
            Vector<MSI> tempVector = new Vector<MSI>();
            while (iteratorContent.hasNext()) {
                int value = (Integer)iteratorContent.next();
                tempVector.add(new MSI(value));
            }
            if (sender.equals(downstream.getAID)) {
                int oldSize = downstreamMsi.size();
                downstreamMsi.addAll(0, tempVector);
                downstreamMsi.setSize(oldSize);
            }
            if (sender.equals(upstream.getAID)) {
                int oldSize = upstreamMsi.size();
                upstreamMsi.addAll(0, tempVector);
                upstreamMsi.setSize(oldSize);
            }
            myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
            return result;
        }
    }

    /**
     * Convert an MSI to a JSON String object
     * @param input MSI to be converted to JSON String
     * @return JSON String
     */
    public static String MsiToJson(Vector<MSI> input) {
        Iterator<MSI> inputIterator = input.iterator();
        JSONArray outputArray = new JSONArray();
        while(inputIterator.hasNext()) {
            outputArray.put(inputIterator.next().getSymbol());
        }
        return outputArray.toString();
    }

    /**
     * Compare 2 MSI vectors to check if the MSI content is equal
     * @param v1 First vector to compare
     * @param v2 Second vector to compare
     * @return True is MSI's in both vectors are the same, returns falase if not.
     */
    public static boolean VectorEqual(Vector<MSI> v1, Vector<MSI> v2) {
        boolean result = true;
        if(v1.size() != v2.size()) {
            result = false;
            return result;
        }
        Iterator<MSI> v1Iterator = v1.iterator();
        Iterator<MSI> v2Iterator = v2.iterator();
        while (v1Iterator.hasNext()) {
            if (v1Iterator.next().getSymbol() != v2Iterator.next().getSymbol()) {
                result = false;
                return result;
            }
        }
        return result;
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
     * @return the downstreamMsi
     */
    public Vector<MSI> getDownstreamMsi() {
        return downstreamMsi;
    }

    /**
     * @return the upstreamMsi
     */
    public Vector<MSI> getUpstreamMsi() {
        return upstreamMsi;
    }

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
}