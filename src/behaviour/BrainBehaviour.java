package behaviour;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import agents.osAgent;
import jade.core.behaviours.OneShotBehaviour;
import measure.MSI;

public class BrainBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private osAgent outer;

    public BrainBehaviour(osAgent a) {
        super();
        this.outer = a;
    }

    @Override
    public void action () {
        //TODO: add all rules here
        //Normalise AID rule to 50
        //DIF-V to spread to other traffic streams
        //Continue-v rule or Extend-v rule depending on relation with downstream OS
        //Lowest speed of traffic stream to display
        //(STAT-V, MAX-V, DYN-V)
        //Flasher rule if speed is lower that upstream
        //

        try {
            System.out.println("Agent " + outer.getLocal().location + ": downstream 1st MSI is: " + outer.getDownstreamMsi().get(1).getSymbolString());    
        } catch (Exception e) {
            //TODO: handle exception
        }
        

        Vector<MSI> newMsi = new Vector<MSI>(outer.getLocal().lanes);
        for (int i = 0; i < newMsi.capacity(); i++) {
            newMsi.add(new MSI());
        }
        Iterator<MSI> newMsiIterator = newMsi.iterator();
        //TODO: Check central rules first
        // Iterator<Measure> centralIterator = centralMeasures.iterator();
        // while(centralIterator.hasNext()) {
        //     Measure centralMeasure = centralIterator.next();
        // }
        //Check local congestion second
        newMsiIterator = newMsi.iterator();
        if (outer.getCongestion() == true) {
            while(newMsiIterator.hasNext()) {
                newMsiIterator.next().changeState(MSI.NF_50);
            }
        }
        //TODO: DIF-V rule

        //Check downstream third
        try {
            Iterator<MSI> downstreamMsiIterator = outer.getDownstreamMsi().iterator();
            //TODO: change value to ficitional downstream msi and apply taper, expansion, etc rules afterwards
            newMsiIterator = newMsi.iterator();
            while (downstreamMsiIterator.hasNext()) {
                MSI nextDownstreamMsi = downstreamMsiIterator.next();
                if (nextDownstreamMsi.getSymbol() == MSI.X) {
                    newMsiIterator.next().changeState(MSI.ARROW_L);
                }
                if (nextDownstreamMsi.getSymbol() == MSI.ARROW_L) {
                    newMsiIterator.next().changeState(MSI.NF_90);
                }
                if (nextDownstreamMsi.getSymbol() == MSI.NF_70 || nextDownstreamMsi.getSymbol() == MSI.F_70) {
                    newMsiIterator.next().changeState(MSI.NF_90);
                }
                if (nextDownstreamMsi.getSymbol() == MSI.NF_50 || nextDownstreamMsi.getSymbol() == MSI.F_50) {
                    newMsiIterator.next().changeState(MSI.NF_70);
                }
                if (nextDownstreamMsi.getSymbol() == MSI.NF_90 || nextDownstreamMsi.getSymbol() == MSI.F_90) {
                    newMsiIterator.next().changeState(MSI.BLANK);
                }
            }
        } catch (NullPointerException e) {
            //This agent has no downstream neighbour
        }
        
        //Check upstream last
        try {
            Iterator<MSI> upstreamMsiIterator = outer.getUpstreamMsi().iterator();
            ListIterator<MSI> newMsiListIterator = newMsi.listIterator();
            while (upstreamMsiIterator.hasNext()) {
                MSI nextUpstreamMsi = upstreamMsiIterator.next();
                MSI nextNewMsi = newMsiListIterator.next();
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
        

        if (!osAgent.VectorEqual(outer.getMsi(), newMsi)) {
            //update MSI with display
            outer.setMsi(newMsi);
            outer.sendCentralUpdate();
            //send messages to neighbours
            outer.sendMeasure(outer.getDownstream(), osAgent.DISPLAY, osAgent.MsiToJson(outer.getMsi()));
            outer.sendMeasure(outer.getUpstream(), osAgent.DISPLAY, osAgent.MsiToJson(outer.getMsi()));
        }
    }
}