package fr.benco11.modhulk;

import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Supplier;

import static fr.benco11.modhulk.Utils.SeleniumUtils.loadDriver;

public class ModHulk {
    private static final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());

    public static void main(String... argA) throws IOException {
        // Get arguments
        String[] args = (argA.length > 0) ? String.join(" ", argA).split("@") : new String[0];

        String filePath = getArg(args , 0, a -> a, () -> scanLine("Please enter the path of the mods' list file :"));
        String folderPath = getArg(args , 1, a -> a, () -> scanLine("Please enter the destination folder's path :"));
        String version = getArg(args , 2, a -> a, () -> scanLine("Please enter the minecraft target version :"));
        boolean selenium = getArg(args , 3, Boolean::parseBoolean, () -> scanBoolean("Please enter if you want to use selenium firefox (your browser) when the program can't get mod informations :"));
        String platform = getPlatformArg(args);

        // Load selenium webdriver
        WebDriver driver = loadDriver();

        ModProcessor modProcessor = new ModProcessor(Files.readAllLines(new File(filePath).toPath()), driver, selenium, version, folderPath, platform);

        System.out.println("Start processing...");
        modProcessor.processAllMods();
        if(selenium) driver.close();
        System.out.printf("Download finished with %o success and %o errors.", modProcessor.success.get(), modProcessor.error.get());
    }

    public static String getPlatformArg(String... args) {
        String platform = getArg(args , 4, a -> a, () -> "");
        while(!platform.equalsIgnoreCase("Forge") && !platform.equalsIgnoreCase("Fabric")) {
            platform = scanLine("Please enter Forge or Fabric :");
            if(platform.equalsIgnoreCase("Forge")) platform = "Forge";
            if(platform.equalsIgnoreCase("Fabric")) platform = "Fabric";
        }
        return platform;
    }

    public static <T> T getArg(String[] args, int argIndex, Function<String, T> mapper, Supplier<T> orElse) {
        return (args.length > argIndex) ? mapper.apply(args[argIndex]) : orElse.get();
    }

    public static String scanLine(String text) {
        System.out.println(text);
        return scanner.nextLine();
    }

    public static boolean scanBoolean(String text) {
        System.out.println(text);
        return scanner.nextBoolean();
    }
}
