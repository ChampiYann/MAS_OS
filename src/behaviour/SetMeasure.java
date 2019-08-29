package behaviour;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import agents.centralAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
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
        Object input = outer.getO2AObject();
        if (input != null) {
            LocalDateTime dateTime = (LocalDateTime)input;
            outer.setDateTime(dateTime);
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
                    if (lineStartDateTime.compareTo(dateTime.plusMinutes(1)) > -1) {
                        outer.getMeasureReader().reset();
                        done = true;
                    } else {
                        outer.getMeasureReader().mark(1000);
                        // Add measure to list en send it
                        int[] lanes = new int[values.length-7];
                        for (int i = 0; i < lanes.length; i++) {
                            lanes[i] = Integer.parseInt(values[7+i]);
                        }
                        float startKm = Float.parseFloat(values[6]);
                        float endKm = Float.parseFloat(values[5]);
                        if (startKm == endKm) {
                            endKm -= (float)0.001;
                        }
                        LocalTime lineEndTime = LocalTime.parse(values[3]);
                        LocalDate lineEndDate = LocalDate.parse(values[2],dateFormatter);
                        LocalDateTime lineEndDateTime = lineEndDate.atTime(lineEndTime);
                        Measure mr = new Measure(outer.getAID(), lineStartDateTime, lineEndDateTime, values[4], startKm, endKm, lanes);
                        outer.getMeasures().add(mr);
                        addMeasure(mr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    done = true;
                }
            }
            // cycle though active measures and cancel those that need cancelling
            Iterator<Measure> iterator = outer.getMeasures().iterator();
            while (iterator.hasNext()) {
                Measure mr = iterator.next();
                if (mr.getEndTime().compareTo(dateTime) == 0) {
                    // remove measure and  send cancel
                    cancelMeasure(mr);
                    iterator.remove();
                }
            }
        }
    }

    private void addMeasure (Measure mr) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("ADD");
        newMsg.setContent(mr.toJSON().toString());
        newMsg.addReceiver(outer.getCentralTopic());
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