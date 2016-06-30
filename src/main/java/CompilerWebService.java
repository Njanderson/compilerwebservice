import static spark.Spark.get;

/**
 * This runs on port 4567.
 * Go to http://54.224.114.104:4567
 */
public class CompilerWebService {
    public static void main(String[] args) {
        get("/hello", (req, res) -> "Hello world"
        );
    }
}
