package agents;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;

import config.Configuration;
import gui.centralGui;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
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
import measure.Maatregel;
import measure.NoMaatregel;

public class centralAgent extends Agent {

    // Measures
    private ArrayList<Maatregel> measures = new ArrayList<Maatregel>();

    // Measure switch
    private boolean sw = false;

    // GUI
    transient protected centralGui myGui;

    // List of OSes
    private ArrayList<Configuration> OS = new ArrayList<Configuration>();

    private AID centralTopic;

    @Override
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        // Set up the gui
        myGui = new centralGui(this);
        myGui.setVisible(true);

        myGui.addPortal();

        try {
            TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
            final AID topicConfiguration = topicHelper.createTopic("CONFIGURATION");
            topicHelper.register(topicConfiguration);

            MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

            // COnfiguration response
            MessageTemplate ConfigTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("CONFIGURATION"));
            addBehaviour(new ConfigurationResponder(this, ConfigTemplate));
        } catch (ServiceException e) {
            System.out.println("Wrong configuration for " + getAID().getName());
            doDelete();
        }

        try{
        TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
		centralTopic = topicHelper.createTopic("CENTRAL");

        
        addBehaviour(new WakerBehaviour(this, 10000) {
            @Override
            protected void onWake() {
                myAgent.addBehaviour(new SetMeasure(myAgent,20000));
            }
        });
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        MessageTemplate SymbolsTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("SYMBOLS"));
        addBehaviour(new AchieveREResponder(this, SymbolsTemplate) {
            @Override
            protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
                return null;
            }

            @Override
            protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
                ACLMessage msg = request.createReply();

                AID sender = request.getSender();
                JSONArray matrixJson = new JSONArray(request.getContent());
                int[] symbols = new int[3];
                for (int i = 0; i < 3; i++) {
                    symbols[i] = matrixJson.getInt(i);
                }
                myGui.update(sender, symbols);

                msg.setPerformative(ACLMessage.INFORM);
                return msg;
            }
        });
    }

    public class SetMeasure extends TickerBehaviour {

        public SetMeasure(Agent a, long period) {
            super(a, period);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void onTick() {
            if (sw == false) {
                sw = true;
                boolean lanes[] = {true, false, false};
                Maatregel mt = new Maatregel(Maatregel.CROSS, getAID(), 3, (float)12.0, (float)8.0, "RW013", lanes);
                measures.add(mt);
                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                newMsg.setOntology("ADD");
                newMsg.setContent(mt.toJSON().toString());
                newMsg.addReceiver(centralTopic);
                myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
            } else {
                sw = false;
                try {
                    int mr = getMaatregel(Maatregel.CROSS,getAID());
                    Maatregel mt = measures.get(mr);
                    ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                    newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                    newMsg.setOntology("CANCEL");
                    newMsg.setContent(mt.toJSON().toString());
                    newMsg.addReceiver(centralTopic);
                    myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));

                    measures.remove(mr);
                } catch (NoMaatregel e) {
                    //TODO: handle exception
                }
            }
        }
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
            Iterator<Configuration> iterator = OS.iterator();
            boolean exists = false;
            while (iterator.hasNext()) {
                Configuration config = iterator.next();
                if (config.getAID.equals(newConfig.getAID)) {
                    exists = true;
                }
            }
            if (!exists) {
                OS.add(newConfig);
                myGui.addPortal();
            }
            return null;
        }
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

    public ArrayList<Configuration> getOS() {
        return OS;
    }

    @Override
    protected void takeDown() {
		// Printout a dismissal message
        System.out.println("Central terminating.");
        myGui.dispose();
    }
}