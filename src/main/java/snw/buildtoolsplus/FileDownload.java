package snw.buildtoolsplus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import static snw.buildtoolsplus.Util.getFileDigest;

public class FileDownload {
    private final URL remoteUrl;
    private final String localPath;
    private final String sha1;

    public FileDownload(String remoteUrl, String localPath, String sha1) throws MalformedURLException {
        this.remoteUrl = new URL(remoteUrl);
        this.localPath = localPath;
        this.sha1 = sha1;
    }

    public void start() {
        // 下载网络文件
        int bytesum = 0;
        int byteread;

        try {
            URLConnection conn = remoteUrl.openConnection();
            InputStream inStream = conn.getInputStream();
            @SuppressWarnings("resource")
            FileOutputStream fs = new FileOutputStream(localPath);

            byte[] buffer = new byte[1204];
            while ((byteread = inStream.read(buffer)) != -1) {
                bytesum += byteread;
                fs.write(buffer, 0, byteread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sha1 != null) { // if sha-1 is null, ignore check.
            final String digest = getFileDigest(new File(localPath), "sha-1");
            assert digest != null; // impossible! unless some critical exception happen.
            if (!Objects.equals(digest.toLowerCase(), sha1.toLowerCase())) {
                throw new RuntimeException("The SHA-1 of the requested file on local disk is not equals to provided value. Provided value: " + digest + ", Wanted value: " + sha1);
            }
        }
    }


}
