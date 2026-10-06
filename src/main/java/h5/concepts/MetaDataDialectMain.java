package h5.concepts;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hibernate.JDBCException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MetaDataDialectFactory;
import org.hibernate.cfg.reveng.ReverseEngineeringRuntimeInfo;
import org.hibernate.cfg.reveng.dialect.AbstractMetaDataDialect;
import org.hibernate.cfg.reveng.dialect.JDBCMetaDataDialect;
import org.hibernate.cfg.reveng.dialect.MetaDataDialect;
import org.hibernate.cfg.reveng.dialect.MySQLMetaDataDialect;
import org.hibernate.cfg.reveng.dialect.ResultSetIterator;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.exception.spi.SQLExceptionConverter;

import h5.concepts.support.DemoDatabase;
import h5.concepts.support.DemoMetaDataDialect;
import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;

/**
 * Rows 38, 48–52: Hibernate Tools <b>metadata</b> dialects — the layer that reads JDBC {@code DatabaseMetaData}
 * during reverse engineering (unrelated to the SQL {@code Dialect}). wmstdappdbimpl has one per database
 * ({@code WMGenericMetaDataDialect}, {@code WMMySQLMetaDataDialect}, ...) and picks it with the
 * {@code hibernatetool.metadatadialect} property.
 * <p>
 * Normally Tools drives these during {@code createMetadata()}. Here they are configured by hand with a
 * {@link ReverseEngineeringRuntimeInfo}, so each method can be called and its rows printed.
 */
public class MetaDataDialectMain {

    public static void main(String[] args) throws Exception {
        Out.banner("MetaDataDialectMain — MetaDataDialect, JDBC/MySQL metadata dialects, ResultSetIterator, SQLExceptionConverter");
        DemoDatabase.ensureCreated();
        selection();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySettings(DemoDatabase.hibernateProperties()).build();
        try {
            ConnectionProvider connectionProvider = registry.getService(ConnectionProvider.class);
            SQLExceptionConverter converter = registry.getService(JdbcServices.class)
                .getSqlExceptionHelper().getSqlExceptionConverter();
            ReverseEngineeringRuntimeInfo runtimeInfo =
                ReverseEngineeringRuntimeInfo.createInstance(connectionProvider, converter, null);

            jdbcMetaDataDialect(runtimeInfo);
            customDialect(runtimeInfo);
            mySqlDialect(runtimeInfo);
            resultSetIteratorAndConverter(converter);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    static void selection() {
        Out.row("48", "MetaDataDialect selection — property hibernatetool.metadatadialect, else from the SQL Dialect");
        Properties none = new Properties();
        Out.kv("HSQLDialect, no property", Out.simpleName(MetaDataDialectFactory.createMetaDataDialect(new HSQLDialect(), none)));
        Out.kv("MySQLDialect, no property", Out.simpleName(MetaDataDialectFactory.createMetaDataDialect(new MySQLDialect(), none)));
        Out.kv("OracleDialect, no property", Out.simpleName(MetaDataDialectFactory.createMetaDataDialect(new OracleDialect(), none)));
        Properties withProperty = RevengSupport.properties(DemoMetaDataDialect.class);
        Out.kv("HSQLDialect, property = DemoMetaDataDialect",
            Out.simpleName(MetaDataDialectFactory.createMetaDataDialect(new HSQLDialect(), withProperty)));
    }

    static void jdbcMetaDataDialect(ReverseEngineeringRuntimeInfo runtimeInfo) {
        MetaDataDialect dialect = new JDBCMetaDataDialect();
        dialect.configure(runtimeInfo);
        try {
            Out.row("50", "JDBCMetaDataDialect.getTables(null, PUBLIC, null) — TABLEs and VIEWs");
            print(dialect.getTables(null, "PUBLIC", null), dialect, "TABLE_NAME", "TABLE_TYPE");

            Out.row("50", "getColumns(null, PUBLIC, DEPARTMENT, null)");
            print(dialect.getColumns(null, "PUBLIC", "DEPARTMENT", null), dialect,
                "COLUMN_NAME", "TYPE_NAME", "DATA_TYPE", "COLUMN_SIZE", "DECIMAL_DIGITS", "NULLABLE");

            Out.row("50", "getPrimaryKeys(null, PUBLIC, EMPLOYEE_PROJECT)");
            print(dialect.getPrimaryKeys(null, "PUBLIC", "EMPLOYEE_PROJECT"), dialect, "PK_NAME", "COLUMN_NAME", "KEY_SEQ");

            Out.row("50", "getIndexInfo(null, PUBLIC, EMPLOYEE)");
            print(dialect.getIndexInfo(null, "PUBLIC", "EMPLOYEE"), dialect, "INDEX_NAME", "COLUMN_NAME", "NON_UNIQUE");

            Out.row("50", "getExportedKeys(null, PUBLIC, EMPLOYEE) — FKs that reference EMPLOYEE");
            print(dialect.getExportedKeys(null, "PUBLIC", "EMPLOYEE"), dialect, "FK_NAME", "FKTABLE_NAME", "FKCOLUMN_NAME", "PKCOLUMN_NAME");

            Out.row("49", "AbstractMetaDataDialect defaults: getSuggestedPrimaryKeyStrategyName / needQuote");
            print(dialect.getSuggestedPrimaryKeyStrategyName(null, "PUBLIC", "DEPARTMENT"), dialect,
                "TABLE_NAME", "HIBERNATE_STRATEGY");
            Out.kv("needQuote(\"EMPLOYEE\") / needQuote(\"my table\")",
                dialect.needQuote("EMPLOYEE") + " / " + dialect.needQuote("my table"));
            Out.kv("is an AbstractMetaDataDialect", dialect instanceof AbstractMetaDataDialect);
        } finally {
            dialect.close();
        }
    }

    static void customDialect(ReverseEngineeringRuntimeInfo runtimeInfo) {
        Out.row("49/52", "DemoMetaDataDialect (extends JDBCMetaDataDialect) — overridden getTables, TABLE only");
        DemoMetaDataDialect dialect = new DemoMetaDataDialect();
        dialect.configure(runtimeInfo);
        try {
            print(dialect.getTables(null, "PUBLIC", null), dialect, "TABLE_NAME", "TABLE_TYPE");
            Out.line("The HIGH_EARNERS view is gone. The override uses the inherited protected helpers "
                + "getMetaData(), caseForSearch(..) and getSQLExceptionConverter().");
            Out.kv("needQuote(\"Employee\") (overridden)", dialect.needQuote("Employee"));
        } finally {
            dialect.close();
        }
    }

    static void mySqlDialect(ReverseEngineeringRuntimeInfo runtimeInfo) {
        Out.row("51", "MySQLMetaDataDialect — MySQL-only SQL; run against HSQLDB to show the error path");
        MySQLMetaDataDialect dialect = new MySQLMetaDataDialect();
        dialect.configure(runtimeInfo);
        try {
            Out.kv("superclass", dialect.getClass().getSuperclass().getName());
            Out.line("getTables/getColumns: same as JDBCMetaDataDialect but null patterns become '%' (works on HSQLDB too):");
            print(dialect.getColumns(null, "PUBLIC", "PROJECT", null), dialect, "COLUMN_NAME", "TYPE_NAME");
            Out.line("getSuggestedPrimaryKeyStrategyName runs 'show table status' to find AUTO_INCREMENT -> \"identity\":");
            try {
                dialect.getSuggestedPrimaryKeyStrategyName(null, "PUBLIC", "DEPARTMENT");
            } catch (JDBCException e) {
                Out.kv("thrown", e.getClass().getName());
                Out.kv("message", e.getMessage());
                Out.kv("getSQL()", e.getSQL());
                Out.kv("getSQLException()", e.getSQLException().getMessage());
                Out.note("The JDBC SQLException was turned into a typed JDBCException by the SQLExceptionConverter (row 38).");
            }
        } finally {
            dialect.close();
        }
    }

    static void resultSetIteratorAndConverter(SQLExceptionConverter converter) throws SQLException {
        Out.row("52", "ResultSetIterator — any JDBC ResultSet as Iterator<Map<String,Object>>");
        try (Connection connection = DemoDatabase.openConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT NAME, BUDGET FROM DEPARTMENT ORDER BY NAME");
            ResultSetIterator iterator = new ResultSetIterator(statement, rs, converter) {
                @Override
                protected Map<String, Object> convertRow(ResultSet row) throws SQLException {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", row.getString("NAME"));
                    map.put("budget", row.getBigDecimal("BUDGET"));
                    return map;
                }

                @Override
                protected Throwable handleSQLException(SQLException e) {
                    return converter.convert(e, "reading departments", null);
                }
            };
            iterator.forEachRemaining(m -> Out.line("  " + m));
            iterator.close(); // closes the ResultSet and the Statement
            Out.kv("statement.isClosed() after iterator.close()", statement.isClosed());
        }

        Out.row("38", "SQLExceptionConverter.convert(SQLException, message, sql) — SQLState decides the type");
        Out.kv("converter class", Out.simpleName(converter));
        String[][] samples = {
            {"42501", "object not found"},
            {"23505", "unique constraint violated"},
            {"08001", "cannot connect"},
            {"40001", "serialization failure"},
        };
        for (String[] sample : samples) {
            JDBCException converted = converter.convert(new SQLException(sample[1], sample[0]), "demo", "SELECT 1");
            Out.kv("SQLState " + sample[0] + " (" + sample[1] + ")", converted.getClass().getSimpleName());
        }
    }

    private static void print(Iterator<Map<String, Object>> rows, MetaDataDialect dialect, String... keys) {
        try {
            while (rows.hasNext()) {
                Map<String, Object> row = rows.next();
                StringBuilder sb = new StringBuilder("  ");
                for (String key : keys) {
                    sb.append(key).append('=').append(row.get(key)).append("  ");
                }
                Out.line(sb.toString());
            }
        } finally {
            dialect.close(rows);
        }
    }
}
