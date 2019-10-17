package behaviour;

import java.util.ArrayList;
import java.util.Arrays;

import agents.osAgent;
// import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
// import jade.core.behaviours.TickerBehaviour;
import measure.MSI;
import measure.Measure;

public class CompilerBehaviour extends OneShotBehaviour{

    // public CompilerBehaviour(Agent a, long period) {
    //     super(a, period);
    //     // TODO Auto-generated constructor stub
    // }

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
            MSI[] congestionMsi = { new MSI(MSI.NF_50), new MSI(MSI.NF_50), new MSI(MSI.NF_50) };
            newMeasures.add(new Measure(Measure.AID, outer.getLocal().getRoad(), outer.getLocal().getLocation(),
                    congestionMsi));
        }

        // Central message
        outer.getCentralMeasures().forEach(n -> {
            if ((n.getEnd() <= outer.getLocal().getLocation() && outer.getLocal().getLocation() <= n.getStart())
                    || (outer.getLocal().getLocation() < n.getStart()
                            && outer.getConfig().get(outer.getConfig().size() / 2 + 1).getLocation() > n.getEnd())
                    || (outer.getLocal().getLocation() > n.getEnd()
                            && outer.getConfig().get(outer.getConfig().size() / 2 - 1).getLocation() < n.getStart())) {
                newMeasures.add(new Measure(Measure.CENTRAL, outer.getLocal().getRoad(), outer.getLocal().getLocation(),
                        n.getDisplay()));
            }
        });

        // Neighbour messages

        // EOR
        // Central measure
        outer.getMeasures()[outer.getMeasures().length / 2 - 1].stream().filter(n -> n.getType() == Measure.CENTRAL)
                .forEach(n -> {
                    MSI[] EORMsi = { new MSI(MSI.EOR), new MSI(MSI.EOR), new MSI(MSI.EOR) };
                    newMeasures.add(new Measure(n.getID(), Measure.REACTION, outer.getLocal().getRoad(),
                            outer.getLocal().getLocation(), EORMsi));
                });

        // AID
        outer.getMeasures()[outer.getMeasures().length / 2 + 1].stream().filter(n -> n.getType() == Measure.AID)
                .forEach(n -> {
                    MSI[] congestionLeadinMsi = { new MSI(MSI.NF_70), new MSI(MSI.NF_70), new MSI(MSI.NF_70) };
                    newMeasures.add(new Measure(n.getID(), Measure.REACTION, outer.getLocal().getRoad(),
                            outer.getLocal().getLocation(), congestionLeadinMsi));
                });

        // Central Measure
        for (int i = 1; i < outer.getMeasures().length / 2; i++) {
            ArrayList<Measure> measures = outer.getMeasures()[outer.getMeasures().length / 2 + i];
            for (int j = 0; j < measures.size(); j++) {
                if (measures.get(j).getType() == Measure.CENTRAL) {
                    MSI[] previousMsi = measures.get(j).getDisplay();
                    MSI[] newMsi = new MSI[previousMsi.length];
                    for (int k = 0; k < i; k++) {
                        newMsi = new MSI[previousMsi.length];
                        Arrays.setAll(newMsi, n -> {
                            return new MSI();
                        });
                        // Determine upstream arrow and cross pattern
                        ArrowConfig(newMsi, previousMsi);

                        // Determine upstream continuation of measure
                        MSI[] tempDisplay = Arrays.stream(previousMsi).map(n -> ContV(n)).toArray(MSI[]::new);
                        for (int l = 0; l < newMsi.length; l++) {
                            newMsi[l].changeState(tempDisplay[l].getSymbol());
                        }

                        // dif-v spread measure across road
                        DifV(newMsi);

                        previousMsi = newMsi;
                    }
                    newMeasures.add(new Measure(measures.get(j).getID(), Measure.REACTION, outer.getLocal().getRoad(),
                            outer.getLocal().getLocation(), newMsi));
                }
            }
        }

        // // Downstream messages
        // // Central measure
        // outer.getDownstreamMeasures().stream()
        // .filter(n -> n.getType() == Measure.CENTRAL)
        // .forEach(
        // n -> {
        // MSI[] newMsi = {new MSI(MSI.BLANK), new MSI(MSI.BLANK), new MSI(MSI.BLANK)};
        // // Determine upstream arrow and cross pattern
        // ArrowConfig(newMsi,n.getDisplay());

        // // Determine upstream continuation of measure
        // MSI[] tempDisplay = Arrays.stream(n.getDisplay())
        // .map(m -> ContV(m))
        // .toArray(MSI[]::new);
        // for (int i = 0; i < newMsi.length; i++) {
        // newMsi[i].changeState(tempDisplay[i].getSymbol());
        // }

        // // dif-v spread measure across road
        // DifV(newMsi);

        // newMeasures.add(new Measure(n.getID(), Measure.REACTION,
        // outer.getLocal().getRoad(), outer.getLocal().getLocation(), newMsi));
        // }
        // );

        // send local measures
        // if (!newMeasures.equals(outer.getLocalMeasures())) {
        outer.setLocalMeasures(newMeasures);
        // outer.sendMeasures();
        myAgent.addBehaviour(new HandleMsi(myAgent));
        // }
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