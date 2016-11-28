import java.io.*;
import java.util.Scanner;

import static spark.Spark.*;

/**
 * This runs on port 4567.
 * Go to http://54.224.114.104:4567
 */
public class CompilerWebService {
    public static void main(String[] args) {
        // https://github.com/tipsy/spark-file-upload/blob/master/src/main/java/UploadExample.java
//        get("/", (req, res) ->
//                "<form method='post' enctype='multipart/form-data'>" // note the enctype
//                        + "    <input type='file' name='uploaded_file' accept='.png'>" // make sure to call getPart using the same "name" in the post
//                        + "    <button>Upload picture</button>"
//                        + "</form>"
//        );

        CompilerWebService webService = new CompilerWebService();
        enableCORS("http://njanderson.me", "POST", "Content-Type");
        post("/compile", (req, res) -> {
            try {
                // scala -cp cafebabe_2.11-1.2.jar slacc_2.11-1.2.jar <program.slac>
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("/raw/compile-source.slacc"), "utf-8"))) {
                    writer.write(req.body());
                }
                Process proc = Runtime.getRuntime().exec("scala -cp /code/src/main/resources/cafebabe.jar " +
                        "/code/src/main/resources/slacc-compiler.jar -d /classfiles /raw/compile-source.slacc");
                proc.waitFor();
                proc = Runtime.getRuntime().exec("java -cp /classfiles Main");
                proc.waitFor();
                // Then retrieve the process output
                InputStream in = proc.getInputStream();
                InputStream err = proc.getErrorStream();

                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                String result = scanner.hasNext() ? scanner.next() : "";
                webService.writeResult("/out/out.txt", result);
                res.header("Access-Control-Allow-Origin", "*");
                return result;
            } catch (Exception e) {
                return "Failed out.";
            }

        });

        get("/hello", (req, res) -> "Hello CORS"
        );
    }

    // Enables CORS on requests. This method is an initialization method and should be called once.
    private static void enableCORS(final String origin, final String methods, final String headers) {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
        });
    }

    private void writeResult(String resultPath, String result) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(resultPath), "utf-8"))) {
            writer.write(result);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
