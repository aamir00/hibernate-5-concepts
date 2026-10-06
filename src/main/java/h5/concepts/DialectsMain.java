package h5.concepts;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import h5.concepts.support.Out;

/**
 * Rows 21–28: SQL dialects. wmstdappdbimpl names these per database ({@code *Details.getDialect()}), sets the
 * name as {@code hibernate.dialect}, and uses {@code dialect.openQuote()/closeQuote()} to quote identifiers
 * in generated query code ({@code QueryCodegenEntityBuilder}).
 * <p>
 * Dialects are plain objects, so they are created here with their constructors and need no database
 * connection. In 6 a dialect is version-aware: the no-arg constructor assumes the oldest supported DB version.
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
        Out.kv("DdlTypeRegistry.getTypeName(string, length 50)", ddlTypeName(hsql, StandardBasicTypes.STRING, 50));
        Out.note("CHANGED in 6: supportsSequences()/getSequenceNextValString() moved to dialect.getSequenceSupport(); "
            + "getTypeName(int, long, int, int) was removed -> TypeConfiguration.getDdlTypeRegistry().getTypeName(int, Size, Type), "
            + "which is filled during bootstrap (see ddlTypeName below).");

        Out.row("22–28", "Concrete dialects: openQuote()/closeQuote(), superclass, @Deprecated");
        List<Dialect> dialects = new ArrayList<>();
        dialects.add(new DB2Dialect());                   // 22
        dialects.add(new HANADialect(DatabaseVersion.make(4)));       // 23  was HANACloudColumnStoreDialect
        dialects.add(new HSQLDialect());                  // 24
        dialects.add(new MySQLDialect());                 // 25
        dialects.add(new OracleDialect());                // 26  (deprecated in 5.x, the version-aware dialect in 6)
        dialects.add(new PostgreSQLDialect());            // 27  (deprecated in 5.x, the version-aware dialect in 6)
        dialects.add(new SQLServerDialect(DatabaseVersion.make(11)));  // 28  was SQLServer2012Dialect
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
        Out.note("CHANGED in 6: one version-aware dialect per DB replaces 5.x's versioned classes (Oracle12cDialect, "
            + "PostgreSQL10Dialect, ...); OracleDialect / PostgreSQLDialect are no longer deprecated and now extend Dialect. "
            + "HANACloudColumnStoreDialect is deprecated for removal -> HANADialect(DatabaseVersion.make(4)); "
            + "SQLServer2012Dialect is deprecated -> SQLServerDialect(DatabaseVersion.make(11)).");
    }

    /**
     * The DDL type name for a basic type, the 6.x way. A connection-less registry (dialect given, JDBC metadata
     * access off) is enough for the dialect to contribute its DDL types to the TypeConfiguration.
     */
    static String ddlTypeName(Dialect dialect, BasicTypeReference<?> typeReference, long length) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", dialect.getClass().getName())
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
            .build();
        try {
            TypeConfiguration typeConfiguration = new MetadataSources(registry).buildMetadata().getDatabase().getTypeConfiguration();
            BasicType<?> basicType = typeConfiguration.getBasicTypeRegistry().resolve(typeReference);
            return typeConfiguration.getDdlTypeRegistry()
                .getTypeName(basicType.getJdbcType().getDdlTypeCode(), Size.length(length), basicType);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
