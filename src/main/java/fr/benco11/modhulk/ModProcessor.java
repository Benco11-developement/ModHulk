package fr.benco11.modhulk;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.BasicCurseFile;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.project.CurseProject;
import okhttp3.HttpUrl;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.benco11.modhulk.Utils.sleepThread;

public class ModProcessor {
    private final WebDriver driver;
    private final List<String> mods;
    public final AtomicInteger success;
    public final AtomicInteger error;
    private final boolean useSelenium;
    private final String mcVersion;
    private final String destinationFolder;
    private final String platform;

    public ModProcessor(List<String> lines, WebDriver driver, boolean useSelenium, String mcVersion, String destinationFolder, String platform) {
        this.mods = lines.stream()
                .filter(a -> a.matches("https?:\\/{2}(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&/=]*)"))
                .collect(Collectors.toList());

        success = new AtomicInteger();
        error =  new AtomicInteger();
        this.driver = driver;
        this.useSelenium = useSelenium;
        this.mcVersion = mcVersion;
        this.destinationFolder = destinationFolder;
        this.platform = platform;
    }

    public void processAllMods() {
        mods.forEach(modUrl -> {
            try {
                processMod(mcVersion, destinationFolder, processModProject(modUrl));
                success.getAndIncrement();
            } catch(Exception e) {
                System.out.printf("Error during process of %s, retrying !%n", modUrl);
                sleepThread(20);
                try {
                    processMod(mcVersion, destinationFolder, processModProject(modUrl));
                    success.getAndIncrement();
                } catch(Exception ex) {
                    error.getAndIncrement();
                    System.err.printf("Can't process %s !%n", modUrl);
                    if(ex instanceof CurseException)
                        ex.printStackTrace();
                }
            }
            sleepThread(20);
        });
    }

    public int getProjectId(String url) {
        driver.get(url);
        return Integer.parseInt(driver.findElements(By.xpath("//a[@data-project-id]")).get(0).getAttribute("data-project-id"));
    }

    public CurseProject processModProject(String url) throws Exception {
        Optional<CurseProject> oP;
        try {
            oP = CurseAPI.project(HttpUrl.get(url));
            if(oP.isPresent()) return oP.get();
        } catch(CurseException e) {
            if(!useSelenium) {
                System.err.printf("Can't get project %s %n", url);
                throw new Exception();
            }
        }
        System.out.printf("Can't get project %s, trying with selenium %n", url);
        try {
            oP = CurseAPI.project(getProjectId(url));
            if(oP.isPresent()) return oP.get();
        } catch(CurseException e) {
            System.err.printf("Can't get project %s %n", url);
            e.printStackTrace();
        }
        throw new Exception();
    }

    public void processMod(String version, String folderPath, CurseProject project) throws Exception {
        Optional<CurseFile> fileO = project.files().stream()
                .filter(b -> b.gameVersionStrings().contains(version) && b.gameVersionStrings().contains(platform))
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
}
