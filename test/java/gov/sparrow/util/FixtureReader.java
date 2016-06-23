package gov.sparrow.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FixtureReader {

    public static final String TEST_FIXTURES_PATH = "src/test/java/gov/sparrow/fixtures/";

    public static File getGzippedFixture(final String name) throws IOException {
        File originalFile = new File(FixtureReader.TEST_FIXTURES_PATH + name);
        File gzippedFile = File.createTempFile(name, "json.gz");

        FileInputStream fileInputStream = new FileInputStream(originalFile);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(gzippedFile));
        int c;
        while ((c = fileInputStream.read()) != -1) {
            gzipOutputStream.write(c);
        }
        fileInputStream.close();
        gzipOutputStream.close();

        return gzippedFile;
    }

    public static String readFile(String path) throws IOException {
        File jsonFile = new File(path);
        FileReader fileReader = new FileReader(jsonFile);

        int length = (int) jsonFile.length();
        char[] buffer = new char[length];

        fileReader.read(buffer, 0, length);

        String jsonString = new String(buffer);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(new JsonParser().parse(jsonString));
    }

    public static String readGZIPFile(String path) throws IOException {
        File jsonFile = new File(path);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(jsonFile))));

        StringBuilder stringBuilder = new StringBuilder();
        int c;
        while ((c = bufferedReader.read()) != -1) {
            stringBuilder.append((char) c);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(new JsonParser().parse(stringBuilder.toString()));
    }

}
