package h5.concepts;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import h5.concepts.support.Out;

/**
 * Rows 21–28: SQL dialects. wmstdappdbimpl names these per database ({@code *Details.getDialect()}), sets the
 * name as {@code hibernate.dialect}, and uses {@code dialect.openQuote()/closeQuote()} to quote identifiers
 * in generated query code ({@code QueryCodegenEntityBuilder}).
 * <p>
 * Dialects are plain objects, so they are created here with their no-arg constructors and need no database
 * connection. Only the DDL type-name lookup needs a (connection-less) boot {@code Metadata} in 7.
 */
public class DialectsMain {

    public static void main(String[] args) {
        Out.banner("DialectsMain — Dialect + the 7 concrete dialects wmstdappdbimpl names");

        Out.row("21", "org.hibernate.dialect.Dialect — base class; quoting, paging, sequences, type names");
        Dialect hsql = new HSQLDialect();
        Out.kv("quote(\"`Employee`\")", hsql.quote("`Employee`"));
        Out.kv("getSequenceSupport().supportsSequences()", hsql.getSequenceSupport().supportsSequences());
        Out.kv("getSequenceSupport().getSequenceNextValString(\"EMP_SEQ\")", hsql.getSequenceSupport().getSequenceNextValString("EMP_SEQ"));
        Out.kv("getLimitHandler()", Out.simpleName(hsql.getLimitHandler()));
        Out.kv("DdlTypeRegistry.getTypeName(Types.VARCHAR, length 50, string)", ddlTypeName(java.sql.Types.VARCHAR, Size.length(50L)));
        Out.note("CHANGED in 7: Dialect.supportsSequences()/getSequenceNextValString(..) -> getSequenceSupport().* (moved in 6.0).");
        Out.note("REMOVED in 7: Dialect.getTypeName(int, long, int, int) (removed in 6.0) -> TypeConfiguration.getDdlTypeRegistry()"
            + ".getTypeName(int, Size, Type). The registry is filled by the dialect during bootstrap, so the type configuration comes "
            + "from a connection-less Metadata (hibernate.dialect set, hibernate.boot.allow_jdbc_metadata_access=false).");

        Out.row("22–28", "Concrete dialects: openQuote()/closeQuote(), superclass, @Deprecated");
        List<Dialect> dialects = new ArrayList<>();
        dialects.add(new DB2Dialect());                   // 22
        dialects.add(new HANADialect());                  // 23  (was HANACloudColumnStoreDialect)
        dialects.add(new HSQLDialect());                  // 24
        dialects.add(new MySQLDialect());                 // 25
        dialects.add(new OracleDialect());                // 26  (deprecated in 5.x, version-aware since 6)
        dialects.add(new PostgreSQLDialect());            // 27  (deprecated in 5.x, version-aware since 6)
        dialects.add(new SQLServerDialect());             // 28  (was SQLServer2012Dialect)
        for (Dialect dialect : dialects) {
            Class<?> type = dialect.getClass();
            System.out.printf("    %-30s quotes=%s%s  extends %-34s deprecated=%-5s identity=%-5s version=%s%n",
                type.getSimpleName(), dialect.openQuote(), dialect.closeQuote(),
                type.getSuperclass().getSimpleName(), type.isAnnotationPresent(Deprecated.class),
                dialect.getIdentityColumnSupport().supportsIdentityColumns(), dialect.getVersion());
        }
        Out.line("Quoted identifier, the way QueryCodegenEntityBuilder builds it:");
        for (Dialect dialect : dialects) {
            Out.kv(dialect.getClass().getSimpleName(), dialect.openQuote() + "Employee" + dialect.closeQuote());
        }
        Out.note("REMOVED in 7: HANACloudColumnStoreDialect -> HANADialect, SQLServer2012Dialect -> SQLServerDialect "
            + "(both deprecated in 6, removed in 7.0; not in hibernate-community-dialects 7 either).");
        Out.note("CHANGED in 7: one version-aware dialect per database (since 6.0). OracleDialect / PostgreSQLDialect are no "
            + "longer deprecated. A no-arg dialect assumes the dialect's minimum supported version (shown as version=); "
            + "with a connection the version is read from the database.");
    }

    /** DDL type name for a JDBC type code, from the type configuration of a connection-less boot Metadata. */
    static String ddlTypeName(int sqlTypeCode, Size size) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", HSQLDialect.class.getName())
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
            .build();
        try {
            TypeConfiguration typeConfiguration = new MetadataSources(registry).buildMetadata().getDatabase().getTypeConfiguration();
            return typeConfiguration.getDdlTypeRegistry().getTypeName(sqlTypeCode, size,
                typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
