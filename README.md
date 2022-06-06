# BuildTools+

这是一个为 [BuildTools](https://www.spigotmc.org/wiki/buildtools/) 制作的包装器，旨在解决中国大陆用户构建 Spigot 服务端 速度过慢甚至失败 的问题。

本包装器本质是 [这个教程](https://www.mcbbs.net/thread-1285303-1-1.html) 的自动化版本。

使用此包装器请准备 Java 环境。另外，对于不同的 Minecraft 服务端，需要不同的 Java 版本来编译。

用法: `java -jar BuildToolsPlus.jar --rev <Minecraft版本> --compile <SPIGOT/CRAFTBUKKIT> --giteeUserName <您的Gitee用户名>`

若有乱码，在 `java` 后面加上 ` -Dfile.encoding=gbk` 即可。

更多参数请执行 `java -jar BuildToolsPlus.jar --help` 查看。

**你可以把这个当成 BuildTools 使用，提供和 BuildTools 一样的*基础*参数(似乎仅允许 `--rev` 和 `--compile`)。**

注意：在使用此包装器前，请先在您的 Gitee 账号上 Clone SpigotMC的一些 Git 仓库。

方法是：

	登录你的 Gitee 账号，打开 https://gitee.com/projects/import/url ，依次导入以下 4 个仓库(你将会需要打开前面那个链接4遍)：
	https://hub.spigotmc.org/stash/scm/spigot/builddata.git
	https://hub.spigotmc.org/stash/scm/spigot/bukkit.git
	https://hub.spigotmc.org/stash/scm/spigot/craftbukkit.git
	https://hub.spigotmc.org/stash/scm/spigot/spigot.git

**注意!** 这四个仓库一定要在拉取时设置为"开源"仓库（所有人可见）。否则在构建时会出现错误。

若过了一段时间仍然想使用此程序，请记得更新您在 Gitee 上的仓库。

有自动化脚本完成此事，是本仓库下的 `update.py` 。

第一次使用自动化脚本前，请先安装 Python 3.8，然后在你的命令行程序 (Windows 一般是 `cmd.exe`，Linux 一般是 `sh`) 下执行如下命令:

    pip install pyWebBrowser

之后，将这两个脚本内的 username = 'XXXXX' 和 password = 'XXXXX' 中的 XXXXX 分别替换为您的Gitee账号和密码以便操作。

您的账号和密码不会被上传，请放心使用。

此程序使用 MIT 许可协议授权。
