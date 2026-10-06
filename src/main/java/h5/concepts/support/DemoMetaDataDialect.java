package h5.concepts.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.tool.internal.reveng.dialect.JDBCMetaDataDialect;
import org.hibernate.tool.internal.reveng.dialect.ResultSetIterator;

/**
 * [Rows 38, 48, 49, 50, 52] A custom Tools metadata dialect, the way wmstdappdbimpl's
 * {@code WMGenericMetaDataDialect} / {@code WMMySQLMetaDataDialect} are built:
 * <ul>
 *   <li>extends {@link JDBCMetaDataDialect} (which extends {@code AbstractMetaDataDialect}),</li>
 *   <li>uses the inherited protected helpers {@code getMetaData()} and {@code caseForSearch()},</li>
 *   <li>returns rows through an anonymous {@link ResultSetIterator}.</li>
 * </ul>
 * Selected by the Tools property {@code hibernatetool.metadatadialect}.
 * <p>
 * Tools 6 removed {@code AbstractMetaDataDialect.getSQLExceptionConverter()} (its own dialects now throw a plain
 * {@code RuntimeException}). To keep turning {@code SQLException}s into typed {@code JDBCException}s, this dialect
 * converts through core's {@link SqlExceptionHelper}.
 */
public class DemoMetaDataDialect extends JDBCMetaDataDialect {

    private static final SqlExceptionHelper SQL_EXCEPTION_HELPER = new SqlExceptionHelper(false);

    /** Counts getTables(..) calls so the demo can prove this dialect was actually used. */
    public static int getTablesCalls;

    @Override
    public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
        getTablesCalls++;
        try {
            String c = caseForSearch(catalog);
            String s = caseForSearch(schema);
            String t = caseForSearch(table);
            // Only real tables; the stock JDBCMetaDataDialect also asks for VIEWs.
            ResultSet tables = getMetaData().getTables(c, s, t, new String[] {"TABLE"});
            return new ResultSetIterator(tables) {

                private final Map<String, Object> row = new HashMap<>();

                @Override
                protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
                    row.clear();
                    row.put("TABLE_NAME", rs.getString("TABLE_NAME"));
                    row.put("TABLE_SCHEM", rs.getString("TABLE_SCHEM"));
                    row.put("TABLE_CAT", rs.getString("TABLE_CAT"));
                    row.put("TABLE_TYPE", rs.getString("TABLE_TYPE"));
                    row.put("REMARKS", rs.getString("REMARKS"));
                    return row;
                }

                @Override
                protected Throwable handleSQLException(SQLException e) {
                    // SQLExceptionConverter (inside SqlExceptionHelper): JDBC SQLException -> typed Hibernate JDBCException
                    return SQL_EXCEPTION_HELPER.convert(e, "Could not get list of tables from database");
                }
            };
        } catch (SQLException e) {
            throw SQL_EXCEPTION_HELPER.convert(e, "Could not get list of tables from database");
        }
    }

    /** Quote anything that is not plain upper-case, like wmstdappdbimpl's {@code BasicMetaDataDialect}. */
    @Override
    public boolean needQuote(String name) {
        return name != null && !name.equals(name.toUpperCase());
    }
}
