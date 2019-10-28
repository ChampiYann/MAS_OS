package behaviour;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

// import org.json.JSONArray;

import agents.centralAgent;
// import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import measure.MSI;
import measure.Measure;

public class SetMeasure extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private centralAgent outer;

    public SetMeasure(Agent a, long period) {
        super(a, period);
        outer = (centralAgent)a;
    }

    @Override
    protected void onTick() {
            boolean done = false;
            while(!done) {
                try {
                    String line = null;
                    line = outer.getMeasureReader().readLine();
                    String[] values = line.split(",");
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    LocalTime lineStartTime = LocalTime.parse(values[1]);
                    LocalDate lineStartDate = LocalDate.parse(values[0],dateFormatter);
                    LocalDateTime lineStartDateTime = lineStartDate.atTime(lineStartTime);
                    if (lineStartDateTime.compareTo(outer.getDateTime().plusMinutes(1)) > -1) {
                        outer.getMeasureReader().reset();
                        done = true;
                    } else {
                        outer.getMeasureReader().mark(1000);
                        // Add measure to list en send it
                        MSI[] lanes = new MSI[values.length-7];
                        for (int i = 0; i < lanes.length; i++) {
                            lanes[i] = new MSI(Integer.parseInt(values[7+i]));
                        }
                        double startKm = Double.parseDouble(values[6]);
                        double endKm = Double.parseDouble(values[5]);
                        LocalTime lineEndTime = LocalTime.parse(values[3]);
                        LocalDate lineEndDate = LocalDate.parse(values[2],dateFormatter);
                        LocalDateTime lineEndDateTime = lineEndDate.atTime(lineEndTime);
                        Measure mr = new Measure(lineStartDateTime, lineEndDateTime, values[4], startKm, endKm, lanes);
                        outer.getMeasures().add(mr);
                        addMeasure(mr);
                        // sendMeasures();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    try {
                        outer.getMeasureReader().reset();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    done = true;
                }
            }
            // cycle though active measures and cancel those that need cancelling
            Iterator<Measure> iterator = outer.getMeasures().iterator();
            while (iterator.hasNext()) {
                Measure mr = iterator.next();
                if (mr.getEndTime().compareTo(outer.getDateTime()) == 0) {
                    // remove measure and send cancel
                    cancelMeasure(mr);
                    iterator.remove();
                    // sendMeasures();
                }
            }
    }

    // private void sendMeasures () {
    //     ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
    //     newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
    //     newMsg.addReceiver(outer.getCentralTopic());
    //     newMsg.setOntology(osAgent.MEASURE);
    //     newMsg.setContent(new JSONArray(outer.getMeasures()).toString());
    //     newMsg.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
    //     myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
    // }

    private void addMeasure (Measure mr) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("ADD");
        newMsg.setContent(mr.toJSON().toString());
        newMsg.addReceiver(outer.getCentralTopic());
        newMsg.addUserDefinedParameter("time", Long.toString(System.currentTimeMillis()));
        myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
    }

    private void cancelMeasure (Measure mr) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("CANCEL");
        newMsg.setContent(mr.toJSON().toString());
        newMsg.addReceiver(outer.getCentralTopic());
        myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
    }
}