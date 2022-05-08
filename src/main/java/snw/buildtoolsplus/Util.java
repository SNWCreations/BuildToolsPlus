package snw.buildtoolsplus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Util {

    private static final int BUFFER_SIZE = 2 * 1024;

    // standard format:
    // USER - username
    // REPO - repository name
    // BRANCH - branch
    // FILE - path to file
    // ORIGINAL_URL - the value of format parameter
    // "format" format: https://raw.githubusercontent.com/{USER}/{REPO}/{BRANCH}/{FILE}
    public static String redirectGithubToMirror(String target, String format) {
        String[] parts = target.split("/");
        String user = parts[3];
        String repo = parts[4];
        String branch = parts[5];
        String file = String.join("/", Arrays.copyOfRange(parts, 6, parts.length));
        return format
                .replace("{ORIGINAL_URL}", target)
                .replace("{USER}", user)
                .replace("{REPO}", repo)
                .replace("{BRANCH}", branch)
                .replace("{FILE}", file);
    }

    public static void zipUncompress(String inputFile, String destDirPath) throws Exception {
        File srcFile = new File(inputFile);//获取当前压缩文件
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw new Exception(srcFile.getPath() + "所指文件不存在");
        }
        ZipFile zipFile = new ZipFile(srcFile);//创建压缩文件对象
        //开始解压
        Enumeration<?> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            // 如果是文件夹，就创建个文件夹
            if (entry.isDirectory()) {
                String dirPath = destDirPath + "/" + entry.getName();
                srcFile.mkdirs();
            } else {
                // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
                File targetFile = new File(destDirPath + "/" + entry.getName());
                // 保证这个文件的父文件夹必须要存在
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }
                targetFile.createNewFile();
                // 将压缩文件内容写入到这个文件中
                InputStream is = zipFile.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(targetFile);
                int len;
                byte[] buf = new byte[1024];
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                // 关流顺序，先打开的后关闭
                fos.close();
                is.close();
            }
        }
    }

    // source - "MOJANG", "BMCLAPI", "MCBBS"
    public static String[] getServerJARUrlAndSha1(String minecraftVersion, String source) throws Exception {
        String mainDomain;

        switch (source) {
            case "BMCLAPI":
                mainDomain = "https://bmclapi2.bangbang93.com";
                break;
            case "MCBBS":
                mainDomain = "https://download.mcbbs.net";
                break;
            default:
                mainDomain = "https://launchermeta.mojang.com";
                break;
        }

        JsonElement element = JsonParser.parseReader(new InputStreamReader(
                new URL(mainDomain + "/mc/game/version_manifest.json").openConnection().getInputStream()
        ));
        for (JsonElement version : element.getAsJsonObject().get("versions").getAsJsonArray()) {
            JsonObject object = version.getAsJsonObject();
            if (Objects.equals(object.get("id").getAsString(), minecraftVersion)) {
                JsonElement versionJson = JsonParser.parseReader(new InputStreamReader(
                        new URL(
                                object.get("url").getAsString()
                        ).openConnection().getInputStream()
                ));
                JsonObject serverInfo = versionJson.getAsJsonObject().get("downloads").getAsJsonObject().get("server").getAsJsonObject();
                return new String[]{serverInfo.get("url").getAsString(), serverInfo.get("sha1").getAsString()};
            }
        }
        return new String[]{};
    }

    // source - "MOJANG", "BMCLAPI", "MCBBS"
    public static String getLatestMinecraftVersion(String source) throws Exception {
        String mainDomain;

        switch (source) {
            case "BMCLAPI":
                mainDomain = "https://bmclapi2.bangbang93.com";
                break;
            case "MCBBS":
                mainDomain = "https://download.mcbbs.net";
                break;
            default:
                mainDomain = "https://launchermeta.mojang.com";
                break;
        }

        JsonElement element = JsonParser.parseReader(new InputStreamReader(
                new URL(mainDomain + "/mc/game/version_manifest.json").openConnection().getInputStream()
        ));
        return element.getAsJsonObject().get("latest").getAsJsonObject().get("release").getAsString();
    }


    public static String getFileDigest(File file, String algorithm) {
        if (!file.isFile()) {
            return null;
        }

        MessageDigest digest;
        FileInputStream in;
        byte[] buffer = new byte[1024];
        int len;

        try {
            digest = MessageDigest.getInstance(algorithm);
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    public static void cloneGitRepo(String remoteUrl, String localPath) throws GitAPIException {
        final File localPathFile = new File(localPath);
        Git.cloneRepository()
                .setURI(remoteUrl)
                .setBranch("master")
                .setDirectory(localPathFile)
                .call();
    }

    public static void deleteDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                //删除文件
                file.delete();
            }
        }
        //删除目录
        dir.delete();
    }

    public static boolean notContainsGit(File file) {
        if (!file.exists()) {
            return true;
        }
        return !(new File(file, ".git")).isDirectory();
    }

    public static void readProcessOutput(final Process process) {
        read(process.getInputStream(), System.out);
        read(process.getErrorStream(), System.err);
    }

    private static void read(InputStream inputStream, PrintStream out) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
