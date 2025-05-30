# forge-yniffi

## 发布

1. 设置 core/gradle.properties 中的版本号为。
2. 执行以下命令发布到 Maven Central：

```shell
# clean rust
cd rust && cargo clean && cd ..

./gradlew :core:clean
# ./gradlew :core:publishToMavenLocal --no-configuration-cache
./gradlew :core:publishToMavenCentral --no-configuration-cache
```

## 参考
[rust/yniffi](rust/yniffi) 源于项目 [yswift](https://github.com/y-crdt/yswift)
主项目源于 [uniffi-starter](https://github.com/ianthetechie/uniffi-starter)
