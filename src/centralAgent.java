import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
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

public class centralAgent extends Agent {

    // Measures
    private ArrayList<Maatregel> measures = new ArrayList<Maatregel>();

    // Measure switch
    private boolean sw = false;

    // GUI
    transient protected centralGui myGui;

    @Override
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        // Set up the gui
        myGui = new centralGui(this);
        myGui.setVisible(true);

        myGui.addPortal();

        // try {
        //     TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
        //     final AID topicConfiguration = topicHelper.createTopic("CONFIGURATION");
        //     topicHelper.register(topicConfiguration);

        //     MessageTemplate requestTemplate = MessageTemplate.and(
		// 		MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
        //         MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        //     // COnfiguration response
        //     MessageTemplate ConfigTemplate = MessageTemplate.and(requestTemplate,
        //         MessageTemplate.MatchOntology("CONFIGURATION"));
        //     addBehaviour(new CyclicBehaviour(this) {
		// 		@Override
		// 		public void action() {
        //             ACLMessage newMsg = receive(ConfigTemplate);
        //             if (newMsg != null) {
        //                 Configuration newConfig = new Configuration();
        //                 newConfig.getConfigFromJSON(newMsg.getContent());
        //             }
		// 		}
        //     });
        // } catch (ServiceException e) {
        //     System.out.println("Wrong configuration for " + getAID().getName());
        //     doDelete();
        // }

        try{
        TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
		final AID topic = topicHelper.createTopic("CENTRAL");

        addBehaviour(new WakerBehaviour(this, 30000) {
            @Override
            protected void onWake() {
                myAgent.addBehaviour(new TickerBehaviour(myAgent,30000) {
                
                    @Override
                    protected void onTick() {
                        if (sw == false) {
                            sw = true;
                            boolean lanes[] = {true, false, false};
                            Maatregel mt = new Maatregel(Maatregel.CROSS, getAID(), 3, (float)59.0, (float)58.5, "RW009", lanes);
                            measures.add(mt);
                            ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                            newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                            newMsg.setOntology("ADD");
                            newMsg.setContent(mt.toJSON().toString());
                            newMsg.addReceiver(topic);
                            myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
                        } else {
                            sw = false;
                            try {
                                int mr = getMaatregel(Maatregel.CROSS,getAID());
                                Maatregel mt = measures.get(mr);
                                ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
                                newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                                newMsg.setOntology("CANCEL");
                                newMsg.setContent(mt.toJSON().toString());
                                newMsg.addReceiver(topic);
                                myAgent.addBehaviour(new SendMeasure(myAgent,newMsg));
        
                                measures.remove(mr);
                            } catch (NoMaatregel e) {
                                //TODO: handle exception
                            }
                        }
                    }
                });
            }
        });
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Central terminating.");
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
}