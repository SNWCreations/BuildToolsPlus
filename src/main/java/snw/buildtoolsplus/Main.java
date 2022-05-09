package snw.buildtoolsplus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import static snw.buildtoolsplus.Util.*;

/**
 * BuildTools+ main class
 * @author SNWCreations
 */
public class Main {
    public static final File CURRENT_DIR = new File(".");
    public static JsonObject GITHUB_MIRROR_DATA;
    public static String TARGETED_MIRROR_NAME;

    public static void main(String[] args) throws Exception {
        // initial information
        String ver = Main.class.getPackage().getImplementationVersion();
        System.out.println("Loading BuildTools+ version " + ver + " by SNWCreations");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Current path: " + new File(".").getAbsolutePath());
        System.out.println();

        OptionParser parser = new OptionParser();
        OptionSpec<Void> help = parser.accepts("help", "Show the help of this program. (Not the help of BuildTools!)");
        OptionSpec<Void> seeMirrors = parser.accepts("see-mirrors", "Use this option to get available Github mirror names.");
        OptionSpec<String> minecraftVersion = parser.accepts("rev", "Version to build" ).withRequiredArg().defaultsTo( "latest" );
        OptionSpec<String> githubMirror = parser.accepts("githubMirror", "The Github mirror name, use '--see-mirrors' argument to know all mirror names.").withOptionalArg().defaultsTo("ghproxy");
        OptionSpec<String> serverJarSource = parser.accepts("serverJarSource", "The source will be used to download vanilla Minecraft server core. Only MOJANG, MCBBS and BMCLAPI are supported.").withOptionalArg().defaultsTo("BMCLAPI");
        OptionSpec<String> compileTarget = parser.accepts("compile", "Server software to build. Only SPIGOT and CRAFTBUKKIT supported.").withOptionalArg().defaultsTo("SPIGOT");
        OptionSpec<String> giteeUserName = parser.accepts("giteeUserName", "The Gitee username we will use to clone the repository.").withRequiredArg();

        OptionSet options = parser.parse(args);

        if (options.has(help)) {
            parser.printHelpOn(System.out);
            return;
        }
        if (options.has(seeMirrors)) {
            System.out.println("Known Github Mirrors: ");
            GITHUB_MIRROR_DATA.keySet().forEach(System.out::println);
            System.out.println();
            System.out.println("If you know other Github mirrors, you can make an issue on my repository to let me know your mirror!");
            return;
        }

        if (!options.has(giteeUserName)) {
            System.err.println("Error: Gitee user name required! Provide the value to argument --giteeUserName!");
            System.exit(1);
        }

        final String serverJarSourceResult = serverJarSource.value(options);

        if (!Arrays.asList("MCBBS", "BMCLAPI", "MOJANG").contains(serverJarSourceResult)) {
            System.err.println("Invalid Server JAR source! Only 'MCBBS', 'BMCLAPI' and 'MOJANG' are supported.");
            System.exit(1);
        }

        if (!Arrays.asList("SPIGOT", "CRAFTBUKKIT").contains(compileTarget.value(options))) {
            System.err.println("Invalid compile target! Only SPIGOT and CRAFTBUKKIT are supported.");
            System.exit(1);
        }

        String giteeUserNameResult = giteeUserName.value(options);
        System.out.println("Gitee username: " + giteeUserNameResult);
        System.out.println();

        System.out.println("Loading Github Mirrors...");
        GITHUB_MIRROR_DATA = JsonParser.parseReader(
                new InputStreamReader(
                        Objects.requireNonNull(Main.class.getResourceAsStream("/githubproxies.json"))
                )
        ).getAsJsonObject();

        TARGETED_MIRROR_NAME = githubMirror.value(options);
        if (!GITHUB_MIRROR_DATA.keySet().contains(TARGETED_MIRROR_NAME)) {
            System.err.println("Invalid Github mirror name! use --see-mirrors option to see mirror names.");
            System.exit(1);
        }

        String minecraftVersionResult = minecraftVersion.value(options);

        if (Objects.equals(minecraftVersionResult, "latest")) {
            minecraftVersionResult = getLatestMinecraftVersion(serverJarSourceResult);
        }



        System.out.println("We will use the Github mirror named " + TARGETED_MIRROR_NAME);
        System.out.println("We will download the vanilla Minecraft server core from " + serverJarSourceResult);
        System.out.println("Attempting to build " + compileTarget.value(options));
        System.out.println();

        if (!new File(CURRENT_DIR, "BuildTools.jar").exists()) {
            System.out.println("Attempting to download BuildTools.");
            String realBuildToolsURL = redirectGithubToMirror("https://raw.githubusercontent.com/SNWCreations/spigotversions/main/BuildTools.jar", GITHUB_MIRROR_DATA.get(TARGETED_MIRROR_NAME).getAsString());

            new FileDownload(realBuildToolsURL, "./BuildTools.jar", null).start();
        } else {
            System.out.println("BuildTools.jar already exists. Skipping.");
        }


        final File workDir = new File(CURRENT_DIR, "work");
        if (!workDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            workDir.mkdir();
        }

        String[] urlAndSha1 = getServerJARUrlAndSha1(minecraftVersionResult, serverJarSourceResult);

        if (urlAndSha1.length != 2) {
            throw new RuntimeException("We cannot find a server core url! Is your version exists?");
        }

        File serverCoreFile = new File("./work/minecraft_server." + minecraftVersionResult + ".jar");
        if (serverCoreFile.exists() && Objects.requireNonNull(getFileDigest(serverCoreFile, "sha-1")).equalsIgnoreCase(urlAndSha1[1])) {
            System.out.println("Found good Minecraft hash! Skipping.");
        } else {
            System.out.println("Attempting to download vanilla Minecraft server core.");
            serverCoreFile.delete(); // wrong file cannot be used
            new FileDownload(urlAndSha1[0], serverCoreFile.getAbsolutePath(), urlAndSha1[1]).start();
        }


        File mavenPackFile = new File("./apache-maven-3.6.0.zip");

        if (mavenPackFile.exists() && Objects.requireNonNull(getFileDigest(mavenPackFile, "sha-1")).equalsIgnoreCase("51819F414A5DA3AAC855BBCA48C68AAFB95AAE81")) {
            System.out.println("Found good Maven hash! Skipping.");
        } else {
            System.out.println("Attempting to download configured Maven.");

            new FileDownload(
                    redirectGithubToMirror("https://raw.githubusercontent.com/SNWCreations/spigotversions/main/apache-maven-3.6.0.zip", GITHUB_MIRROR_DATA.get(TARGETED_MIRROR_NAME).getAsString()),
                    "./apache-maven-3.6.0.zip",
                    "51819F414A5DA3AAC855BBCA48C68AAFB95AAE81"
            ).start();
        }

        if (!new File("./apache-maven-3.6.0").isDirectory()) {
            System.out.println("Attempting to extract downloaded Maven.");
            zipUncompress("./apache-maven-3.6.0.zip", "./apache-maven-3.6.0");
        }



        String gitDir = "PortableGit-2.30.0-" + ((System.getProperty("os.arch").endsWith("64") ? "64" : "32")) + "-bit";
        String gitHash = (
                System.getProperty("os.arch").endsWith("64")
                        ? "373ADFE909902354EA6C39C0B5CAF3DEC07972DD"
                        : "B650383F54DEE64666B97A9F8DCE16ED330D2B2B"
        );

        File gitInstallerFile = new File("./" + gitDir, gitDir + ".7z.exe");

        if (gitInstallerFile.exists() && Objects.requireNonNull(getFileDigest(gitInstallerFile, "sha-1")).equalsIgnoreCase(gitHash)) {
            System.out.println("Git installer exists. Skipping download.");
        } else {
            System.out.println("Downloading PortableGit...");

            File gitDirFile = new File(gitDir);
            if (!gitDirFile.isDirectory()) {
                gitDirFile.mkdirs();
            }

            new FileDownload(
                    "https://ghproxy.com/https://github.com/git-for-windows/git/releases/download/v2.30.0.windows.1/" + gitDir + ".7z.exe",
                    gitInstallerFile.getAbsolutePath(), gitHash
            ).start();
        }

        if (!new File("./" + gitDir, "PortableGit").isDirectory()) {
            System.out.println("Installing Git...");
            Process gitProcess = Runtime.getRuntime().exec("\"" + gitInstallerFile.getAbsolutePath() + "\"" + " -y -gm2 -nr");
            gitProcess.waitFor();
            if (gitProcess.exitValue() != 0) {
                System.err.println("Error occurred while we attempting to install Git!");
                System.exit(1);
            }
        } else {
            System.out.println("Git installation found!");
            System.out.println();
        }


        System.out.println("Attempting to clone Repositories from your Gitee account.");

        try {
            if (notContainsGit(new File(CURRENT_DIR, "Bukkit"))){
                System.out.println("Attempting to clone Bukkit repository.");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/bukkit", "./Bukkit");
            } else {
                System.out.println("Bukkit repository exists. Skipping.");
            }
            if (notContainsGit(new File(CURRENT_DIR, "CraftBukkit"))) {
                System.out.println("Attempting to clone CraftBukkit repository.");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/craftbukkit", "./CraftBukkit");
            } else {
                System.out.println("CraftBukkit repository exists. Skipping.");
            }
            if (notContainsGit(new File(CURRENT_DIR, "Spigot"))){
                System.out.println("Attempting to clone Spigot repository.");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/spigot", "./Spigot");
            } else {
                System.out.println("Spigot repository exists. Skipping.");
            }
            if (notContainsGit(new File(CURRENT_DIR, "BuildData"))){
                System.out.println("Attempting to clone BuildData repository.");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/builddata", "./BuildData");
            } else {
                System.out.println("BuildData repository exists. Skipping.");
            }
        } catch (GitAPIException e) {
            System.err.println("Error occurred while we attempting to clone repository. " + e.getMessage());
            System.err.println("Do you have Internet connection? Is the remote repository exists?");
            System.err.println("Program cannot continue.");
            System.exit(1);
        }

        System.out.println();

        if (!new File(CURRENT_DIR, "svredirector.jar").exists()) {
            System.out.println("Attempting to download svredirector.");
            new FileDownload(
                    "https://ghproxy.com/https://github.com/SNWCreations/svredirector/releases/download/v1.1.0-FINAL/svredirector-1.1.0-FINAL-jar-with-dependencies.jar",
                    "./svredirector.jar", null
            ).start();
        }

        System.out.println("Everything is ready. Can we start BuildTools now?");
        System.out.println("'Y' to yes, 'N' to exit.");
        if (new Scanner(System.in).next().equalsIgnoreCase("n")) {
            System.out.println("Thanks for using BuildTools+!");
            return;
        }

        System.out.println("Ok. Here we go!");
        System.out.println();

        final String javaLibraryPath = System.getProperty("java.library.path");
        String jvmFolder = javaLibraryPath.substring(0, javaLibraryPath.indexOf((System.getProperty("os.name").toLowerCase().startsWith("windows") ? ';' : ':')));

        Process buildTools = Runtime.getRuntime().exec(
                "\"" + new File(jvmFolder, "java.exe").getAbsolutePath() + "\" " +
                        "-javaagent:svredirector.jar -jar BuildTools.jar --rev " + minecraftVersionResult + " --compile " + compileTarget.value(options)
        );
        readProcessOutput(buildTools);
        buildTools.waitFor();
        if (buildTools.exitValue() != 0) {
            System.out.println();
            System.err.println("Emmm.. It seems that BuildTools failed. We can't do anything :(");
            System.err.println();
            System.exit(1);
        }
    }
}
