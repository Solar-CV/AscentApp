#!/bin/bash
# 同步 App 的内容源文件 → 校验 → 导出在线题库（bank.json + manifest.json）
#
# 只复制「纯 Kotlin、不依赖 Android」的文件，因此这个工具可以直接执行真实的内容装配逻辑。
# 用法：bash tools/contentcheck/run.sh
set -e
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/android-toolchain/jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

D=../../app/src/main/java/com/ascent/climb/data
K=src/main/kotlin

# 清理上次同步的内容源（保留 Check.kt / BankExport.kt 这两个工具自己的文件）
rm -f "$K"/Model.kt "$K"/Unit1.kt "$K"/Unit2.kt "$K"/Unit3.kt "$K"/Unit4.kt "$K"/Unit5.kt
rm -f "$K"/Expansion.kt "$K"/Expansion2.kt "$K"/Expansion3.kt "$K"/Expansion4.kt "$K"/Expansion5.kt "$K"/Expansion6.kt "$K"/Expansion7.kt "$K"/Expansion8.kt "$K"/Expansion9.kt "$K"/Expansion10.kt "$K"/Expansion11.kt "$K"/Expansion12.kt "$K"/Expansion13.kt "$K"/Content.kt "$K"/BundledBank.kt "$K"/BookContent.kt "$K"/BankConfig.kt
rm -f "$K"/Curriculum.kt "$K"/NewLessons.kt "$K"/Altitude.kt "$K"/TrekLessons.kt
rm -f "$K"/LessonBuilders.kt "$K"/Expansion14.kt "$K"/Expansion15.kt "$K"/Expansion16.kt "$K"/Expansion17.kt "$K"/Expansion18.kt "$K"/Expansion19.kt "$K"/Expansion20.kt "$K"/Expansion21.kt "$K"/Expansion22.kt "$K"/Expansion23.kt "$K"/Expansion24.kt "$K"/Expansion25.kt "$K"/Expansion26.kt "$K"/Expansion27.kt "$K"/Expansion28.kt "$K"/Expansion29.kt "$K"/Expansion30.kt "$K"/Expansion31.kt "$K"/Expansion32.kt "$K"/Expansion33.kt
rm -f "$K"/TrekExpansion1.kt "$K"/TrekExpansion2.kt "$K"/TrekExpansion3.kt "$K"/HandbookF.kt
rm -f "$K"/HandbookA.kt "$K"/HandbookB.kt "$K"/HandbookC.kt "$K"/HandbookD.kt "$K"/HandbookE.kt
rm -f "$K"/Achievements.kt

cp "$D"/Model.kt "$D"/Unit1.kt "$D"/Unit2.kt "$D"/Unit3.kt "$D"/Unit4.kt "$D"/Unit5.kt "$K"/
cp "$D"/Expansion.kt "$D"/Expansion2.kt "$D"/Expansion3.kt "$D"/Expansion4.kt "$D"/Expansion5.kt "$D"/Expansion6.kt "$D"/Expansion7.kt "$D"/Expansion8.kt "$D"/Expansion9.kt "$D"/Expansion10.kt "$D"/Expansion11.kt "$D"/Expansion12.kt "$D"/Expansion13.kt "$K"/
cp "$D"/HandbookA.kt "$D"/HandbookB.kt "$D"/HandbookC.kt "$D"/HandbookD.kt "$D"/HandbookE.kt "$D"/HandbookF.kt "$K"/
cp "$D"/BundledBank.kt "$D"/BookContent.kt "$D"/Curriculum.kt "$D"/NewLessons.kt "$D"/Altitude.kt "$D"/TrekLessons.kt "$K"/
cp "$D"/LessonBuilders.kt "$D"/Expansion14.kt "$D"/Expansion15.kt "$D"/Expansion16.kt "$D"/Expansion17.kt "$D"/Expansion18.kt "$D"/Expansion19.kt "$D"/Expansion20.kt "$D"/Expansion21.kt "$D"/Expansion22.kt "$D"/Expansion23.kt "$D"/Expansion24.kt "$D"/Expansion25.kt "$D"/Expansion26.kt "$D"/Expansion27.kt "$D"/Expansion28.kt "$D"/Expansion29.kt "$D"/Expansion30.kt "$D"/Expansion31.kt "$D"/Expansion32.kt "$D"/Expansion33.kt "$K"/
cp "$D"/TrekExpansion1.kt "$D"/TrekExpansion2.kt "$D"/TrekExpansion3.kt "$K"/
cp "$D"/Achievements.kt "$K"/
cp "$D"/bank/BankConfig.kt "$K"/

echo "已同步 $(ls "$K" | wc -l | tr -d ' ') 个源文件"
echo

"$HOME/android-toolchain/gradle-8.9/bin/gradle" --console=plain -q run
