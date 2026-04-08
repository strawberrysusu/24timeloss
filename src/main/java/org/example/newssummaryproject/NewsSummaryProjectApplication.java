package org.example.newssummaryproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 프로젝트 시작점이다.
 *
 * 현재 실행 흐름은 아래처럼 이해하면 된다.
 * 1. main 메서드가 실행된다.
 * 2. Spring Boot가 application.properties를 읽는다.
 * 3. MySQL 연결(DataSource)과 JPA 설정을 만든다.
 * 4. 엔티티를 스캔해서 필요한 테이블을 생성하거나 업데이트한다.
 * 5. Controller가 HTTP 요청을 받을 준비를 끝내고 서버가 열린다.
 */
@SpringBootApplication
public class NewsSummaryProjectApplication {

    public static void main(String[] args) {
        // .env 파일의 환경변수를 시스템 속성으로 주입하여 Spring Boot가 인식하도록 설정
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(NewsSummaryProjectApplication.class, args);
    }

}
