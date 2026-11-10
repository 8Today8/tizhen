# tizhen-test-support

测试支撑模块，提供：

- `IntegrationTestBase`：MySQL 8.4 + Redis 7.4 Testcontainers 基类，自动注入数据源与 Redis 配置；无 Docker 时自动跳过。
- Testcontainers、ArchUnit、JUnit 5 依赖，供各模块测试复用。

使用方式：

```java
@SpringBootTest(classes = TizhenApplication.class)
class SomeIntegrationTest extends IntegrationTestBase {
    // ...
}
```

各业务模块的 `pom.xml` 以 `test` 作用域依赖本模块。
