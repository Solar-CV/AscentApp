# tools/shufflecheck —— 展示打乱算法校验

与 App 内 `ui/LessonScreen.kt` 的 `shuffledSingleDisplay` / `shuffledOrderStart` 等价的独立实现：

- `ShuffleCheck.java`：用 **JDK 原生 `java.util.Random`** 复现同一算法（基准）；
- `shuffle_sim.mjs`：Node 等价实现。`--print` 与 Java 输出 diff（交叉校验）；`--stats` 对 `bank/bank.json` 全库统计答案位置分布；
- `uniformity.mjs`：大样本均匀性测试（20 万随机题面）。

用法（本机已有 JDK 17 与 node）：

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk/Contents/Home"; export PATH="$JAVA_HOME/bin:$PATH"
cd ~/AscentApp/tools/shufflecheck
javac ShuffleCheck.java && java ShuffleCheck > /tmp/java_out.txt
node shuffle_sim.mjs --print | diff /tmp/java_out.txt -     # 交叉校验（应无输出）
node shuffle_sim.mjs --stats                                # 全库分布 + 排序题开局检查
node uniformity.mjs                                         # 大样本均匀性
```

**2026-09-30 验证结果**：Java↔Node 逐位一致；25 万样本各位置 ≈ 25.0%；全库非复习单选 A 25.7% / B 26.5% / C 24.6% / D 23.2%（修复前 B 84.2%）；35 道排序题开局即正确 0 道。

> 注意：算法种子含 `prompt.hashCode()`（UTF-16 / Java 语义）与 `java.util.Random` 语义，
> 移植到其他语言时如无法逐位复现，可等价改为「随机打乱」——只是每次展示的题序不同，不影响判定正确性。
