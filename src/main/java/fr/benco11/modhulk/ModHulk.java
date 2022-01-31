package fr.benco11.modhulk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.BasicCurseFile;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.project.CurseProject;
import okhttp3.HttpUrl;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModHulk {
    private static final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());

    public static void main(String... argA) throws IOException {
        String[] args = (argA.length > 0) ? String.join(" ", argA).split("@") : new String[0];

        String filePath = (args.length > 0) ? args[0] : scanLine("Please enter the path of the mods' list file :");
        String folderPath = (args.length > 1) ? args[1] : scanLine("Please enter the destination folder's path :");
        String version = (args.length > 2) ? args[2] : scanLine("Please enter the minecraft target version :");
        boolean selenium = (args.length > 3) ? Boolean.parseBoolean(args[3]) : scanBoolean("Please enter if you want to use selenium firefox (your browser) when the program can't get mod informations :");
        String platform = (args.length > 4) ? args[4] : "";

        while(!platform.equalsIgnoreCase("Forge") && !platform.equalsIgnoreCase("Fabric")) {
            platform = scanLine("Please enter Forge or Fabric :");
            platform = (platform.equalsIgnoreCase("Forge")) ? "Forge" : ((platform.equalsIgnoreCase("Fabric") ? "Fabric" : platform));
        }

        File geckoDriver = new File(System.getProperty("user.home") + "\\geckodriver.exe");
        geckoDriver.deleteOnExit();

        WebDriver driver = null;

        if(selenium) {
            if(geckoDriver.exists()) {
                Runtime.getRuntime().exec("taskkill /F /IM geckodriver.exe");
                geckoDriver.delete();
            }
            try(InputStream is = new URL(getLatestGeckoDriverURL(getLatestGeckoDriverRepoURL(), "win64")).openStream()) {
                unzip(is, System.getProperty("user.home"));
                System.setProperty("webdriver.gecko.driver", geckoDriver.getPath());
            }
           driver = new FirefoxDriver();
        }

        List<String> mods = Files.readAllLines(new File(filePath).toPath())
                .stream()
                .filter(a -> a.matches("https?:\\/{2}(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&/=]*)"))
                .collect(Collectors.toList());

        System.out.println("Processing...\n");

        AtomicInteger success = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();
        WebDriver finalDriver = driver;
        mods.forEach(a -> {
            try {
                processMod(version, folderPath, processModProject(a, selenium, finalDriver));
                success.getAndIncrement();
            } catch(Exception e) {
                System.out.printf("Error during process of %s, retrying !%n", a);
                sleep(20);
                try {
                    processMod(version, folderPath, processModProject(a, selenium, finalDriver));
                    success.getAndIncrement();
                } catch(Exception ex) {
                    error.getAndIncrement();
                    System.err.printf("Can't process %s !%n", a);
                    if(ex instanceof CurseException)
                        ex.printStackTrace();
                }
            }
            sleep(20);
        });

        if(selenium) driver.close();

        System.out.printf("Download finished with %o success and %o errors.", success.get(), error.get());
    }

    public static String scanLine(String text) {
        System.out.println(text);
        return scanner.nextLine();
    }

    public static boolean scanBoolean(String text) {
        System.out.println(text);
        return scanner.nextBoolean();
    }

    public static CurseProject processModProject(String url, boolean selenium, WebDriver driver) throws Exception {
        Optional<CurseProject> oP;
        try {
            oP = CurseAPI.project(HttpUrl.get(url));
            if(oP.isPresent()) return oP.get();
        } catch(CurseException e) {
            if(!selenium) {
                System.err.printf("Can't get project %s %n", url);
                throw new Exception();
            }
        }
        System.out.printf("Can't get project %s, trying with selenium %n", url);
        try {
            oP = CurseAPI.project(getProjectId(url, driver));
            if(oP.isPresent()) return oP.get();
        } catch(CurseException e) {
            System.err.printf("Can't get project %s %n", url);
            e.printStackTrace();
        }
        throw new Exception();
    }

    public static int getProjectId(String url, WebDriver driver) {
        driver.get(url);
        int id = Integer.parseInt(driver.findElements(By.xpath("//a[@data-project-id]")).get(0).getAttribute("data-project-id"));
        driver.close();
        return id;
    }

    public static void processMod(String version, String folderPath, CurseProject project) throws Exception {
        Optional<CurseFile> fileO = project.files().stream()
                .filter(b -> b.gameVersionStrings().contains(version) && b.gameVersionStrings().contains("Forge"))
                .max(Comparator.comparing(BasicCurseFile::id));
        if(fileO.isPresent()) {
            CurseFile file = fileO.get();
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.printf("%s - %.1fkb downloading...%n", project.name(), file.fileSize()/1024d);
                    file.downloadToDirectory(new File(folderPath).toPath());
                } catch(CurseException e) {
                    System.out.printf("Error during downloading of %s, retrying !%n", project.name());
                    try {
                        file.downloadToDirectory(new File(folderPath).toPath());
                    } catch(CurseException ex) {
                        System.err.printf("Can't download %s !%n", project.name());
                        ex.printStackTrace();
                    }
                }
            }).join();
        }
    }

    public static void unzip(InputStream zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if(!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(zipFilePath);
        ZipEntry entry = zipIn.getNextEntry();
        while(entry != null) {
            String filePath = destDirectory+File.separator+entry.getName();
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
                return l+"macos.zip";
            default:
                return l+"linux32.tar.gz";
        }
    }

    private static String getUrlContent(String url) throws IOException {
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

    private static final int BUFFER_SIZE = 8192;

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}
