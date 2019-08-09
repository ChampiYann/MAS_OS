package measure;

public class MSI {
    /**
     * This class represents the displays above the road that show the measures
     * applied. They are initialized as BLANK
     */

    public static final int X          = 0;
    public static final int ARROW_L    = 1;
    public static final int ARROW_R    = 2;
    public static final int F_50       = 3;
    public static final int NF_50      = 4;
    public static final int F_70       = 5;
    public static final int NF_70      = 6;
    public static final int F_90       = 7;
    public static final int NF_90      = 8;
    public static final int EOR        = 9;
    public static final int BLANK      = 10;

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
        symbols[F_90]       = "*90*";
        symbols[EOR]        = "@";
    }

    private int symbol;

    public MSI(int sym) {
        symbol = sym;
    }

    public MSI() {
        symbol = BLANK;
    }

    public void changeState(int sym) {

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