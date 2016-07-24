import java.io.*;
import java.util.Scanner;

import static spark.Spark.get;
import static spark.Spark.post;

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
        post("/compile", (req, res) -> {
            // scala -cp cafebabe_2.11-1.2.jar slacc_2.11-1.2.jar <program.slac>
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("/home/ec2-user/raw/compile-source.slacc"), "utf-8"))) {
                writer.write(req.body());
            }
            Process proc = Runtime.getRuntime().exec("scala -cp /home/ec2-user/compilerwebservice/src/main/resources/cafebabe.jar " +
                    "/home/ec2-user/compilerwebservice/src/main/resources/slacc-compiler.jar -d /home/ec2-user/classfiles /home/ec2-user/raw/compile-source.slacc");
            proc = Runtime.getRuntime().exec("java -cp /home/ec2-user/classfiles Main");

            // Then retreive the process output
            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();

            Scanner scanner = new Scanner(in).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next() : "";
            webService.writeResult("/home/ec2-user/out/out.txt", result);
            return result;
        });

        get("/hello", (req, res) -> "Hello world"
        );
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
