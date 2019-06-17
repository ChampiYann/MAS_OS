
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
import java.util.Random;
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

    // GUI
    transient protected osGui myGui;

    // Message templates
    // private MessageTemplate requestTemplate;
    // private MessageTemplate responseTemplate;

    // Message
    private ACLMessage receivedRequest;
    private ACLMessage sentRequest;

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

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            MessageTemplate AddTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("ADD"));
            MessageTemplate CancelTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("CANCEL"));

            addBehaviour(new AddResponder(this, AddTemplate));

            addBehaviour(new CancelResponder(this, CancelTemplate));

            // Add behaviour simulting traffic passing by but delay it by 1 second
            addBehaviour(new WakerBehaviour(this, 5000) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new TrafficSensing(myAgent, 10000));
                }
            });

            addBehaviour(new updateGui(this, 500));

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

            JSONObject msgContent = new JSONObject(request.getContent());
            try {
                int mr = getMaatregel(msgContent.getLong("ID"));
                Maatregel mt = measures.get(mr);
                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                newMsg.setOntology("CANCEL");
                newMsg.setContent(mt.toJSON().toString());
                newMsg.addReceiver(upstream);
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
            if (it - 1 != 0) {
                msgContent.put("iteration", it - 1);
                measures.add(new Maatregel(msgContent));
                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                newMsg.setOntology("ADD");
                newMsg.setContent(msgContent.toString());
                newMsg.addReceiver(upstream);
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
                        Maatregel mt = new Maatregel(Maatregel.AIDet,myAgent.getAID());
                        measures.add(mt);
                        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                        newMsg.setOntology("ADD");
                        newMsg.setContent(mt.toJSON().toString());
                        newMsg.addReceiver(upstream);
                        myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));

                    // ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    // msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    // msg.addReceiver(myAgent.getAID());

                    // JSONObject messageContent = new JSONObject();
                    // messageContent.put("lane",1);
                    // messageContent.put("symbol",Measure.NF_50);

                    // msg.setContent(messageContent.toString());

                    // send(msg);
                    } else {
                        
                    }
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
                            newMsg.addReceiver(upstream);
                            myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
    
                            measures.remove(mr);
                        } catch (NoMaatregel e) {
                            //TODO: handle exception
                        }    

                    // ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    // msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    // msg.addReceiver(myAgent.getAID());

                    // JSONObject messageContent = new JSONObject();
                    // messageContent.put("lane",1);
                    // messageContent.put("symbol",Measure.BLANK);

                    // msg.setContent(messageContent.toString());

                    // send(msg);
                }
            // }
            System.out.println("measures at "+ getLocalName() + "are" + measures.toString());
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

    class NoMaatregel extends Exception {

        public NoMaatregel() {
        }
     
        public String toString() {
           return "No measure found";
        }
    }
}