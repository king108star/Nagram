# Nagram：编译 20 账户自用 APK

此 Fork 将普通账户和 Premium 账户的登录上限设为 20，内部 Java/C++ 槽位保留 128。自用工作流只编译 arm64-v8a，包名为 `xyz.nextalone.nagram`。

## 已有签名密钥时更新个人 API

继续使用 VPS 上已有的 `release.keystore`，保留原来的三个签名字段。在 `local.properties` 中补齐个人 Telegram API，完整配置包含五行：

```properties
KEYSTORE_PASS=原来的密钥库密码
ALIAS_NAME=原来的别名
ALIAS_PASS=原来的密钥密码
TELEGRAM_APP_ID=自己的api_id
TELEGRAM_APP_HASH=自己的api_hash
```

API ID 和 Hash 必须来自同一个应用；Hash 是 32 位十六进制字符。API 可从 [Telegram Developer](https://my.telegram.org/auth) 的 API development tools 获取。

在 VPS 上执行：

```bash
umask 077
base64 -w0 local.properties > LOCAL_PROPERTIES.txt
```

用生成文件的完整单行内容更新 GitHub 仓库 Settings → Secrets and variables → Actions 中的 `LOCAL_PROPERTIES` Secret。已有 `KEYSTORE_BASE64` 保持原值；修改 API 无需重新生成签名密钥或密钥库的 Base64。

源码不再提供可用的默认 API，也不再提供使用上游 API 的开关。缺少个人 API 时工作流会在编译前停止。Base64 只是编码，API Hash、密码、密钥库和两个 Secrets 均应私密保存。

## 首次配置签名

仅在从未生成自己的签名密钥时执行一次：

```bash
sudo apt update
sudo apt install -y default-jre-headless
mkdir -p nagram-signing
cd nagram-signing
umask 077
keytool -genkeypair -keystore release.keystore -storetype JKS \
  -alias nagram20 -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Nagram Self 20"
base64 -w0 release.keystore > KEYSTORE_BASE64.txt
```

将 `KEYSTORE_BASE64.txt` 的完整单行内容保存为 `KEYSTORE_BASE64` Secret，并按上一节配置 `LOCAL_PROPERTIES`。备份密钥库和密码，后续更新继续使用同一份。

## 运行与下载

打开 [仓库 Actions](https://github.com/king108star/Nagram/actions)，选择 **Self Build 20 Accounts → Run workflow**，选择需要编译的分支。合入 `main` 的代码更新也会触发 **Release Build**；仓库内部 PR 会使用相同流程检查改动。

工作流递归检出子模块，使用 Java 17、Android SDK 36、NDK 27.2.12479018 和 CMake 3.22.1。原生代码由 Gradle 编译，首次构建可能较慢，后续可复用 ccache。工作流先执行四个账户边界测试，再生成签名 APK，并核对个人 API 配置、签名、包名、版本和原生库架构。

运行成功后，下载 Artifacts 中的 **Nagram-self-20-APKs** 并解压 APK。账户测试结果保存在 **Nagram-self-20-account-tests**。自用流程无需 Telegram 频道发布 Bot Token。

同包名、同签名的后续 APK 可用于覆盖安装自己的旧版本。自己的签名通常不能覆盖官方签名版本；安装前确认手机上现有应用的来源。

## 验证范围

账户边界测试检查普通与 Premium 账户在 19 个已登录账户时可分配新槽位，在 20 个时拒绝分配。编译与静态 APK 检查无法代替手机上的登录、消息收发、通知和通话测试。实际使用 20 个账户会增加内存、电量和流量消耗。

上游编译说明见 [README](https://github.com/NextAlone/Nagram/blob/main/README.md)。
