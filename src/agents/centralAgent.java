package agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;

import config.Configuration;
import gui.centralGui;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import measure.CrossMeasure;
import measure.Measure;
import measure.NoMeasure;

public class centralAgent extends Agent {

    // Measures
    private ArrayList<CrossMeasure> measures = new ArrayList<CrossMeasure>();

    // GUI
    transient protected centralGui myGui;

    // List of OSes
    private ArrayList<Configuration> OS = new ArrayList<Configuration>();

    private AID centralTopic;

    private BufferedReader measureReader;

    private LocalTime time;

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

            setEnabledO2ACommunication(true,0);
            Behaviour o2aBehaviour = new SetMeasure(this, osAgent.minute);
            addBehaviour(o2aBehaviour);
            setO2AManager(o2aBehaviour);
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

        try {
            FileReader reader = new FileReader("measures\\central.txt");
            measureReader = new BufferedReader(reader);
            measureReader.mark(1000);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public class SetMeasure extends TickerBehaviour {

        public SetMeasure(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            Object input = getO2AObject();
            if (input != null) {
                time = (LocalTime)input;
                boolean done = false;
                while(!done) {
                    try {
                        String line = null;
                        line = measureReader.readLine();
                        String[] values = line.split(",");
                        LocalTime lineStartTime = LocalTime.parse(values[1]);
                        if (lineStartTime.compareTo(time.plusMinutes(1)) > -1) {
                            measureReader.reset();
                            done = true;
                        } else {
                            measureReader.mark(1000);
                            // Add measure to list en send it
                            boolean[] lanes = new boolean[values.length-7];
                            for (int i = 0; i < lanes.length; i++) {
                                lanes[i] = Boolean.parseBoolean(values[7+i]);
                            }
                            float startKm = Float.parseFloat(values[6]);
                            float endKm = Float.parseFloat(values[5]);
                            if (startKm == endKm) {
                                endKm -= (float)0.001;
                            }
                            CrossMeasure mr = new CrossMeasure(getAID(), time, LocalTime.parse(values[3]), values[4], startKm, endKm, lanes);
                            measures.add(mr);
                            addMeasure(mr);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        done = true;
                    }
                }
                // cycle though active measures and cancel those that need cancelling
                Iterator<CrossMeasure> iterator = measures.iterator();
                while (iterator.hasNext()) {
                    CrossMeasure mr = iterator.next();
                    if (mr.getEndTime().compareTo(time) == 0) {
                        // remove measure and  send cancel
                        cancelMeasure(mr);
                        iterator.remove();
                    }
                }
            }
        }

        private void addMeasure (CrossMeasure mr) {
            ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
            newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            newMsg.setOntology("ADD");
            newMsg.setContent(mr.toJSON().toString());
            newMsg.addReceiver(centralTopic);
            myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
        }

        private void cancelMeasure (CrossMeasure mr) {
            ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
            newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            newMsg.setOntology("CANCEL");
            newMsg.setContent(mr.toJSON().toString());
            newMsg.addReceiver(centralTopic);
            myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
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

    public int getMeasure (int t, AID o) throws NoMeasure {

        for (int i = 0; i < measures.size(); i++) {
            Measure mr = measures.get(i);
            if (mr.getType() == t && mr.getOrigin().equals(o)) {
                return i;
            }
        }
        throw new NoMeasure();
    }

    public int getMeasure (long id) throws NoMeasure {

        for (int i = 0; i < measures.size(); i++) {
            Measure mr = measures.get(i);
            if (mr.getID() == id) {
                return i;
            }
        }
        throw new NoMeasure();
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

    /**
     * @return the time
     */
    public LocalTime getTime() {
        return time;
    }
}