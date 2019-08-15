package behaviour;

import java.util.Iterator;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import measure.CentralMeasure;
import measure.MSI;

public class HandleMsi extends TickerBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HandleMsi(Agent a, long period) {
        super(a, period);
        this.outer = (osAgent)a;
    }

    @Override
    protected void onTick() {
        Iterator<MSI> msiIterator = outer.getMsi().iterator();
        int laneId = 0;
        while(msiIterator.hasNext()) {
            //TODO: DIF-V rule

            MSI newMsi = new MSI();
            Iterator<CentralMeasure> centralMeasureIterator = outer.getCentralMeasures().iterator();
            while (centralMeasureIterator.hasNext()) {
                CentralMeasure nextMeasure = centralMeasureIterator.next();
                // System.out.println("nextMeasure order: " + nextMeasure.getOrder() + ", nextMeasure lane value: " + nextMeasure.getLanes().elementAt(laneId));
                if (nextMeasure.getOrder() == 0) {
                    newMsi.changeState(nextMeasure.getLanes().elementAt(laneId));
                }
                if (nextMeasure.getOrder() == 1) {
                    newMsi.changeState(nextMeasure.getLanes().elementAt(laneId)+2);
                }
                if (nextMeasure.getOrder() == 2) {
                    int nextLane = nextMeasure.getLanes().elementAt(laneId);
                    if (nextLane < 3) {
                        newMsi.changeState(nextLane+7);
                    } else {
                        newMsi.changeState(nextLane+1);
                    }
                }
                if (nextMeasure.getOrder() == -1) {
                    newMsi.changeState(MSI.EOR);
                }
            }
            if (outer.getCongestion().get(0) == true) {
                newMsi.changeState(MSI.F_50);
            }
            if (outer.getCongestion().get(1) == true) {
                newMsi.changeState(MSI.F_70);
            }
            if (outer.getCongestion().get(3) == true && newMsi.getSymbol() > 2 && newMsi.getSymbol() < 8) {
                newMsi.setSymbol(newMsi.getSymbol()+1);
            }
            msiIterator.next().setSymbol(newMsi.getSymbol());
            laneId += 1;
        }
        outer.sendCentralUpdate();
    }

}