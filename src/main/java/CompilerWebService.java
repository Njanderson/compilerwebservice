import com.google.gson.Gson;
import objects.CompileJobRequest;
import objects.CompileJobResponse;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * This runs on port 4567.
 * Go to http://54.224.114.104:4567
 */
public class CompilerWebService {

    static final Gson gson = new Gson();

    public static void main(String[] args) {
        // https://github.com/tipsy/spark-file-upload/blob/master/src/main/java/UploadExample.java
//        get("/", (req, res) ->
//                "<form method='post' enctype='multipart/form-data'>" // note the enctype
//                        + "    <input type='file' name='uploaded_file' accept='.png'>" // make sure to call getPart using the same "name" in the post
//                        + "    <button>Upload picture</button>"
//                        + "</form>"
//        );

        post("/compile", (reqJson, res) -> {
            try {
                CompileJobRequest request = gson.fromJson(reqJson.body(), CompileJobRequest.class);
                CompileJobResponse response = new CompileJobResponse();
                String suffix = reqJson.ip() + System.currentTimeMillis();
                String sourceFileName = "/raw/compile-source" + suffix + ".slacc";
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(sourceFileName), "utf-8"))) {
                    writer.write(request.code);
                }
                Process proc = Runtime.getRuntime().exec("scala -cp /code/src/main/resources/cafebabe.jar " +
                        "/code/src/main/resources/slacc-compiler.jar -d /classfiles/" + suffix + " " + sourceFileName);
                boolean success = proc.waitFor(500, TimeUnit.MILLISECONDS);
                InputStream in;
                InputStream err;
                in = proc.getInputStream();
                err = proc.getErrorStream(); // Ignore error stream?

                Scanner scanner = new Scanner(err).useDelimiter("\\A");
                String buildMessage = scanner.hasNext() ? scanner.next() : "";
                response.buildMessage = buildMessage;

                proc = Runtime.getRuntime().exec("java -cp /classfiles Main");
                success = proc.waitFor(2000, TimeUnit.MILLISECONDS);
                // Then retrieve the process output
                in = proc.getInputStream();
                err = proc.getErrorStream(); // Ignore error stream?

                scanner = new Scanner(success ? in : err).useDelimiter("\\A");
                String runOutput = scanner.hasNext() ? scanner.next() : "";
                response.output = runOutput;
                res.header("Access-Control-Allow-Origin", "http://njanderson.me");

                // Cleanup
                File sourceFile = new File(sourceFileName);
                sourceFile.delete(); // Consider cleanup job that parses file names and uses the timestamps
                File outDir = new File("/classfiles/" + suffix);
                File[] contents = outDir.listFiles();
                for (File f : contents) {
                    f.delete();
                }

                String ret = gson.toJson(response);
                res.body(ret);
                return ret;
            } catch (Exception e) {
                return "Failed out.";
            }
        });

        get("/hello", (req, res) -> "Hello World!"
        );
    }

}
