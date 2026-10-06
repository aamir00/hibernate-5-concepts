package h5.concepts;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANACloudColumnStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServer2012Dialect;

import h5.concepts.support.Out;

/**
 * Rows 21–28: SQL dialects. wmstdappdbimpl names these per database ({@code *Details.getDialect()}), sets the
 * name as {@code hibernate.dialect}, and uses {@code dialect.openQuote()/closeQuote()} to quote identifiers
 * in generated query code ({@code QueryCodegenEntityBuilder}).
 * <p>
 * Dialects are plain objects, so they are created here with their no-arg constructors and need no database
 * connection.
 */
public class DialectsMain {

    public static void main(String[] args) {
        Out.banner("DialectsMain — Dialect + the 7 concrete dialects wmstdappdbimpl names");

        Out.row("21", "org.hibernate.dialect.Dialect — base class; quoting, paging, sequences, type names");
        Dialect hsql = new HSQLDialect();
        Out.kv("quote(\"`Employee`\")", hsql.quote("`Employee`"));
        Out.kv("supportsSequences()", hsql.supportsSequences());
        Out.kv("getSequenceNextValString(\"EMP_SEQ\")", hsql.getSequenceNextValString("EMP_SEQ"));
        Out.kv("getLimitHandler()", Out.simpleName(hsql.getLimitHandler()));
        Out.kv("getTypeName(Types.VARCHAR, 50, 0, 0)", hsql.getTypeName(java.sql.Types.VARCHAR, 50, 0, 0));

        Out.row("22–28", "Concrete dialects: openQuote()/closeQuote(), superclass, @Deprecated");
        List<Dialect> dialects = new ArrayList<>();
        dialects.add(new DB2Dialect());                   // 22
        dialects.add(new HANACloudColumnStoreDialect());  // 23
        dialects.add(new HSQLDialect());                  // 24
        dialects.add(new MySQLDialect());                 // 25
        dialects.add(new OracleDialect());                // 26  (deprecated in 5.x)
        dialects.add(new PostgreSQLDialect());            // 27  (deprecated in 5.x)
        dialects.add(new SQLServer2012Dialect());         // 28
        for (Dialect dialect : dialects) {
            Class<?> type = dialect.getClass();
            System.out.printf("    %-30s quotes=%s%s  extends %-34s deprecated=%-5s identity=%s%n",
                type.getSimpleName(), dialect.openQuote(), dialect.closeQuote(),
                type.getSuperclass().getSimpleName(), type.isAnnotationPresent(Deprecated.class),
                dialect.getIdentityColumnSupport().supportsIdentityColumns());
        }
        Out.line("Quoted identifier, the way QueryCodegenEntityBuilder builds it:");
        for (Dialect dialect : dialects) {
            Out.kv(dialect.getClass().getSimpleName(), dialect.openQuote() + "Employee" + dialect.closeQuote());
        }
        Out.note("In 5.x each DB has versioned dialect classes (Oracle12cDialect, PostgreSQL10Dialect, ...); "
            + "OracleDialect / PostgreSQLDialect are deprecated aliases of old versions.");
    }
}
