// 内容校验模块（纯 JVM，不依赖 Android）。
// 用途：把 app 的内容源文件复制进来直接运行，验证题量、复习题插入、答案越界等。
// 与 app 模块完全解耦，不影响正式构建。
plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

application {
    mainClass.set("CheckKt")
}
