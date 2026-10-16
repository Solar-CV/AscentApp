# PORTING-03｜内容素材与发布流程

> 对应对象：`bank/bank.json`（题库 v14）、`app/src/main/assets/bank/img/`（题图）、`knowledge/`（生产端知识库）
> 移植端（iOS / 鸿蒙）**只需要**：一份 bank.json + 11 张题图 + 本文的字段说明。

---

## 1. 内容快照（2026-09-30，题库 v14 / App 0.9.6）

- 47 门课 / 5 个海拔单元（5000 入门 12、5000 进阶 9、6000 米 10、7000 米 8、8000 米 8）
- 展示题 711 条 = 收录题 629 道 + 自动复习副本 82 条（构建期回插，规则见 PORTING-01 §4.4）
- 题型：单选 563 / 多选 61 / 排序 35 / 配对 20 / 填空 14 / 计算 5 / 热点 5 / 情景 8（含 82 条副本；收录题口径见 `REVIEW-711-通读校对报告.md`）
- 情景子题 27 个；题图引用 16 处；目标山峰 30 座；出处标注 540/711
- 海拔：起步 3200 → 第一课 4800 → 第 47 课 8848

## 2. bank.json 结构（schema = 2）

```jsonc
{
  "schema": 2, "version": 14, "updatedAt": "...", "minAppVersion": 6,
  "tracks": [{
    "id": "climb", "title": "高山攀登", "icon": "🏔️",
    "units": [{
      "id": "m5i", "index": 1, "title": "5000 米 · 入门", "altitude": 5000,
      "subtitle": "...", "peaks": [{ "name": "...", "altitude": 5211, "location": "...", "style": "徒步型", "note": "..." }],
      "lessons": [{
        "id": "l1", "title": "高山环境与风险认知", "brief": ["技能要点…"],
        "questions": [
          { "type": "single", "prompt": "...", "options": ["..."], "correct": 1, "explanation": "...", "source": "..." },
          { "type": "order",  "options": ["..."], "correctOrder": [0,1,2,3] },
          { "type": "scenario", "prompt": "情景：…", "context": "…", "steps": [ { "type": "single", "prompt": "第 1 步：…", "options": [], "correct": 0 } ] }
        ]
      }]
    }]
  }]
}
```

要点：
- 解码遵循「宁可少收，不可崩溃」：未知字段忽略、单条格式错误只跳过该条（移植时保持该宽容度）。
- `minAppVersion`：客户端版本低于该值时应拒绝使用（防新字段导致旧端异常）。
- 兼容：顶层若只有 `units`（旧格式），自动包成「高山攀登」轨道。
- ⚠️ Order 题的 `options` 按**正确顺序**书写、`correctOrder=[0,1,2,3]`；展示打乱是**客户端行为**（见 PORTING-01 §4.5）。

## 3. 在线更新协议

- 文件：`manifest.json`（小） + `bank.json`（≈550 KB）。放任意静态服务器/CDN/GitHub raw。
- manifest 实际样例：
```json
{ "version": 14, "updatedAt": "2026-09-30T16:44:54+08:00", "file": "bank.json",
  "sha256": "cb4d5a66…", "minAppVersion": 6 }
```
- 客户端流程：检查（>6h 节流）→ 取 manifest → `version` 比本地新才下载 → 校验（schema/minAppVersion/大小 ≤6MB）→ 落盘缓存。
- 发布新题库（不发版）：改内容 → `BUNDLED_VERSION +1` → `bash tools/contentcheck/run.sh`（同步+校验+导出）→ 上传 `bank.json + manifest.json`。

## 4. 题图清单（`assets/bank/img/`，共 11 张，4.4 MB）

| 文件 | 尺寸 | 类型 | 用途 |
|---|---|---|---|
| wx-manaslu-before.png | 695×1300 | 气象截图（竖） | 零度层热点题（L36.18） |
| wx-manaslu-after.png | 691×1300 | 气象截图 | 气旋/降雪情境（L36.19/20） |
| wx-gongga-1.png | 701×1300 | 气象截图 | 逐小时预报情境（L36.21/22） |
| wx-gongga-2.png | 699×1300 | 气象截图 | 16 天图：雨雪线+雷暴（L36.27/28） |
| wx-mtnforecast.png | 1400×1101 | 专业界面 | 四层高度并列/云底/风向（L36.24-26） |
| topo-manaslu-srtm.jpg | 1500×1283 | 等高线（50 m） | 峰顶热点（L37.19）+ 陡坡判读（L37.20） |
| topo-manaslu-google.jpg | 1500×1050 | 谷歌（200 m） | 地形阴影误导（L37.21） |
| topo-everest-srtm.jpg | 1500×1225 | 等高线 | 峰顶热点 + 鞍部（L37.23/24） |
| topo-muztagh-srtm.jpg | 1500×1290 | 等高线 | 穹状峰顶热点（L37.25） |
| topo-muztagh-google.jpg | 1500×999 | 谷歌 | 地标 vs 等高线（L36.29） |
| demo-slope.png | 800×600 | 程序生成 | 雪崩坡度热点（L29.13） |

**已标定的热点坐标（必须裁剪验证后使用）**：

| 题图 | 目标 | 坐标 | 半径 |
|---|---|---|---|
| wx-manaslu-before | 零度层曲线 | (0.50, 0.496) | 0.12 |
| topo-manaslu-srtm | 马纳斯鲁峰顶 | (0.50, 0.55) | 0.11 |
| topo-everest-srtm | 珠峰峰顶 | (0.45, 0.185) | 0.09 |
| topo-muztagh-srtm | 慕士塔格峰顶 | (0.535, 0.475) | 0.085 |
| demo-slope | 最陡坡面（约 40°） | (0.481, 0.592) | 0.13 |

> 素材来源：`~/技能书/图片资源/`；等高线由 NASA SRTM + QGIS 生成（工程在 `~/技能书/QGIS SAVE/`）。

## 5. 知识库与书源（生产端，不随客户端发布）

- 知识库 `~/AscentApp/knowledge/`：`docs/`（10 份规范化正文，含页码锚点），`topics/`（28 份主题汇编），`tools/kb.mjs`（build/topics/stats/search/page）。
- 书源清单（出题标注格式 `《书名》p.页码`）：
  1. 了解高原病（SEVEN SUMMIT TREKS 向导资料）
  2. 装备指南
  3. 探险活动通用指南
  4. 高山探险
  5. 登山手册
  6. 登山户外安全手册（两册）
  7. 行走在高海拔
  8. 户外领导力
  9. 手册合集
- 内容生产工具链（保持现状即可，各端不需要）：`tools/qbank/build.mjs`（Markdown→BookContent.kt）、`tools/contentcheck/run.sh`（校验+导出）、`gen-demo-image.mjs`（零依赖 PNG 生成）。

## 6. 版本与发布约定

| 编号 | 位置 | 规则 |
|---|---|---|
| `versionName` | `app/build.gradle.kts` | 内容批次走 patch（0.9.5→0.9.6），架构变更才动 minor |
| `versionCode` | 同上 | 每次 +1（当前 18） |
| `BUNDLED_VERSION` | `data/bank/BankConfig.kt` | 与 App 独立编号（当前 14），改内置内容必须 +1 |
| `schema` | bank.json | 结构变更才 +1（当前 2） |
| `minAppVersion` | 导出时写入 | 客户端消费该题库所需的最低 App 版本（当前 6） |

## 7. 移植端自检（消费侧）

- [ ] 能加载 711 条、按单元/课渲染，去重字段正确（复习副本带 `review=true`）
- [ ] 图片按 `Question.image` 命中，缺失时显示「题图缺失」占位而不是崩溃
- [ ] 更新检查：断网/空文件/坏 JSON/更旧版本 → 全部保留现状
- [ ] 语言规范：内容已是简体中文（本版已清理 2 个繁体字与 2 处拼写）
