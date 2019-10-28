package agents;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

import behaviour.CentralConfigurationResponder;
import behaviour.InputHandlerBehaviour;
import behaviour.SetMeasure;
import behaviour.SymbolListener;
import config.Configuration;
import gui.centralGui;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.Behaviour;
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

    private LocalDateTime dateTime;

    @Override
    protected void setup() {

        // Object[] args = getArguments();
        
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
                MessageTemplate.MatchTopic(topicConfiguration));
            addBehaviour(new CentralConfigurationResponder(this, ConfigTemplate));
        } catch (ServiceException e) {
            System.out.println("Wrong configuration for " + getAID().getName());
            doDelete();
        }

        try{
            TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
            centralTopic = topicHelper.createTopic("CENTRAL");

            addBehaviour(new SetMeasure(this, osAgent.minute/2));

        } catch (ServiceException e) {
            e.printStackTrace();
        }

        MessageTemplate requestTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        MessageTemplate SymbolsTemplate = MessageTemplate.and(requestTemplate,
                MessageTemplate.MatchOntology("SYMBOLS"));
        addBehaviour(new SymbolListener(this, SymbolsTemplate));

        setEnabledO2ACommunication(true,0);
        Behaviour o2aBehaviour = new InputHandlerBehaviour(this);
        addBehaviour(o2aBehaviour);
        setO2AManager(o2aBehaviour);

        try {
            FileReader reader = new FileReader("measures\\central.csv");
            measureReader = new BufferedReader(reader);
            measureReader.mark(1000);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Object[] args = getArguments();
        LocalDateTime startDateTime = (LocalDateTime) args[0];
        updateGuiTime(startDateTime);
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

    /**
     * @param dateTime the time to set
     */
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * @return the time
     */
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void updateGuiTime(LocalDateTime dateTime) {
        myGui.updateTime(dateTime);
        this.dateTime = dateTime;
    }

    public void updateGuiCongestion(float location, boolean congestion) {
        myGui.updateCongestion(location,congestion);
    }

    public void updateGuiMsi(float location, String[] symbols) {
        myGui.updateRef(location, symbols);
    }

    public void guiLog() {
        myGui.log();
    }
}