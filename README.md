# fast_use_mod

Minecraft **26.1.2** / Fabric 的客戶端小模組，做兩件事：

1. **挖東西的時候可以放方塊** —— 左鍵右鍵同時按會同時生效。
2. **拿掉使用之間的間隔** —— 原版右鍵之間固定等 4 tick，這裡壓到 0。

只有客戶端需要安裝，伺服器不用。

## 原版擋在哪裡

三個地方，全部在 `net.minecraft.client.Minecraft` 裡：

| 位置 | 原版行為 | 造成的問題 |
| --- | --- | --- |
| `startUseItem()` 開頭 | `if (gameMode.isDestroying()) return;` | 只要在挖方塊，右鍵直接被丟掉 |
| `continueAttack()` 開頭 | `if (missTime > 0 \|\| player.isUsingItem()) return;` | 只要在使用物品，挖掘就停住 |
| `handleKeybinds()` | 要等 `rightClickDelay == 0` 才呼叫 `startUseItem()`，而 `startUseItem()` 每次都把它設回 4 | 右鍵最快 4 tick 一次 |

另外 `MultiPlayerGameMode.continueDestroyBlock()` 開頭有個 `destroyDelay`，秒破的方塊之間會硬等 5 tick。

## 這個模組怎麼處理

`src/main/java/com/sql1024/fastuse/mixin/` 底下兩個 mixin：

- `MinecraftMixin`
  - 用 `@WrapOperation` 讓 `startUseItem()` 裡那個 `isDestroying()` 回報 false → 挖掘中照樣能放。
  - 用 `@WrapOperation` 讓 `continueAttack()` 裡那個 `isUsingItem()` 回報 false → 使用中照樣能挖。
  - 在 `handleKeybinds()` 的 HEAD 把 `rightClickDelay` 夾到設定值（預設 0），順便清掉 `missTime`。
- `MultiPlayerGameModeMixin`
  - 在 `continueDestroyBlock()` 的 HEAD 把 `destroyDelay` 歸零。

## 設定

第一次啟動會產生 `config/fast_use_mod.json`：

```json
{
  "enabled": true,
  "placeWhileMining": true,
  "mineWhileUsing": true,
  "useDelayTicks": 0,
  "removeBreakDelay": true,
  "removeMissDelay": true
}
```

- `enabled` —— 總開關。
- `placeWhileMining` —— 挖掘中允許右鍵。
- `mineWhileUsing` —— 使用物品中允許繼續挖。
- `useDelayTicks` —— 兩次使用之間強制間隔幾個 tick，`0` 就是每 tick 都能用（原版是 4）。
- `removeBreakDelay` —— 拿掉秒破方塊之間的 5 tick 冷卻。
- `removeMissDelay` —— 拿掉揮空之後的 10 tick 懲罰。

遊戲內「選項 → 按鍵設定 → 快速使用」有一個切換鍵，**預設沒有綁定**，自己綁一個就能隨時開關（切換時畫面下方會顯示狀態）。

## 建置

需要 **JDK 25**（26.1.2 本身就是 Java 25 編的）。

```bash
./gradlew build
```

產物在 `build/libs/fast_use_mod-1.0.0.jar`，丟進 `.minecraft/mods/` 即可。開發測試用 `./gradlew runClient`。

依賴版本寫在 `gradle.properties`：Fabric Loader `0.19.3`、Fabric API `0.155.2+26.1.2`。

> 26.x 之後 Minecraft 不再混淆，Yarn 也停在 1.21.11，所以這裡用的是 loom 的 no-remap plugin
> （`net.fabricmc.fabric-loom`，不是舊的 `fabric-loom`），依賴直接用 `implementation` 而不是
> `modImplementation`，也沒有 `mappings` 這一行。

## 注意

伺服器對放置和挖掘有自己的速率檢查。單人和一般伺服器沒問題，但有裝反作弊的伺服器可能會把過快的操作判定成異常，自己斟酌。
