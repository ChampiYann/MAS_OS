package behaviour;

import java.util.Arrays;
import java.util.Iterator;

import agents.osAgent;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import measure.MSI;
import measure.Measure;

public class HandleMsi extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public HandleMsi(Agent a) {
        super(a);
        this.outer = (osAgent)a;
    }

    @Override
    public void action() {
        // Initialize new MSI array
        MSI[] newMsi = new MSI[outer.getLocal().getLanes()];
        for (int i = 0; i < newMsi.length; i++) {
            newMsi[i] = new MSI();
        }

        // Go through measure to can most restricive symbol
        Iterator<Measure> measureIterator = outer.getLocalMeasures().iterator();
        while(measureIterator.hasNext()) {
            MSI[] nextMeasureDisplay = measureIterator.next().getDisplay();
            for (int i = 0; i < newMsi.length; i++) {
                newMsi[i].changeState(nextMeasureDisplay[i].getSymbol());
            }
        }

        // check for flashers
        boolean[] flashers = new boolean[newMsi.length];
        Arrays.fill(flashers, true);
        outer.getMeasures()[outer.getMeasures().length/2-1].stream().forEach(n -> {for (int i = 0; i < newMsi.length; i++) {
            if (n.getDisplay()[i].getSymbol() <= newMsi[i].getSymbol()) {
                flashers[i] = false;
            }
        }});
        for (int i = 0; i < newMsi.length; i++) {
            if (flashers[i] == true && MSI.F_50 < newMsi[i].getSymbol() && newMsi[i].getSymbol() <= MSI.NF_90) {
                newMsi[i].changeState(newMsi[i].getSymbol()-1);
            }
        }

        // Update MSI
        if (!Arrays.equals(newMsi, outer.getMsi())) {
            outer.setMsi(newMsi);
            outer.sendCentralUpdate();
        }

        // myAgent.addBehaviour(new CompilerBehaviour());
    }

}