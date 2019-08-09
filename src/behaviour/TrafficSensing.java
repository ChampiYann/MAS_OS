package behaviour;

import java.io.IOException;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public class TrafficSensing extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    private long T;
    private long simLastTime;

    public TrafficSensing(Agent a, long period, long wakeUpTime) {
        super(a, period);
        outer = (osAgent)a;
        T = period;
        simLastTime = wakeUpTime;
        // time = LocalTime.of(1, 0, 0);
    }

    @Override
    protected void onTick() {
        long simCurrentTime = System.currentTimeMillis();
        long elapsedTime = simCurrentTime - simLastTime;
        long steps = elapsedTime/T;
        simLastTime += steps*T;
        while (steps > 0) {
            boolean newCongestion = false;
            String newLine = null;
            try {
                newLine = outer.getCongestionReader().readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String[] values = newLine.split(",");
            String[] elements = values[1].split(" ");
            for (int i = 0; i < elements.length; i++) {
                if (Integer.parseInt(elements[i]) == 1) {
                    newCongestion = true;
                }
            }
            if (newCongestion == true && outer.getCongestion() == false) {
                outer.setCongestion(true);
                myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
                System.out.println("Congestion detected!");

            } else if (newCongestion == false && outer.getCongestion() == true) {
                outer.setCongestion(false);
                myAgent.addBehaviour(new BrainBehaviour((osAgent)myAgent));
                System.out.println("Congestion cleared!");

            }
            steps -= 1;
        }
    }
}