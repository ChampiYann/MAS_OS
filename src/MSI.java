import java.util.ArrayList;
import java.util.Vector;

class MSI {
    /**
     * This class represents the displays above the road that show the measures
     * applied. They are initialized as BLANK
     */

    final osAgent outer;

    private Measure currentState;
    private int position;

    public MSI(osAgent outer, int p) {
        this.outer = outer;
        try {
            currentState = new Measure();
        } catch (Exception e) {
            System.out.println("Exception in the creation of MSI");
        }
        position = p;
    }

    public void updateState() {
        Vector<Maatregel> measures = outer.getMeasures();
        Measure mr = new Measure();
        for (int i = 0; i < measures.size(); i++) {
            int type = measures.get(i).getType();
            int iteration = measures.get(i).getIteration();
            boolean lane = measures.get(i).getLane(position);
            if (type == Maatregel.AIDet) {
                switch (iteration) {
                    case 3:
                        mr.changeDesiredState(Measure.NF_50);
                        break;
                    case 2:
                        mr.changeDesiredState(Measure.F_50);
                    break;
                    case 1:
                        mr.changeDesiredState(Measure.F_70);
                    break;
                    default:
                    break;
                }
            } else if (type == Maatregel.CROSS && lane == true) {
                switch (iteration) {
                    case 4:
                        mr.changeDesiredState(Measure.X);
                        break;
                    case 3:
                        mr.changeDesiredState(Measure.X);
                        break;
                    case 2:
                        mr.changeDesiredState(Measure.ARROW_L);
                    break;
                    case 1:
                        mr.changeDesiredState(Measure.NF_90);
                    break;
                    default:
                    break;
                }
            }
        }
        currentState = mr;
    }

    public Measure getState() {
        return currentState;
    }
}