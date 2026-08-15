# fast_use_mod

Fabric 客戶端模組（Minecraft 26.1.2）：**只有在手上拿著終界水晶（End Crystal）時**才會啟用快速使用功能。

## 功能

拿著終界水晶時（主手或副手皆可）：

- 挖掘的同時可以放置／使用物品
- 使用物品的同時可以繼續挖掘
- 移除物品使用之間的延遲（`rightClickDelay`）
- 移除挖掘方塊的延遲（`destroyDelay`）
- 移除揮空的攻擊延遲（`missTime`）

手上沒有終界水晶時，以上行為都跟原版完全一樣。

另外有一個獨立於水晶判斷的功能（拿的是螢石，不是水晶）：

- **重生錨充能上限**：重生錨已經有 1 格充能後，再拿螢石按右鍵不會繼續充能，而是把螢石當方塊放出去
  （等同於原版蹲下右鍵的行為）。充能滿 4 格時不介入，讓原本的引爆／設定重生點照常運作。

## 使用方式

在「選項 → 控制 → 按鍵設定 → 快速使用」裡設定一個開關鍵，按下即可整體開／關（畫面上會顯示「快速使用：開啟／關閉」）。

## 設定檔

`config/fast_use_mod.json`：

| 欄位 | 預設 | 說明 |
| --- | --- | --- |
| `enabled` | `true` | 總開關，對應按鍵切換 |
| `requireEndCrystal` | `true` | 只在手上有終界水晶時生效；設成 `false` 會回到舊版（任何物品都生效）的行為 |
| `placeWhileMining` | `true` | 挖掘時仍可放置／使用 |
| `mineWhileUsing` | `true` | 使用物品時仍可挖掘 |
| `useDelayTicks` | `0` | 使用物品之間保留的延遲 tick 數 |
| `removeBreakDelay` | `true` | 移除破壞方塊延遲 |
| `removeMissDelay` | `true` | 移除揮空延遲 |
| `limitAnchorCharge` | `true` | 重生錨充能到上限後，改成放置螢石 |
| `anchorChargeLimit` | `1` | 重生錨的充能上限格數 |

> `limitAnchorCharge` 需要在送出互動封包前，先傳一個 `shift=true` 的輸入封包給伺服器（用完立刻還原成原本的輸入），
> 因為「充能還是放方塊」是伺服器依照玩家蹲下狀態決定的，純客戶端無法改變。這會讓伺服器看到一瞬間的蹲下。

## 編譯

需要 JDK 25：

```bash
./gradlew build
```

產物在 `build/libs/`。
