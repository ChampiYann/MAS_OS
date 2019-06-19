import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

public class centralAgent extends Agent {

    // Measures
    private ArrayList<Maatregel> measures = new ArrayList<Maatregel>();

    // Measure switch
    private boolean sw = false;

    @Override
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        try{
        TopicManagementHelper topicHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
		final AID topic = topicHelper.createTopic("central");

        addBehaviour(new WakerBehaviour(this, 60000) {
            @Override
            protected void onWake() {
                myAgent.addBehaviour(new TickerBehaviour(myAgent,10000) {
                
                    @Override
                    protected void onTick() {
                        if (sw == false) {
                            sw = true;
                            Maatregel mt = new Maatregel(Maatregel.CROSS, getAID(), 3, (float)58.5, (float)59.0, "RW009");
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