
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

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
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

    // Configuration (km specification for now)
    private String configuration;
    private String road;
    private float location;
    private String side;

    // AIDs for the neighbours of the OS and the central
    private Configuration upstream;
    private Configuration local;
    private AID downstream;
    private AID central;

    // MSIs for this OS
    private MSI matrix[];

    // GUI
    transient protected osGui myGui;

    // Measures
    private ArrayList<Maatregel> measures = new ArrayList<Maatregel>();

    private boolean congestion = false;

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
            downstream = getAID((String) args[2]);
            configuration =(String)args[3];

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

            // Declare central agent
            central = getAID("central");

            // Create new upstrem configuration
            upstream = new Configuration(this);
            upstream.getAID = getAID((String) args[1]);

            // Create new local configuration
            local = new Configuration(this);
            local.getAID = getAID();

            // Set up the gui
            myGui = new osGui(this);
            myGui.setVisible(true);

            // Setup configuration based on BPS
            Pattern roadPattern = Pattern.compile("\\w{2}\\d{3}");
            Matcher roadMatcher = roadPattern.matcher(configuration);
            roadMatcher.find();
            road = roadMatcher.group();
            local.road = road;

            Pattern kmPattern = Pattern.compile("(?<=\\s)\\d{1,3},\\d");
            Matcher kmMatcher = kmPattern.matcher(configuration);
            kmMatcher.find();
            Pattern hmPattern = Pattern.compile("(?<=[+-])\\d{1,3}");
            Matcher hmMatcher = hmPattern.matcher(configuration);
            if (hmMatcher.find()) {
                location = Float.parseFloat(kmMatcher.group().replaceAll(",", ".")) + Float.parseFloat(hmMatcher.group())/1000;
                local.location = location;
            } else {
                location = Float.parseFloat(kmMatcher.group().replaceAll(",", "."));
                local.location = location;
            }

            Pattern sidePattern = Pattern.compile("(?<=\\s)[LRlr]");
            Matcher sideMatcher = sidePattern.matcher(configuration);
            sideMatcher.find();
            side = sideMatcher.group();
            local.side = side;

            try {
            TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
			final AID topic = topicHelper.createTopic("central");
			topicHelper.register(topic);

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            MessageTemplate AddTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("ADD"));
            MessageTemplate CancelTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("CANCEL"));

            addBehaviour(new AddResponder(this, AddTemplate));

            addBehaviour(new CancelResponder(this, CancelTemplate));
            } catch (ServiceException e) {}

            // COnfiguration response
            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            MessageTemplate ConfigTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("CONFIGURATION"));
            addBehaviour(new AchieveREResponder(this, ConfigTemplate) {
                @Override
                protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
                    ACLMessage result = request.createReply();
                    result.setPerformative(ACLMessage.INFORM);
                    result.setContent(configToJSON());
                    return result;
                }
            });

            // COnfiguration request
            addBehaviour(new WakerBehaviour(this, 20000) {
                @Override
                protected void onWake() {
                    ACLMessage configurationRequest = new ACLMessage(ACLMessage.REQUEST);
                    configurationRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    configurationRequest.setOntology("CONFIGURATION");
                    configurationRequest.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                    configurationRequest.addReceiver(upstream.getAID);
                    myAgent.addBehaviour(new AchieveREInitiator(myAgent, configurationRequest) {
                        @Override
                        protected void handleInform(ACLMessage inform) {
                            String messageContent = inform.getContent();
                            upstream.getConfigFromJSON(messageContent);
                            System.out.println("upstream configuration is " + messageContent);
                        }
                    });
                }
            });

            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, 10000) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, 10000));
                }
            });

            addBehaviour(new updateGui(this, 500));

            // Print message stating that the configuration was succefull
            System.out.println("OS " + getAID().getLocalName() + " configured on road " + road + " at km " + location +
                " on side " + side + " with " + lanes + " lanes, upstream agent "
                + upstream.getAID.getLocalName() + " and downstream agent " + downstream.getLocalName());

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

    public class SendMeasure extends AchieveREInitiator{

        public SendMeasure(Agent a, ACLMessage msg) {
            super(a, msg);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void handleAgree(ACLMessage agree) {
            super.handleAgree(agree);
        }

        @Override
        protected void handleFailure(ACLMessage failure) {
            super.handleFailure(failure);
        }
    }

    public class CancelResponder extends AchieveREResponder {

        public CancelResponder(Agent a, MessageTemplate mt) {
            super(a, mt);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage msg = request.createReply();
            msg.setPerformative(ACLMessage.AGREE);
            return msg;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage msg = request.createReply();

            if (request.getSender().equals(central)) {
                System.out.println("CANCEL received from central");
                // msg.setPerformative(ACLMessage.INFORM);
                // return msg;
            } 
            JSONObject msgContent = new JSONObject(request.getContent());
            try {
                int mr = getMaatregel(msgContent.getLong("ID"));
                Maatregel mt = measures.get(mr);
                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                newMsg.setOntology("CANCEL");
                newMsg.setContent(mt.toJSON().toString());
                newMsg.addReceiver(upstream.getAID);
                myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));

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
            ACLMessage msg = request.createReply();
            msg.setPerformative(ACLMessage.AGREE);
            return msg;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage msg = request.createReply();

            JSONObject msgContent = new JSONObject(request.getContent());
            int it = msgContent.getInt("iteration");
            if (((msgContent.getFloat("start") >= location &&
            msgContent.getFloat("start") < upstream.location &&
            msgContent.getFloat("end") > location) || 
            (msgContent.getFloat("end") >= location &&
            msgContent.getFloat("end") < upstream.location &&
            msgContent.getFloat("start") < location)) && msgContent.getString("road").equals(road)) {
                measures.add(new Maatregel(msgContent));
                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                newMsg.setOntology("ADD");
                newMsg.setContent(msgContent.toString());
                newMsg.addReceiver(upstream.getAID);
                myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
            } else if (it - 1 != 0 && !request.getSender().equals(central) && !request.getSender().equals(getAID())) {
                msgContent.put("iteration", it - 1);
                measures.add(new Maatregel(msgContent));
                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                newMsg.setOntology("ADD");
                newMsg.setContent(msgContent.toString());
                newMsg.addReceiver(upstream.getAID);
                myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
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
            // for (int i = 0; i < lanes; i++) {
                if (rand.nextInt(100) >= 80) {
                    if (congestion == false) {
                        congestion = true;
                        System.out.println("Congestion detected!");

                        // Create measure
                        Maatregel mt = new Maatregel(Maatregel.AIDet,local);
                        measures.add(mt);
                        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                        newMsg.setOntology("ADD");
                        newMsg.setContent(mt.toJSON().toString());
                        newMsg.addReceiver(upstream.getAID);
                        myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
                    } else {
                        congestion = false;
                        try {
                            System.out.println("Congestion cleared!");
                            int mr = getMaatregel(Maatregel.AIDet,myAgent.getAID());
                            Maatregel mt = measures.get(mr);
                            ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                            newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                            newMsg.setOntology("CANCEL");
                            newMsg.setContent(mt.toJSON().toString());
                            newMsg.addReceiver(upstream.getAID);
                            myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
    
                            measures.remove(mr);
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
    protected void onGuiEvent(GuiEvent ev) {

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

    public ArrayList<Maatregel> getMeasures() {
        return measures;
    }

    public String configToJSON() {
        JSONObject content = new JSONObject();
        content.put("road", road);
        content.put("location", location);
        content.put("side", side);
        return content.toString();
    }
}