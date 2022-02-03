package fr.benco11.modhulk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.benco11.modhulk.Utils.FileUtils.untargz;
import static fr.benco11.modhulk.Utils.FileUtils.unzip;

public class Utils {
    private Utils() {}

    public static class FileUtils {
        private FileUtils() {}

        public static void unzip(InputStream zipInputStream, String destDirectory) throws IOException {
            File destDir = new File(destDirectory);
            if(!destDir.exists()) {
                destDir.mkdir();
            }
            ZipInputStream zipIn = new ZipInputStream(zipInputStream);
            ZipEntry entry = zipIn.getNextEntry();
            while(entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if(!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        }

        public static void untargz(InputStream tarGzInputStream, String destDirectory) throws IOException {
            File destDir = new File(destDirectory);
            if(!destDir.exists()) {
                destDir.mkdir();
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while((read = tarGzInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, read);
            }
            TarArchiveInputStream tarArchive = new TarArchiveInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            TarArchiveEntry entry = tarArchive.getNextTarEntry();
            while(entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if(!entry.isDirectory()) {
                    extractFile(tarArchive, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                entry = tarArchive.getNextTarEntry();
            }
            tarArchive.close();
        }

        public static void extractFile(InputStream is, String filePath) throws IOException {
            try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
                byte[] bytesIn = new byte[BUFFER_SIZE];
                int read;
                while((read = is.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
            }
        }

        private static final int BUFFER_SIZE = 8192;
    }

    public static class SeleniumUtils {
        private SeleniumUtils() {}

        public static String getLatestGeckoDriverRepoURL() throws IOException {
            String json = getUrlContent("https://api.github.com/repos/mozilla/geckodriver/releases/latest");
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.get("html_url").getAsString();
        }

        public static String getLatestGeckoDriverURL(String e, String os) {
            String version = e.split("/tag/")[1];
            String l = "https://github.com/mozilla/geckodriver/releases/download/"+version+"/geckodriver-"+version+"-";
            switch(os) {
                case "win32":
                    return l+"win32.zip";
                case "win64":
                    return l+"win64.zip";
                case "mac64":
                    return l+"macos.tar.gz";
                case "linux64":
                    return l+"linux64.tar.gz";
                default:
                    return l+"linux32.tar.gz";
            }
        }

        public static WebDriver loadDriver() throws IOException {
            String fileName = "geckodriver" + ((isWindows()) ? ".exe" : "");
            File geckoDriver = File.createTempFile(UUID.randomUUID().toString().replace("-", ""), fileName);
            geckoDriver.deleteOnExit();

            String os = getOS();

            if(geckoDriver.exists()) {
                for(int i = 0; i < 2; i++) Runtime.getRuntime().exec((isWindows()) ? "taskkill /F /IM " + fileName : "pkill -f \"" + fileName + "\"");
                Files.delete(geckoDriver.toPath());
            }

            try(InputStream is = new URL(getLatestGeckoDriverURL(getLatestGeckoDriverRepoURL(), os)).openStream()) {
                if(os.contains("win")) unzip(is, System.getProperty("user.home"));
                else untargz(is, System.getProperty("user.home"));
                System.setProperty("webdriver.gecko.driver", geckoDriver.getPath());
            }
            return new FirefoxDriver();
        }
    }

    public static String getUrlContent(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64; rv:77.0) Gecko/20100101 Firefox/77.0");
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String e;
            while((e = br.readLine()) != null)
                sb.append(e);
        }
        connection.disconnect();
        return sb.toString();
    }

    public static void sleepThread(long ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("win")) return (System.getProperty("os.arch").endsWith("64")) ? "win64" : "win32";
        if(os.contains("mac")) return "mac64";
        return (System.getProperty("os.arch").endsWith("64")) ? "linux64" : "linux32";
    }

    public static boolean isWindows() {
        return getOS().contains("win");
    }
}
