/**
 * Created by Njand on 6/28/2016.
 */
public class CompilerWebService {

    public static void main(String[] args) {
        get("/hello", (req, res) -> "Hello World");
    }
}
