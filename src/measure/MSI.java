package measure;

import java.util.Vector;

import agents.osAgent;

public class MSI {
    /**
     * This class represents the displays above the road that show the measures
     * applied. They are initialized as BLANK
     */

    final osAgent outer;

    private int position;

    static final int X          = 0;
    static final int ARROW_L    = 1;
    static final int ARROW_R    = 2;
    static final int NF_50      = 3;
    static final int F_50       = 4;
    static final int NF_70      = 5;
    static final int F_70       = 6;
    static final int NF_90      = 7;
    static final int BLANK      = 8;

    private static final String[] symbols = new String[BLANK+1];
    static {
        symbols[BLANK]      = " ";
        symbols[NF_50]      = "50";
        symbols[F_50]       = "*50*";
        symbols[NF_70]      = "70";
        symbols[F_70]       = "*70*";
        symbols[X]          = "X";
        symbols[ARROW_L]    = "*PL*";
        symbols[ARROW_R]    = "*PR*";
        symbols[NF_90]      = "90";
    }

    private int symbol;

    public MSI(osAgent outer, int p) {
        this.outer = outer;
        symbol = BLANK;
        position = p;
    }

    public void updateState() {
        Vector<Maatregel> measures = outer.getMeasures();
        symbol = BLANK;
        for (int i = 0; i < measures.size(); i++) {
            int type = measures.get(i).getType();
            int iteration = measures.get(i).getIteration();
            boolean lane = measures.get(i).getLane(position);
            if (type == Maatregel.AIDet) {
                switch (iteration) {
                    case 3:
                        changeDesiredState(NF_50);
                        break;
                    case 2:
                        changeDesiredState(F_50);
                    break;
                    case 1:
                        changeDesiredState(F_70);
                    break;
                    default:
                    break;
                }
            } else if (type == Maatregel.CROSS && lane == true) {
                switch (iteration) {
                    case 4:
                        changeDesiredState(X);
                        break;
                    case 3:
                        changeDesiredState(X);
                        break;
                    case 2:
                        changeDesiredState(ARROW_L);
                    break;
                    case 1:
                        changeDesiredState(NF_90);
                    break;
                    default:
                    break;
                }
            } else if (type == Maatregel.CROSS && lane == false) {
                switch (iteration) {
                    case 4:
                        changeDesiredState(NF_70);
                        break;
                    case 3:
                        changeDesiredState(NF_70);
                        break;
                    case 2:
                        changeDesiredState(NF_90);
                    break;
                    case 1:
                        changeDesiredState(NF_90);
                    break;
                    default:
                    break;
                }
            }
        }
    }

    public void changeDesiredState(int sym) {

        if (getSymbol() >= sym) {
            symbol = sym;
        }     
    }

    public int getSymbol() {
        return symbol;
    }

    public String getSymbolString() {
        return getSymbol(symbol);
    }

    public static String getSymbol(int sym) {
        return symbols[sym];
    }

    public boolean isBlank() {
        if (symbol == BLANK) {
            return true;
        } else {
            return false;
        }
    }
}