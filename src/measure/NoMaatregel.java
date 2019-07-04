package measure;

public class NoMaatregel extends Exception {

    private static final long serialVersionUID = 1L;

    public NoMaatregel() {
    }
 
    public String toString() {
       return "No measure found";
    }
}