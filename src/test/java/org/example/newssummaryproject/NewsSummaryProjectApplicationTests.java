package org.example.newssummaryproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
// 테스트에서는 실제 MySQL 대신 application-test.properties 설정을 사용한다.
@ActiveProfiles("test")
class NewsSummaryProjectApplicationTests {

    @Test
    // 스프링 컨테이너가 정상적으로 뜨는지만 확인하는 가장 기본 테스트
    void contextLoads() {
    }

}
