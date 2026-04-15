package org.example.newssummaryproject.global.config;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4.x에서 Flyway auto-configuration이 제거됐으므로 수동으로 등록한다.
 * JPA EntityManagerFactory가 schema validation을 하기 전에 Flyway가 먼저 실행돼야 하므로
 * BeanDefinitionRegistryPostProcessor로 실행 순서를 보장한다.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @Bean
    public static BeanDefinitionRegistryPostProcessor flywayDependencyPostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                for (String beanName : new String[]{"entityManagerFactory", "jpaSharedEM_entityManagerFactory"}) {
                    if (registry.containsBeanDefinition(beanName)) {
                        BeanDefinition def = registry.getBeanDefinition(beanName);
                        List<String> deps = new ArrayList<>();
                        String[] existing = def.getDependsOn();
                        if (existing != null) deps.addAll(Arrays.asList(existing));
                        if (!deps.contains("flyway")) {
                            deps.add("flyway");
                            def.setDependsOn(deps.toArray(String[]::new));
                        }
                    }
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            }
        };
    }
}
