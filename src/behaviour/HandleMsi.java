package behaviour;

import java.util.Iterator;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
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
        while(msiIterator.hasNext()) {
            MSI newMsi = new MSI();
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
        }
        outer.sendCentralUpdate();
    }

}