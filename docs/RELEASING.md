# 發佈：簽章 APK 與 F-Droid

這份是給維護者看的。

## 一、簽章 APK

### 金鑰在哪

- 金鑰檔：`D:\Product\LimitShorts-keystore\limitshorts-release.jks`（在專案資料夾**外面**）
- 密碼與別名：專案根目錄的 `keystore.properties`（已列入 `.gitignore`），備份在金鑰檔旁邊的 `keystore.properties.backup`
- **金鑰一旦弄丟，就沒辦法發佈給現有使用者更新**（Android 只讓同一把金鑰簽的 App 互相更新）。請把整個 `LimitShorts-keystore` 資料夾另外備份到安全的地方（例如加密的隨身碟或密碼管理員）。
- **絕對不要把 `.jks` 和 `keystore.properties` 放進公開倉庫。**

### 建置

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\gradlew.bat assembleRelease
```

輸出：`app\build\outputs\apk\release\app-release.apk`。沒有 `keystore.properties` 時照樣能建置，只是不會簽章。

### 發佈前檢查

```powershell
# 簽章
apksigner verify --verbose --print-certs app-release.apk
# 版本與權限（不該有 INTERNET）
aapt2 dump badging app-release.apk
# 雜湊值，放在發佈說明裡讓人驗證
Get-FileHash app-release.apk -Algorithm SHA256
```

### 換版本

改 `app/build.gradle.kts` 的 `versionCode`（每次發佈要加一）和 `versionName`，並在 `fastlane/metadata/android/<語言>/changelogs/` 新增 `<versionCode>.txt`。

## 二、F-Droid

F-Droid 不收別人簽好的 APK，它會**自己從原始碼建置、用自己的金鑰簽**。所以你需要的是：

1. **公開的 git 倉庫**，裡面有這個專案（不含金鑰）。
2. 打上標籤：`git tag v0.1.0`，並推上去。
3. `fdroid/org.limitshorts.app.yml` 裡的倉庫網址已填好（https://github.com/ReoNaBear/LimitShorts）。
4. 到 F-Droid 的 `fdroiddata` 倉庫（gitlab.com/fdroid/fdroiddata），fork 之後把這個檔案放到 `metadata/org.limitshorts.app.yml`，開一個 Merge Request。標題寫 `New app: LimitShorts`。
5. F-Droid 的機器人會試著建置，維護者會看。照他們的意見改。

商店的名稱、說明、截圖都在 `fastlane/metadata/android/`（英文、繁中、日文），F-Droid 會自動讀。

### 這個專案已經符合的條件

- GPL-3.0，全部是自由軟體；沒有 Google Play 服務、沒有追蹤、沒有廣告
- 不宣告 `INTERNET` 權限
- `dependenciesInfo` 已關掉（APK 不夾帶 Google 的相依資訊）
- 沒有簽章金鑰時也能建置

### 還沒確認的事

- F-Droid 的建置機器預設用 JDK 17。我只用 JDK 21 實際建置過，**沒有用 17 試過**。投稿時如果建置失敗，這是第一個要看的地方。
- 無障礙服務類的 App，F-Droid 審核時可能會多問一句用途。商店說明裡已經寫清楚：只看 YouTube、Instagram、Facebook，在手機上讀取畫面標籤，不連網。
