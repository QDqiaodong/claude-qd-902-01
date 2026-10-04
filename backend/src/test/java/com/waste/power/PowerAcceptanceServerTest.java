package com.waste.power;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * 端到端验收服务（仅 -Dpower.acceptance=true 时启动）：
 * H2(MODE=MySQL) + create-drop，灌入验收种子后保持运行，
 * 供 acceptance/power_acceptance.py 打真实 HTTP 接口；
 * 结束后由调用方写入 target/power-acceptance.done 标记，进程退出。
 */
@EnabledIfSystemProperty(named = "power.acceptance", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PowerAcceptanceServerTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void runUntilSentinel() throws Exception {
        File done = new File("target/power-acceptance.done");
        done.delete();
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("seed-power.sql"));
        }
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(8).toMillis();
        while (!done.exists() && System.currentTimeMillis() < deadline) {
            Thread.sleep(500);
        }
        if (done.exists()) {
            Files.deleteIfExists(done.toPath());
        }
    }
}
