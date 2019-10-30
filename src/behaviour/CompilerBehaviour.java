package behaviour;

import java.util.Arrays;
import java.util.Iterator;

import agents.osAgent;
import jade.core.behaviours.OneShotBehaviour;
import measure.MSI;
import measure.Measure;

public class CompilerBehaviour extends OneShotBehaviour{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void action() {
        osAgent outer = (osAgent) myAgent;

        //add all rules here
        //Normalise AID rule to 50
        //DIF-V to spread to other traffic streams
        //Continue-v rule or Extend-v rule depending on relation with downstream OS
        //Lowest speed of traffic stream to display
        //(STAT-V, MAX-V, DYN-V)
        //Flasher rule if speed is lower that upstream
        //
        
        MSI[] newMsi = new MSI[outer.getLocal().getLanes()];
        for (int i = 0; i < outer.getLocal().getLanes(); i++) {
            newMsi[i] = new MSI();
        }
        // Iterator<MSI> newMsiIterator = newMsi.iterator();
        //Check central rules first
        Iterator<Measure> centralIterator = outer.getCentralMeasures().iterator();
        while(centralIterator.hasNext()) {
            Measure centralMeasure = centralIterator.next();
            if ((centralMeasure.getEnd() <= outer.getLocal().getLocation() && outer.getLocal().getLocation() <= centralMeasure.getStart()) ||
            (outer.getLocal().getLocation() < centralMeasure.getStart() && outer.getDownstreamNeighbours().get(0).getConfig().getLocation() > centralMeasure.getEnd()) ||
            (outer.getLocal().getLocation() > centralMeasure.getEnd() && outer.getUpstreamNeighbours().get(osAgent.nsize-1).getConfig().getLocation() < centralMeasure.getStart())) {
                MSI[] centralMsi = centralMeasure.getDisplay();
                // newMsiIterator = newMsi.iterator();
                // while (centralMsiIterator.hasNext()) {
                //     newMsiIterator.next().changeState(centralMsiIterator.next().getSymbol());
                // }
                for (int i = 0; i < newMsi.length; i++) {
                    newMsi[i].changeState(centralMsi[i].getSymbol());
                }
            }
        }
        //DIF-V rule
        DifV(newMsi);

        //Check local congestion second
        // newMsiIterator = newMsi.iterator();
        if (outer.getCongestion() == true) {
            // while(newMsiIterator.hasNext()) {
            //     newMsiIterator.next().changeState(MSI.NF_50);
            // }
            Arrays.stream(newMsi).forEach(n -> n.changeState(MSI.NF_50));
        }

        //Check downstream third
        try {
            //Determine arrow configuration
            ArrowConfig(newMsi,outer.getDownstreamNeighbours().get(0).getMsi());

            // Iterator<MSI> downstreamMsiIterator = outer.getDownstreamMsi().iterator();
            MSI[] downstreamMsi = outer.getDownstreamNeighbours().get(0).getMsi();
            //TODO: change value to ficitional downstream msi and apply taper, expansion, etc rules afterwards
            // newMsiIterator = newMsi.iterator();

            for (int i = 0; i < downstreamMsi.length; i++) {
                MSI nextDownstreamMsi = downstreamMsi[i];
                if (nextDownstreamMsi.getSymbol() == MSI.ARROW_L || nextDownstreamMsi.getSymbol() == MSI.ARROW_R) {
                    newMsi[i].changeState(MSI.NF_90);
                } else if (nextDownstreamMsi.getSymbol() == MSI.NF_70 || nextDownstreamMsi.getSymbol() == MSI.F_70) {
                    newMsi[i].changeState(MSI.NF_90);
                } else if (nextDownstreamMsi.getSymbol() == MSI.NF_50 || nextDownstreamMsi.getSymbol() == MSI.F_50) {
                    newMsi[i].changeState(MSI.NF_70);
                } else if (nextDownstreamMsi.getSymbol() == MSI.NF_90 || nextDownstreamMsi.getSymbol() == MSI.F_90) {
                    newMsi[i].changeState(MSI.BLANK);
                // } else {
                //     newMsiIterator.next();
                }
            }
        } catch (NullPointerException e) {
            //This agent has no downstream neighbour
        }

        //Check DIF-V again
        DifV(newMsi);
        
        //Check upstream last
        try {
            MSI[] upstreamMsi = outer.getUpstreamNeighbours().get(osAgent.nsize-1).getMsi();
            // newMsiIterator = newMsi.iterator();
            for (int i = 0; i < upstreamMsi.length; i++) {
                MSI nextUpstreamMsi = upstreamMsi[i];
                MSI nextNewMsi = newMsi[i];
                if (nextUpstreamMsi.getSymbol() == MSI.X) {
                    nextNewMsi.changeState(MSI.EOR);
                }
                if (nextUpstreamMsi.getSymbol() > nextNewMsi.getSymbol() && nextNewMsi.getSymbol() > 3 && nextNewMsi.getSymbol() < 9) {
                    nextNewMsi.changeState(nextNewMsi.getSymbol() - 1);
                }
            }
        } catch (NullPointerException e) {
            //This agent has no upstream neighbour
        }
        
        //Check DIF-V again
        DifV(newMsi);

        if (!Arrays.equals(newMsi, outer.getMsi())) {
            //update MSI with display
            outer.setMsi(newMsi);
            outer.sendCentralUpdate();
            //send messages to neighbours
            // outer.sendMeasure(outer.getDownstream(), osAgent.DISPLAY, MSI.MsiToJson(outer.getMsi()));
            // outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, MSI.MsiToJson(outer.getMsi()));
        }
    }

    private void DifV(MSI[] newMsi) {
        for (int i = 0; i < newMsi.length; i++) {
            for (int j = 0; j < newMsi.length; j++) {
                if (newMsi[j].getSymbol() == MSI.X && i != j) {
                    newMsi[i].changeState(MSI.NF_70);
                }
                if (newMsi[j].getSymbol() == MSI.NF_70 && i > j) {
                    newMsi[i].changeState(MSI.NF_70);
                }
                if (newMsi[j].getSymbol() == MSI.NF_70 && i < j) {
                    newMsi[i].changeState(MSI.NF_90);
                }
                if (newMsi[j].getSymbol() == MSI.NF_90 && i != j) {
                    newMsi[i].changeState(MSI.NF_90);
                }
                if (newMsi[j].getSymbol() == MSI.EOR && i != j) {
                    newMsi[i].changeState(MSI.EOR);
                }
            }
        }
    }

    private void ArrowConfig(MSI[] newMsi, MSI[] downstreamMsi) {
        if (downstreamMsi[0].getSymbol() == MSI.X && downstreamMsi[1].getSymbol() == MSI.X) {
            newMsi[0].changeState(MSI.X);
            newMsi[1].changeState(MSI.ARROW_R);
        } else if (downstreamMsi[1].getSymbol() == MSI.X && downstreamMsi[2].getSymbol() == MSI.X) {
            newMsi[1].changeState(MSI.ARROW_L);
            newMsi[2].changeState(MSI.X);
        } else if (downstreamMsi[0].getSymbol() == MSI.X) {
            newMsi[0].changeState(MSI.ARROW_R);
        } else if (downstreamMsi[2].getSymbol() == MSI.X) {
            newMsi[2].changeState(MSI.ARROW_L);
        }
    }
}