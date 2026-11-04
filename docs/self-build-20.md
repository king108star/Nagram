# Nagram：用 GitHub Actions 编译最多 20 个账户的自用 APK

适用于 [king108star/Nagram](https://github.com/king108star/Nagram) 的 main 分支。源码中普通账户和 Premium 账户的登录上限均已设为 20，账户数量边界测试也已更新；内部 Java/C++ 槽位保留 128。

仓库已添加 `.github/workflows/self-build-20.yml`，在 Actions 中显示为 **Self Build 20 Accounts**，只需手动触发。它检查源码的 20 个账户上限，在 Actions 的临时工作目录里按上游 README 处理原有签名检查，生成你自己的签名 APK，并将文件保存为 Actions Artifacts。Android 项目的编译全部在 GitHub Actions 上进行。下文的 VPS 只用于生成一次签名密钥和配置文件；有 Java 的电脑也可以完成这一步。

## 手机资源

128 是内部槽位上限，不能据此理解为后台同时运行 128 个账户。当前 ApplicationLoader 主要遍历 SharedConfig.activeAccounts 加载已登录账户。空槽位仍有少量表和扫描开销；内存、电量和流量主要随实际登录账户数量、消息量、媒体操作变化。实际登录 20 个账户会比登录 1–2 个账户增加资源使用，不能仅凭上限保证在某款手机上流畅。

## 1. 获取自己的 Telegram API 信息

在浏览器打开 my.telegram.org，登录自己的 Telegram 账户，进入 API development tools，创建应用。保存 api_id 和 api_hash。平台可以选 Android。不要把 api_hash 发到聊天或提交进公开仓库。

如果暂时申请失败，当前上游 `TMessagesProj/build.gradle` 已包含 API 默认值。工作流提供 `Use the Telegram API settings already included in Nagram source` 可选开关：不填写个人 API 字段且勾选该开关时，Gradle 会使用源码里的现有默认值。开关默认关闭，需要自己明确选择。默认凭据的有效性和实际登录情况尚未验证，不能据此保证 APK 可用。

[Telegram API 条款第 2.1 条](https://core.telegram.org/api/terms#2-transparency)要求为自己的应用取得自己的 API ID。编译配置存在默认值，并不代表获得了上游凭据持有者的授权或 Telegram 的许可；长期使用自己的 API 凭据更合适。自己的 API ID 和 Hash 必须一起填写，不能只填其中一个。

## 2. 在 Debian VPS 上生成签名密钥

下列操作资源用量远小于编译 APK。若以 root 登录，去掉 sudo。

```bash
sudo apt update
sudo apt install -y default-jre-headless nano
mkdir -p nagram-signing
cd nagram-signing
umask 077
keytool -genkeypair -keystore release.keystore -storetype JKS \
  -alias nagram20 -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Nagram Self 20"
```

按提示输入并确认你自己的密钥库密码。使用至少 12 位字母和数字，便于下一步填写 Java properties。若提示单独设置 nagram20 的密钥密码，直接按回车，使用同一个密码。

妥善备份 release.keystore 和密码。后续编译更新继续使用同一份密钥，不要每次重新生成。不要把 release.keystore 上传进公开 Fork。

## 3. 生成两个 Secrets 的内容

仍在 nagram-signing 目录执行：

```bash
nano local.properties
```

使用自己的 API 时，填写以下五行，用你自己的值替换占位文字：

```properties
KEYSTORE_PASS=刚才设置的密钥库密码
ALIAS_NAME=nagram20
ALIAS_PASS=同一个密码
TELEGRAM_APP_ID=你申请到的api_id
TELEGRAM_APP_HASH=你申请到的api_hash
```

暂时尝试源码现有 API 配置时，文件只保留前三行：

```properties
KEYSTORE_PASS=刚才设置的密钥库密码
ALIAS_NAME=nagram20
ALIAS_PASS=同一个密码
```

删除 `TELEGRAM_APP_ID` 和 `TELEGRAM_APP_HASH` 两行，不要填示例数字、`abcdefg` 或占位文字。该方式仍需在启动工作流时勾选上文的开关。以后取得自己的 API 凭据，再补回两行并更新 `LOCAL_PROPERTIES` Secret，开关保持关闭即可。

Ctrl+O 保存，回车确认文件名，Ctrl+X 退出。然后执行：

```bash
base64 -w0 release.keystore > KEYSTORE_BASE64.txt
base64 -w0 local.properties > LOCAL_PROPERTIES.txt
```

两个 txt 的内容用于 GitHub Secrets。Base64 只是编码，按私密信息保管，不能提交到公开仓库。

## 4. 添加 GitHub Actions Secrets

在自己的 Fork 打开 Settings → Secrets and variables → Actions → New repository secret。

| Name | Secret 的内容 |
|---|---|
| KEYSTORE_BASE64 | KEYSTORE_BASE64.txt 的完整单行内容 |
| LOCAL_PROPERTIES | LOCAL_PROPERTIES.txt 的完整单行内容 |

若通过 SSH 查看内容，可分别运行下面两条命令并复制输出：

```bash
cat KEYSTORE_BASE64.txt
```

```bash
cat LOCAL_PROPERTIES.txt
```

不要把这些输出发给他人。不需要配置上游用于向 Telegram 频道发包的 Bot Token。

## 5. 运行和下载

打开 [仓库的 Actions](https://github.com/king108star/Nagram/actions)。如果提示 Fork 的工作流尚未启用，按页面提示启用。选择 Self Build 20 Accounts → Run workflow，选择 main 分支并运行。源码和工作流已提交，无需再上传附件或修改账户数量。

如果 `LOCAL_PROPERTIES` 只有签名的三项，启动时勾选 **Use the Telegram API settings already included in Nagram source**；否则配置检查会停止并提示补齐自己的 API 凭据。

首次需要编译原生库，通常比后续运行慢；不要仅因等待时间长就取消。原生库之后会复用缓存。这里没有承诺具体完成时间。

运行成功后，在同一次运行页面的 Artifacts 下载 Nagram-self-20-APKs，解压获得 arm64-v8a 的 APK。此自用工作流仅编译 64 位 ARM 架构，原生库与 APK 打包均使用 arm64-v8a。工作流修改会在下一次新启动的构建中生效，已在运行的构建继续使用启动时的提交。

若手机已装官方 Nagram，自己的签名 APK 通常不能直接覆盖官方签名的版本。不要为测试贸然卸载已有应用；遇到签名冲突时，再处理独立包名和 Firebase 配置或备份迁移。自用签名 APK 后续使用同一密钥，可以覆盖自己之前安装的版本。

## 验证范围

工作流内嵌 Python 脚本和 Bash 脚本已通过语法检查，API 可选开关的配置校验也已通过本地模拟检查；构建步骤依据当前上游 Gradle 配置和 release.yml 整理。工作流会先运行四个账户数量测试，再生成 APK。尚未执行完整 Actions 构建或在手机上测试；脚本检查通过不代表 APK 已编译成功或默认 API 凭据可登录。

源码与文档来源：

```text
https://github.com/NextAlone/Nagram/blob/main/README.md
https://github.com/NextAlone/Nagram/blob/main/.github/workflows/release.yml
https://github.com/NextAlone/Nagram/blob/main/TMessagesProj/build.gradle
https://github.com/NextAlone/Nagram/blob/main/TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java
https://github.com/NextAlone/Nagram/blob/main/TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java
https://github.com/NextAlone/Nagram/blob/main/TMessagesProj/jni/jni.c
```
