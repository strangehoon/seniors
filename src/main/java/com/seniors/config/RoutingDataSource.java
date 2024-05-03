package com.seniors.config;

import com.seniors.common.constant.DatabaseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@RequiredArgsConstructor
public class RoutingDataSource extends AbstractRoutingDataSource {


    @Override
    protected Object determineCurrentLookupKey() {

        if (DataSourceHolder.isNotEmpty()) {
            DatabaseType databaseType = DataSourceHolder.getDatabaseType();
            log.info("look up dataSoruce ={}", databaseType);
            return databaseType;
        }

        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if (readOnly) {
            log.info("readOnly = true, request to replica");
            return DatabaseType.SLAVE;
        }
        log.info("readOnly = false, request to master");
        return DatabaseType.MASTER;
    }
}
