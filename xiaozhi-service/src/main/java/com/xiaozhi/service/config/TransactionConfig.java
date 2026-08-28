package com.xiaozhi.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Configuração do gerenciador de transações
 *
 * @author Joey
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Cria o gerenciador de transações principal
     * Assim, a anotação @Transactional não precisa especificar transactionManager toda vez
     */
    @Primary
    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}