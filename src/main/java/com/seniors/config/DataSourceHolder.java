package com.seniors.config;

import com.seniors.common.constant.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class DataSourceHolder {
    private static final ThreadLocal<DatabaseType> DATABASE_TYPE_HOLDER = new ThreadLocal<>();

    public static DatabaseType getDatabaseType() {
        return DATABASE_TYPE_HOLDER.get();
    }

    public static void setDatabaseType(DatabaseType databaseType) {
        DATABASE_TYPE_HOLDER.set(databaseType);
    }

    public static void clearDatabaseType() {
        DATABASE_TYPE_HOLDER.remove();
    }

    public static boolean isNotEmpty() {
        return DATABASE_TYPE_HOLDER.get() != null;
    }
}
