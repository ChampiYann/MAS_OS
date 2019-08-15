package agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import behaviour.CentralConfigurationResponder;
import behaviour.SetMeasure;
import behaviour.SymbolListener;
import config.Configuration;
import gui.centralGui;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.WakerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import measure.Measure;

public class centralAgent extends Agent {

    private static final long serialVersionUID = 1L;

    // Measures
    private ArrayList<Measure> measures = new ArrayList<Measure>();

    // GUI
    transient protected centralGui myGui;

    // List of OSes
    private ArrayList<Configuration> OS = new ArrayList<Configuration>();

    private AID centralTopic;

    private BufferedReader measureReader;

    @Override
    protected void setup() {

        Object[] args = getArguments();
        
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        // Set up the gui
        myGui = new centralGui(this, (long)args[0]);
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
                MessageTemplate.MatchTopic(topicConfiguration));
            addBehaviour(new CentralConfigurationResponder(this, ConfigTemplate));
        } catch (ServiceException e) {
            System.out.println("Wrong configuration for " + getAID().getName());
            doDelete();
        }

        try{
            TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
            centralTopic = topicHelper.createTopic("CENTRAL");

            Date wakeupDate = new Date((long) args[0]);
            addBehaviour(new WakerBehaviour(this, wakeupDate) {
                @Override
                protected void onWake() {
                    myAgent.addBehaviour(new SetMeasure(myAgent,osAgent.minute,getWakeupTime()));
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
        addBehaviour(new SymbolListener(this, SymbolsTemplate));

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

    public ArrayList<Configuration> getOS() {
        return OS;
    }

    @Override
    protected void takeDown() {
		// Printout a dismissal message
        System.out.println("Central terminating.");
        myGui.dispose();
    }

    public centralGui getMyGui() {
        return myGui;
    }

    public ArrayList<Measure> getMeasures() {
        return measures;
    }

    public AID getCentralTopic() {
        return centralTopic;
    }

    public BufferedReader getMeasureReader() {
        return measureReader;
    }
}