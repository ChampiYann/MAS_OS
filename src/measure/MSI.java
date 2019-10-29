package measure;

import java.util.Iterator;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPropertyIgnore;

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
        symbols[X]          = "X";
        symbols[ARROW_L]    = "*PL*";
        symbols[ARROW_R]    = "*PR*";
        symbols[F_50]       = "*50*";
        symbols[NF_50]      = "50";
        symbols[F_70]       = "*70*";
        symbols[NF_70]      = "70";
        symbols[F_90]       = "*90*";
        symbols[NF_90]      = "90";
        symbols[EOR]        = "@";
        symbols[BLANK]      = " ";
    }

    private int symbol;

    public MSI(int sym) {
        symbol = sym;
    }

    public MSI() {
        symbol = BLANK;
    }

    public MSI(JSONObject jsonContent) {
        this.symbol = jsonContent.getInt("symbol");
    }

    public void changeState(int sym) {

        if (sym > BLANK) {
            sym = BLANK;
        }
        if (getSymbol() >= sym) {
            symbol = sym;
        }     
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(int symbol) {
        this.symbol = symbol;
    }

    public int getSymbol() {
        return symbol;
    }

    @JSONPropertyIgnore
    public String getSymbolString() {
        return getSymbol(symbol);
    }

    @JSONPropertyIgnore
    public static String getSymbol(int sym) {
        return symbols[sym];
        // return Integer.toString(sym);
    }

    @JSONPropertyIgnore
    public boolean isBlank() {
        if (symbol == BLANK) {
            return true;
        } else {
            return false;
        }
    }

    public JSONObject toJSON() {
        return new JSONObject().put("symbol", this.symbol);
    }

    /**
     * Compare 2 MSI vectors to check if the MSI content is equal
     * @param v1 First vector to compare
     * @param v2 Second vector to compare
     * @return True is MSI's in both vectors are the same, returns falase if not.
     */
    public static boolean VectorEqual(Vector<MSI> v1, Vector<MSI> v2) {
        boolean result = true;
        if(v1.size() != v2.size()) {
            result = false;
            return result;
        }
        Iterator<MSI> v1Iterator = v1.iterator();
        Iterator<MSI> v2Iterator = v2.iterator();
        while (v1Iterator.hasNext()) {
            if (v1Iterator.next().getSymbol() != v2Iterator.next().getSymbol()) {
                result = false;
                return result;
            }
        }
        return result;
    }

    /**
     * Convert an MSI to a JSON String object
     * @param input MSI to be converted to JSON String
     * @return JSON String
     */
    public static String MsiToJson(Vector<MSI> input) {
        Iterator<MSI> inputIterator = input.iterator();
        JSONArray outputArray = new JSONArray();
        while(inputIterator.hasNext()) {
            outputArray.put(inputIterator.next().getSymbol());
        }
        return outputArray.toString();
    }

    @Override
    public boolean equals(Object obj) {
        MSI s = (MSI) obj;
        return this.symbol == s.symbol;
    }
}