package behaviour;

import java.util.ArrayList;
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

        ArrayList<Measure> newMeasures = new ArrayList<Measure>();

        // Traffic sensing
        if (outer.getCongestion() == true) {
            MSI[] congestionMsi = {new MSI(MSI.NF_50), new MSI(MSI.NF_50), new MSI(MSI.NF_50)};
            newMeasures.add(new Measure(Measure.AID, outer.getLocal().getRoad(), outer.getLocal().getLocation(), congestionMsi));
        }

        // Downstream message
        // AID
        outer.getDownstreamNeighbours().get(0).getMeasures().stream()
        .filter(n -> n.getType() == Measure.AID)
        .filter(n -> Arrays.stream(n.getDisplay()).allMatch(m -> m.getSymbol() == MSI.NF_50))
        .forEach(
            n -> {
                MSI[] congestionLeadinMsi = {new MSI(MSI.NF_70), new MSI(MSI.NF_70), new MSI(MSI.NF_70)};
                newMeasures.add(new Measure(n.getID(), Measure.AID, outer.getLocal().getRoad(), outer.getLocal().getLocation(), congestionLeadinMsi));
            }
        );

        // Central measure
        // Determine arrow configuration
        outer.getDownstreamNeighbours().get(0).getMeasures().stream()
        .filter(n -> n.getType() == Measure.CENTRAL)
        .forEach(
            n -> {
                MSI[] newMsi = {new MSI(MSI.BLANK), new MSI(MSI.BLANK), new MSI(MSI.BLANK)};
                // Determine upstream arrow and cross pattern
                ArrowConfig(newMsi,n.getDisplay());

                // Determine upstream continuation of measure
                MSI[] tempDisplay = Arrays.stream(n.getDisplay())
                .map(m -> ContV(m))
                .toArray(MSI[]::new);
                for (int i = 0; i < newMsi.length; i++) {
                    newMsi[i].changeState(tempDisplay[i].getSymbol());
                }

                // dif-v spread measure across road
                DifV(newMsi);
                
                newMeasures.add(new Measure(n.getID(), Measure.CENTRAL, outer.getLocal().getRoad(), outer.getLocal().getLocation(), newMsi));
            }
        );

        // Upstream message
        // Central measure
        outer.getUpstreamNeighbours().get(osAgent.nsize-1).getMeasures().stream()
        .filter(n -> n.getType() == Measure.CENTRAL)
        .filter(n -> Arrays.stream(n.getDisplay()).anyMatch(m -> m.getSymbol() == MSI.X || m.getSymbol() == MSI.NF_70))
        .forEach(
            n -> {
                MSI[] EORMsi = {new MSI(MSI.EOR), new MSI(MSI.EOR), new MSI(MSI.EOR)};
                newMeasures.add(new Measure(n.getID(), Measure.CENTRAL, outer.getLocal().getRoad(), outer.getLocal().getLocation(), EORMsi));
            }
        );

        // Central message
        outer.getCentralMeasures()
        .forEach(
            n -> {
                if ((n.getEnd() <= outer.getLocal().getLocation() && outer.getLocal().getLocation() <= n.getStart()) ||
                (outer.getLocal().getLocation() < n.getStart() && outer.getDownstreamNeighbours().get(0).getConfig().getLocation() > n.getEnd()) ||
                (outer.getLocal().getLocation() > n.getEnd() && outer.getUpstreamNeighbours().get(osAgent.nsize-1).getConfig().getLocation() < n.getStart())) {
                    newMeasures.add(new Measure(Measure.CENTRAL, outer.getLocal().getRoad(), outer.getLocal().getLocation(), n.getDisplay()));
                }
            }
        );
        
        // send local measures
        if (!newMeasures.equals(outer.getLocalMeasures())) {
            outer.setLocalMeasures(newMeasures);
            // outer.sendMeasures();
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
            outer.getUpstreamNeighbours().get(osAgent.nsize-1).getMeasures().stream().forEach(n -> {for (int i = 0; i < newMsi.length; i++) {
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
        }
    }

    /**
     * Calculates the arrow configuration for a downstream central measure
     * @param newMsi
     * @param downstreamMsi
     */
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

    /**
     * Calculate upstream continuation
     * @param m input MSI
     * @return mappes MSI
     */
    private MSI ContV(MSI m) {
        if (m.getSymbol() == MSI.ARROW_L || m.getSymbol() == MSI.ARROW_R) {
            return new MSI(MSI.NF_90);
        } else if (m.getSymbol() == MSI.NF_70) {
            return new MSI(MSI.NF_90);
        } else if (m.getSymbol() == MSI.NF_50) {
            return new MSI(MSI.NF_70);
        } else if (m.getSymbol() == MSI.NF_90) {
            return new MSI(MSI.BLANK);
        } else {
            return new MSI(MSI.BLANK);
        }
    }

    /**
     * Calculate the symbols across the road
     * @param newMsi input MSI
     */
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
}