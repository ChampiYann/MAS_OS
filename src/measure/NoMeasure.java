package measure;

public class NoMeasure extends Exception {

    private static final long serialVersionUID = 1L;

    public NoMeasure() {
    }
 
    public String toString() {
       return "No measure found";
    }
}