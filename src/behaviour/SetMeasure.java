package behaviour;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Vector;

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
            LocalTime timeInput = (LocalTime)input;
            outer.setTime(timeInput);
            boolean done = false;
            while(!done) {
                try {
                    String line = null;
                    line = outer.getMeasureReader().readLine();
                    String[] values = line.split(",");
                    LocalTime lineStartTime = LocalTime.parse(values[1]);
                    if (lineStartTime.compareTo(timeInput.plusMinutes(1)) > -1) {
                        outer.getMeasureReader().reset();
                        done = true;
                    } else {
                        outer.getMeasureReader().mark(1000);
                        // Add measure to list en send it
                        Vector<Integer> lanes = new Vector<Integer>(values.length-7);
                        for (int i = 0; i < lanes.capacity(); i++) {
                            lanes.add(Integer.parseInt(values[7+i]));
                        }
                        double startKm = Double.parseDouble(values[6]);
                        double endKm = Double.parseDouble(values[5]);
                        // Vector<Double> osList = findOsInRange(startKm,endKm);
                        Measure mr = new Measure(outer.getAID(), timeInput, LocalTime.parse(values[3]), values[4], startKm, endKm, lanes);
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
                if (mr.getEndTime().compareTo(timeInput) == 0) {
                    // remove measure and  send cancel
                    cancelMeasure(mr);
                    iterator.remove();
                }
            }
        }
    }

    // private Vector<Double> findOsInRange(double start, double end) {
    //     Vector<Double> outputVector = new Vector<Double>();
    //     Iterator<Configuration> osIterator = outer.getOS().iterator();
    //     while (osIterator.hasNext()) {
    //         Configuration nextOs = osIterator.next();
    //         if (nextOs.location >= start &&  nextOs.location <= end) {
    //             outputVector.add(nextOs.location);
    //         }
    //     }
    //     return outputVector;
    // }

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