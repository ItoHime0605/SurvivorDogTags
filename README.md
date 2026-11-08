# Survivor Dog Tags｜倖存者狗牌

《零號禁區》Minecraft **NeoForge 1.21.1** 模組。整合 Curios 項鍊槽、Exposure 照片及 TaCZ 直接命中傷害。Java 21。

## 初次匯入原始碼（僅需一次）

這個 GitHub 倉庫已設定自動匯入工作流程。建議直接使用 ChatGPT 提供的 **SurvivorDogTags-0.1.9-alpha-src.zip**（保留 ZIP 原樣，不必解壓縮）。

1. 在倉庫首頁選 **Add file → Upload files**。
2. 上傳原始檔名 `SurvivorDogTags-0.1.9-alpha-src.zip` 至倉庫根目錄（不要先解壓縮，也不要更名）。
3. 按 **Commit changes** 提交至 `main`。
4. 進入 **Actions** 查看 **Import source ZIP and build JAR**。
5. 工作流程會自動解壓原始碼、提交 0.1.9-alpha 專案、執行獨立測試、執行 Gradle 編譯。
6. 若成功，從該次 Actions 執行下方 **Artifacts** 下載 **SurvivorDogTags-0.1.9-alpha-JAR**。若失敗，請從 **SurvivorDogTags-compiler-logs** 下載錯誤紀錄。

> 重要：原始碼 ZIP 不是 Minecraft 模組的安裝版，不可直接放入 `mods/`。以 Actions 編譯成功且通過 JAR 驗證為準。

## 模組功能

- 狗牌 Lv.0～Lv.5：專用精工台以礦錠升級
- 玩家身分資訊：角色、名稱、殭屍擊殺數
- Exposure 照片：任何等級皆可更換
- Curios 項鍊配戴：增加最大生命、移動速度
- TaCZ 與近戰物理傷害加成（避免霰彈重複計算）

目前版本：**0.1.9-alpha（原始碼，待 CI 編譯驗證）**。
