package behaviour;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Iterator;

import agents.centralAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import measure.CrossMeasure;

public class SetMeasure extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private centralAgent outer;

    private long T;
    private long simLastTime;
    private LocalTime time;

    public SetMeasure(Agent a, long period, long wakeUpTime) {
        super(a, period);
        outer = (centralAgent)a;
        T = period;
        simLastTime = wakeUpTime;
        time = LocalTime.of(1, 0, 0);
    }

    @Override
    protected void onTick() {
        boolean done = false;
        long simCurrentTime = System.currentTimeMillis();
        long elapsedTime = simCurrentTime - simLastTime;
        long steps = elapsedTime/T;
        simLastTime += steps*T;
        while (steps > 0) {
            while(!done) {
                try {
                    String line = null;
                    line = outer.getMeasureReader().readLine();
                    String[] values = line.split(",");
                    LocalTime lineStartTime = LocalTime.parse(values[1]);
                    if (lineStartTime.compareTo(time.plusMinutes(1)) > -1) {
                        outer.getMeasureReader().reset();
                        done = true;
                    } else {
                        outer.getMeasureReader().mark(1000);
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
                        CrossMeasure mr = new CrossMeasure(outer.getAID(), time, LocalTime.parse(values[3]), values[4], startKm, endKm, lanes);
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
            Iterator<CrossMeasure> iterator = outer.getMeasures().iterator();
            while (iterator.hasNext()) {
                CrossMeasure mr = iterator.next();
                if (mr.getEndTime().compareTo(time) == 0) {
                    // remove measure and  send cancel
                    cancelMeasure(mr);
                    iterator.remove();
                }
            }
            time = time.plusMinutes(1);
            steps -= 1;
        }
    }

    private void addMeasure (CrossMeasure mr) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("ADD");
        newMsg.setContent(mr.toJSON().toString());
        newMsg.addReceiver(outer.getCentralTopic());
        myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
    }

    private void cancelMeasure (CrossMeasure mr) {
        ACLMessage newMsg = new ACLMessage(ACLMessage.REQUEST);
        newMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        newMsg.setOntology("CANCEL");
        newMsg.setContent(mr.toJSON().toString());
        newMsg.addReceiver(outer.getCentralTopic());
        myAgent.addBehaviour(new AchieveREInitiator(myAgent,newMsg));
    }
}