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
        System.out.println("正在加载 BuildTools+ , 版本 " + ver + " , 作者 SNWCreations");
        System.out.println("Java 版本: " + System.getProperty("java.version"));
        System.out.println("运行目录 (构建所需数据以及成品均会保存在这): " + new File(".").getAbsolutePath());
        System.out.println();

        OptionParser parser = new OptionParser();
        OptionSpec<Void> help = parser.accepts("help", "显示此程序的帮助并退出");
        OptionSpec<Void> seeMirrors = parser.accepts("see-mirrors", "获取所有已知 Github 镜像的名称并退出");
        OptionSpec<String> minecraftVersion = parser.accepts("rev", "将要构建的服务端的 Minecraft 版本" ).withRequiredArg().defaultsTo( "latest" );
        OptionSpec<String> githubMirror = parser.accepts("githubMirror", "将用于构建的 Github 的镜像名称。").withOptionalArg().defaultsTo("ghproxy");
        OptionSpec<String> serverJarSource = parser.accepts("serverJarSource", "Minecraft 原版服务端的下载源。仅支持 MOJANG, MCBBS 和 BMCLAPI 。").withOptionalArg().defaultsTo("BMCLAPI");
        OptionSpec<String> compileTarget = parser.accepts("compile", "将要构建的服务端软件。 仅支持 SPIGOT 和 CRAFTBUKKIT 。").withOptionalArg().defaultsTo("SPIGOT");
        OptionSpec<String> giteeUserName = parser.accepts("giteeUserName", "存放构建数据的 Gitee 账号名称。").withRequiredArg();

        OptionSet options = parser.parse(args);

        if (options.has(help)) {
            parser.printHelpOn(System.out);
            return;
        }
        if (options.has(seeMirrors)) {
            System.out.println("已知的 Github 镜像名称: ");
            GITHUB_MIRROR_DATA.keySet().forEach(System.out::println);
            System.out.println();
            System.out.println("如果你知道其他的 Github 镜像名称，欢迎在仓库发布 Issue 让我知道！");
            return;
        }

        if (!options.has(giteeUserName)) {
            System.err.println("错误: 需要一个 Gitee 用户名称才能继续。");
            System.exit(1);
        }

        final String serverJarSourceResult = serverJarSource.value(options);

        if (!Arrays.asList("MCBBS", "BMCLAPI", "MOJANG").contains(serverJarSourceResult)) {
            System.err.println("无效的下载源。仅支持 'MCBBS', 'BMCLAPI' 和 'MOJANG' 。注意大小写！");
            System.exit(1);
        }

        if (!Arrays.asList("SPIGOT", "CRAFTBUKKIT").contains(compileTarget.value(options))) {
            System.err.println("无效的构建目标！ 仅支持 SPIGOT 和 CRAFTBUKKIT 。注意大小写！");
            System.exit(1);
        }

        String giteeUserNameResult = giteeUserName.value(options);
        System.out.println("Gitee 账号名称: " + giteeUserNameResult);
        System.out.println();

        System.out.println("正在加载 Github 镜像数据...");
        GITHUB_MIRROR_DATA = JsonParser.parseReader(
                new InputStreamReader(
                        Objects.requireNonNull(Main.class.getResourceAsStream("/githubproxies.json"))
                )
        ).getAsJsonObject();

        TARGETED_MIRROR_NAME = githubMirror.value(options);
        if (!GITHUB_MIRROR_DATA.keySet().contains(TARGETED_MIRROR_NAME)) {
            System.err.println("无效的 Github 镜像名称！");
            System.exit(1);
        }

        String minecraftVersionResult = minecraftVersion.value(options);

        if (Objects.equals(minecraftVersionResult, "latest")) {
            minecraftVersionResult = getLatestMinecraftVersion(serverJarSourceResult);
        }



        System.out.println("Github 镜像名称: " + TARGETED_MIRROR_NAME);
        System.out.println("Minecraft 原版服务端下载源: " + serverJarSourceResult);
        System.out.println("准备构建 " + compileTarget.value(options));
        System.out.println();

        if (!new File(CURRENT_DIR, "BuildTools.jar").exists()) {
            System.out.println("正在下载 BuildTools 。");
            String realBuildToolsURL = redirectGithubToMirror("https://raw.githubusercontent.com/SNWCreations/spigotversions/main/BuildTools.jar", GITHUB_MIRROR_DATA.get(TARGETED_MIRROR_NAME).getAsString());

            new FileDownload(realBuildToolsURL, "./BuildTools.jar", null).start();
        } else {
            System.out.println("找到 BuildTools.jar 。");
        }


        final File workDir = new File(CURRENT_DIR, "work");
        if (!workDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            workDir.mkdir();
        }

        String[] urlAndSha1 = getServerJARUrlAndSha1(minecraftVersionResult, serverJarSourceResult);

        if (urlAndSha1.length != 2) {
            System.err.println("我们找不到所请求的 Minecraft 版本！这个版本存在吗？");
            System.err.println("程序无法继续。");
            System.exit(1);
        }

        File serverCoreFile = new File("./work/minecraft_server." + minecraftVersionResult + ".jar");
        if (serverCoreFile.exists() && Objects.requireNonNull(getFileDigest(serverCoreFile, "sha-1")).equalsIgnoreCase(urlAndSha1[1])) {
            System.out.println("找到有效的 Minecraft 原版服务端文件。");
        } else {
            System.out.println("找不到有效的 Minecraft 原版服务端文件。正在下载。");
            serverCoreFile.delete(); // wrong file cannot be used
            new FileDownload(urlAndSha1[0], serverCoreFile.getAbsolutePath(), urlAndSha1[1]).start();
        }


        File mavenPackFile = new File("./apache-maven-3.6.0.zip");

        if (!new File("./apache-maven-3.6.0").isDirectory()
                && mavenPackFile.exists()
                && Objects.requireNonNull(getFileDigest(mavenPackFile, "sha-1")).equalsIgnoreCase("51819F414A5DA3AAC855BBCA48C68AAFB95AAE81")
        ) {
            System.out.println("找到了有效的 Maven 压缩包！");
        } else {
            System.out.println("正在下载 Maven 。");

            new FileDownload(
                    redirectGithubToMirror("https://raw.githubusercontent.com/SNWCreations/spigotversions/main/apache-maven-3.6.0.zip", GITHUB_MIRROR_DATA.get(TARGETED_MIRROR_NAME).getAsString()),
                    "./apache-maven-3.6.0.zip",
                    "51819F414A5DA3AAC855BBCA48C68AAFB95AAE81"
            ).start();
        }

        if (!new File("./apache-maven-3.6.0").isDirectory()) {
            System.out.println("正在解压 Maven 。");
            zipUncompress("./apache-maven-3.6.0.zip", "./apache-maven-3.6.0");
        }



        String gitDir = "PortableGit-2.30.0-" + ((System.getProperty("os.arch").endsWith("64") ? "64" : "32")) + "-bit";
        String gitHash = (
                System.getProperty("os.arch").endsWith("64")
                        ? "373ADFE909902354EA6C39C0B5CAF3DEC07972DD"
                        : "B650383F54DEE64666B97A9F8DCE16ED330D2B2B"
        );

        File gitInstallerFile = new File("./" + gitDir, gitDir + ".7z.exe");

        if (!new File("./" + gitDir, "PortableGit").isDirectory()
                && gitInstallerFile.exists()
                && Objects.requireNonNull(getFileDigest(gitInstallerFile, "sha-1")).equalsIgnoreCase(gitHash)) {
            System.out.println("找到 Git 安装程序。");
        } else {
            System.out.println("正在下载 Git 。");

            File gitDirFile = new File(gitDir);
            if (!gitDirFile.isDirectory()) {
                gitDirFile.mkdirs();
            }

            new FileDownload(
                    "https://ghproxy.com/https://github.com/git-for-windows/git/releases/download/v2.30.0.windows.1/" + gitDir + ".7z.exe",
                    gitInstallerFile.getAbsolutePath(), gitHash
            ).start();
        }

        if (!new File("./" + gitDir, "PortableGit").isDirectory()
          && Runtime.getRuntime().exec("sh -c exit").exitValue() != 0) {
            System.out.println("正在安装 Git 。");
            Process gitProcess = Runtime.getRuntime().exec("\"" + gitInstallerFile.getAbsolutePath() + "\"" + " -y -gm2 -nr");
            gitProcess.waitFor();
            if (gitProcess.exitValue() != 0) {
                System.err.println("Git 安装失败！");
                System.exit(1);
            }
        } else {
            System.out.println("Git 已经安装。");
            System.out.println();
        }


        System.out.println("正在检查存放构建数据的仓库。");

        try {
            if (notContainsGit(new File(CURRENT_DIR, "Bukkit"))){
                System.out.println("正在克隆 Bukkit 仓库。");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/bukkit", "./Bukkit");
            } else {
                System.out.println("Bukkit 仓库 已存在。跳过。");
            }
            if (notContainsGit(new File(CURRENT_DIR, "CraftBukkit"))) {
                System.out.println("正在克隆 CraftBukkit 仓库。");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/craftbukkit", "./CraftBukkit");
            } else {
                System.out.println("CraftBukkit 仓库 已存在。跳过。");
            }
            if (notContainsGit(new File(CURRENT_DIR, "Spigot"))){
                System.out.println("正在克隆 Spigot 仓库。");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/spigot", "./Spigot");
            } else {
                System.out.println("Spigot 仓库 已存在。跳过。");
            }
            if (notContainsGit(new File(CURRENT_DIR, "BuildData"))){
                System.out.println("正在克隆 BuildData 仓库。");
                cloneGitRepo("https://gitee.com/" + giteeUserNameResult + "/builddata", "./BuildData");
            } else {
                System.out.println("BuildData 仓库 已存在。跳过。");
            }
        } catch (GitAPIException e) {
            System.err.println("克隆仓库仓库时遇到问题。 " + e.getMessage());
            System.err.println("你的网络连接是否正常？提供的 Gitee 账号上是否有请求的仓库？");
            System.err.println("程序无法继续。");
            System.exit(1);
        }

        System.out.println();

        if (!new File(CURRENT_DIR, "svredirector.jar").exists()) {
            System.out.println("正在下载 SVRedirector 。");
            new FileDownload(
                    "https://ghproxy.com/https://github.com/SNWCreations/svredirector/releases/download/v1.1.0-FINAL/svredirector-1.1.0-FINAL-jar-with-dependencies.jar",
                    "./svredirector.jar", null
            ).start();
        }

        System.out.println("一切都准备好了！可以开始了吗？");
        System.out.println("输入 'N' 退出，否则开始。");
        if (new Scanner(System.in).next().equalsIgnoreCase("n")) {
            System.out.println("感谢使用 BuildTools+ ！");
            System.out.println("自行构建的命令格式是: java -javaagent:svredirector.jar -jar BuildTools.jar --rev <Minecraft 版本> --compile <构建目标>");
            return;
        }

        System.out.println("好的，开始吧！");
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
            System.err.println("BuildTools 失败！我们无法做任何事情 :(");
            System.err.println("不同的 Minecraft 版本需要不同的 Java 来构建！这是一个可能的原因。");
            System.err.println();
            System.exit(1);
        }
    }
}
