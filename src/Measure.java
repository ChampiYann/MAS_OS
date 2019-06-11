import jade.core.AID;

public class Measure {
    /**
     * This class represents a measure displayed on an MSI. It has a symbol and a
     * "reason" why it is displayed. This reason is mainly dependant on the sender
     * of the message for the symbol. This is a sort of priority.
     */
    static final int X          = 0;
    static final int ARROW_L    = 1;
    static final int ARROW_R    = 2;
    static final int NF_50      = 3;
    static final int F_50       = 4;
    static final int F_70       = 5;
    static final int BLANK      = 6;

    private static final String[] symbols = new String[7];
    static {
        symbols[BLANK] = "";
        symbols[NF_50] = "50";
        symbols[F_50] = "*50*";
        symbols[F_70] = "*70*";
        symbols[X] = "X";
        symbols[ARROW_L] = "->";
        symbols[ARROW_R] = "<-";
    }

    private int symbol;
    private AID sender;
    private long time;

    public Measure() {
        symbol = BLANK;
        sender = null;
        time = 0;
    }

    public void update(int sym, AID origin, long msgTime) {
        symbol = sym;
        sender = origin;
        time = msgTime;
    }

    public int getSymbol() {
        return symbol;
    }

    public String getSymbolString() {
        return getSymbol(symbol);
    }

    public String getSymbol(int sym) {
        return symbols[sym];
    }

    public AID getSender() {
        return sender;
    }

    public long getTime() {
        return time;
    }

    public boolean isBlank() {
        if (symbol == BLANK) {
            return true;
        } else {
            return false;
        }
    }
}
