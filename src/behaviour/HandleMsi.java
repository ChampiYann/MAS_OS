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
                newMsi.changeState(MSI.NF_50);
            }
            if (outer.getCongestion().get(1) == true) {
                newMsi.changeState(MSI.NF_70);
            }
            msiIterator.next().setSymbol(newMsi.getSymbol());
        }
        outer.sendCentralUpdate();
    }

}