package h5.concepts;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import jakarta.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionImpl;
import org.hibernate.internal.scrollable.AbstractScrollableResults;
import org.hibernate.query.Query;

import h5.concepts.support.DemoDatabase;
import h5.concepts.support.Out;

/**
 * Rows 29–37: runtime APIs on an <b>entity-less</b> SessionFactory, the way wmstdappdbimpl uses Hibernate to test
 * user queries and procedures from Studio ({@code DBConnectionUtils}, {@code QueryProcedureTester},
 * {@code DatabaseQueryAndProcedureMetaManagerImpl}). No entities are mapped; everything is native SQL.
 */
public class NativeSqlSessionMain {

    public static void main(String[] args) throws Exception {
        Out.banner("NativeSqlSessionMain — SessionFactory, Session, Transaction, Query, ScrollableResults, JDBC access, exceptions");
        DemoDatabase.ensureCreated();
        try (SessionFactory sessionFactory = createSessionFactory()) {
            queryApi(sessionFactory);
            scrollableResults(sessionFactory);
            transactions(sessionFactory);
            jdbcConnectionAccess(sessionFactory);
            exceptions(sessionFactory);
        }
    }

    /** Row 29 — same shape as DBConnectionUtils.createSessionFactory. */
    static SessionFactory createSessionFactory() {
        Out.row("29", "SessionFactory — Configuration + StandardServiceRegistry, no mapped entities");
        Configuration cfg = new Configuration();
        DemoDatabase.hibernateProperties().forEach((k, v) -> cfg.setProperty((String) k, (String) v));
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
        SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistry);
        Out.kv("sessionFactory", Out.simpleName(sessionFactory));
        Out.kv("isOpen()", sessionFactory.isOpen());
        Out.note("Closing the SessionFactory also destroys this service registry (it owns it).");
        return sessionFactory;
    }

    /** Rows 30, 33 — Session + native Query: parameters, paging, list(), uniqueResult(). */
    static void queryApi(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Out.row("30", "Session — sessionFactory.openSession()");
            Out.kv("session", Out.simpleName(session));
            Out.kv("isOpen() / isConnected()", session.isOpen() + " / " + session.isConnected());

            Out.row("33", "org.hibernate.query.Query — session.createNativeQuery(sql, Object[].class) + named parameter");
            Query<Object[]> query = session.createNativeQuery(
                "SELECT EMP_ID, FIRST_NAME, SALARY FROM EMPLOYEE WHERE SALARY > :minSalary ORDER BY EMP_ID", Object[].class);
            query.setParameter("minSalary", new BigDecimal("8000"));
            Out.kv("query class", Out.simpleName(query));
            List<Object[]> rows = query.list();
            rows.forEach(r -> Out.line("  " + r[0] + "  " + r[1] + "  " + r[2]));
            Out.note("CHANGED in 7: untyped createNativeQuery(String) is deprecated (since 6.0) -> createNativeQuery(sql, Object[].class) "
                + "(or Object.class / a scalar class for one column); the raw Query becomes Query<Object[]>.");

            Out.row("33", "Query paging — setFirstResult(1).setMaxResults(2)");
            List<Object[]> page = session.createNativeQuery("SELECT EMP_ID, FIRST_NAME FROM EMPLOYEE ORDER BY EMP_ID", Object[].class)
                .setFirstResult(1)
                .setMaxResults(2)
                .list();
            page.forEach(r -> Out.line("  " + r[0] + "  " + r[1]));
            Out.note("The dialect's LimitHandler rewrites the SQL for paging (HSQLDB: OFFSET/LIMIT).");

            Out.row("33", "Query.uniqueResult() — count query (QueryProcedureTester runs one before the page)");
            Object count = session.createNativeQuery("SELECT COUNT(*) FROM EMPLOYEE", Object.class).uniqueResult();
            Out.kv("count", count + "  (" + count.getClass().getSimpleName() + ")");
            Out.note("CHANGED in 7: a native COUNT(*) comes back as Long (BigInteger in 5.6; changed in 6.0).");
        }
    }

    /** Rows 33, 34, 35 — query.scroll(), and reading the result columns' JDBC metadata. */
    static void scrollableResults(SessionFactory sessionFactory) throws Exception {
        try (Session session = sessionFactory.openSession()) {
            Out.row("34", "ScrollableResults<Object[]> — query.scroll(FORWARD_ONLY) (cursor; rows are not all loaded into memory)");
            String sql = "SELECT EMP_ID, FIRST_NAME, HIRED_ON FROM EMPLOYEE ORDER BY EMP_ID";
            Query<Object[]> query = session.createNativeQuery(sql, Object[].class);
            try (ScrollableResults<Object[]> scroll = query.scroll(ScrollMode.FORWARD_ONLY)) {
                Out.kv("scroll class", Out.simpleName(scroll));
                Object hiredOn = null;
                while (scroll.next()) {
                    Object[] row = scroll.get(); // typed by the query's result class since 6
                    Out.line("  row " + scroll.getPosition() + ": " + row[0] + " " + row[1] + " " + row[2]);
                    hiredOn = row[2];
                }
                Out.kv("HIRED_ON (DATE) Java type", Out.simpleName(hiredOn));
            }
            Out.note("CHANGED in 7: ScrollableResults is generic (6.0) and only AutoCloseable (7.0); getRowNumber() is deprecated "
                + "for removal (7.0) -> getPosition(), which is 1-based (getRowNumber() was 0-based).");
            Out.note("CHANGED in 7: native DATE columns come back as java.time.LocalDate (java.sql.Date in 5.6; changed in 7.0).");
            Out.note("CHANGED in 7: no-arg query.scroll() uses the dialect's SCROLL_INSENSITIVE mode and throws AssertionFailure "
                + "('scrollable result sets are not enabled') while hibernate.boot.allow_jdbc_metadata_access=false -> pass ScrollMode.FORWARD_ONLY.");

            Out.row("35", "AbstractScrollableResults.getResultSet() — protected, called by reflection");
            try (ScrollableResults<Object[]> metaScroll = query.scroll(ScrollMode.FORWARD_ONLY)) {
                Out.kv("scroll instanceof AbstractScrollableResults", metaScroll instanceof AbstractScrollableResults);
                try {
                    AbstractScrollableResults.class.getDeclaredMethod("getResultSet");
                    Out.kv("AbstractScrollableResults.getResultSet()", "present");
                } catch (NoSuchMethodException e) {
                    Out.kv("AbstractScrollableResults.getResultSet()", "NoSuchMethodException");
                }
            }
            Out.note("REMOVED in 7: AbstractScrollableResults.getResultSet() (removed in 6.0; the class moved to "
                + "org.hibernate.internal.scrollable in 7.0). Supported replacement: run the SQL through "
                + "session.doReturningWork(..) and read the JDBC ResultSetMetaData:");
            int columnCount = session.doReturningWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        Out.kv("column " + i + " " + metaData.getColumnLabel(i),
                            metaData.getColumnTypeName(i) + " (java " + metaData.getColumnClassName(i) + ")");
                    }
                    return metaData.getColumnCount();
                }
            });
            Out.kv("doReturningWork(..) column count", columnCount);
            Out.note("wmstdappdbimpl (QueryProcedureTester) reads the result columns' metadata this way, then "
                + "calls query.list() for the data.");
        }
    }

    /** Rows 32, 33 — Transaction: begin, executeUpdate(), rollback / commit. */
    static void transactions(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Out.row("32", "Transaction — beginTransaction(), executeUpdate(), rollback()");
            Transaction tx = session.beginTransaction();
            Out.kv("tx class / status", Out.simpleName(tx) + " / " + tx.getStatus());
            int updated = session.createNativeMutationQuery("UPDATE EMPLOYEE SET SALARY = SALARY + 1000 WHERE DEPT_ID = :dept")
                .setParameter("dept", 0)
                .executeUpdate();
            Out.kv("executeUpdate() rows", updated);
            Out.kv("Ada's salary inside tx", salaryOfAda(session));
            tx.rollback();
            Out.kv("tx status after rollback()", tx.getStatus());
            Out.kv("Ada's salary after rollback", salaryOfAda(session));

            Out.row("32", "Transaction — commit()");
            tx = session.beginTransaction();
            session.createNativeMutationQuery("INSERT INTO AUDIT_LOG VALUES ('NativeSqlSessionMain ran')").executeUpdate();
            tx.commit();
            Out.kv("tx status after commit()", tx.getStatus());
            Out.kv("AUDIT_LOG rows", session.createNativeQuery("SELECT COUNT(*) FROM AUDIT_LOG", Object.class).uniqueResult());
            Out.note("CHANGED in 7: native DML goes through createNativeMutationQuery(sql) (untyped createNativeQuery deprecated since 6.0).");
        }
    }

    private static Object salaryOfAda(Session session) {
        return session.createNativeQuery("SELECT SALARY FROM EMPLOYEE WHERE FIRST_NAME = 'Ada'", Object.class).uniqueResult();
    }

    /** Rows 30, 31 — getting the JDBC Connection: through SessionImpl internals vs the public session.doWork(..). */
    static void jdbcConnectionAccess(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Out.row("31", "SessionImpl.getJdbcCoordinator()...getPhysicalConnection() — cast to the internal impl to get the JDBC Connection");
            Connection connection = ((SessionImpl) session).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
            try (CallableStatement call = connection.prepareCall("{call EMPLOYEE_COUNT(?, ?)}")) {
                call.setInt(1, 0);
                call.registerOutParameter(2, Types.INTEGER);
                call.execute();
                Out.kv("{call EMPLOYEE_COUNT(0, OUT)}", call.getInt(2));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            Out.note("wmstdappdbimpl (QueryProcedureTester) tests stored procedures through this connection.");
            Out.note("REMOVED in 7: SessionImpl.connection() (removed in 6.0). Closest drop-in is the SPI route above; "
                + "the supported public API is session.doWork(..) / doReturningWork(..) (row 30 below), which real code should move to.");

            Out.row("30", "Session.doWork(connection -> ...) — the public way to reach the JDBC Connection");
            session.doWork(conn -> {
                Out.kv("connection.getMetaData().getDatabaseProductName()", conn.getMetaData().getDatabaseProductName());
                try (CallableStatement call = conn.prepareCall("{call EMPLOYEE_COUNT(?, ?)}")) {
                    call.setInt(1, 1);
                    call.registerOutParameter(2, Types.INTEGER);
                    call.execute();
                    Out.kv("{call EMPLOYEE_COUNT(1, OUT)}", call.getInt(2));
                }
            });
            Integer viaReturningWork = session.doReturningWork(conn -> conn.getTransactionIsolation());
            Out.kv("doReturningWork(getTransactionIsolation)", viaReturningWork);
        }
    }

    /** Rows 36, 37 — the exception hierarchy a failing query surfaces as. */
    static void exceptions(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Out.row("36/37", "Query.list() on a missing table -> the HibernateException (a PersistenceException) thrown directly");
            Out.line("(the WARN/ERROR SqlExceptionHelper lines are Hibernate logging the failure — expected)");
            try {
                session.createNativeQuery("SELECT * FROM NO_SUCH_TABLE", Object[].class).list();
            } catch (PersistenceException e) {
                Out.kv("caught (catch PersistenceException)", e.getClass().getName());
                Out.kv("e instanceof HibernateException", e instanceof HibernateException);
                Out.kv("e.getCause() instanceof HibernateException", e.getCause() instanceof HibernateException);
                printChainAndHierarchy(e);
                Out.note("CHANGED in 7: 5.6 wrapped the SQLGrammarException in a plain PersistenceException; since 6.0 it is "
                    + "thrown directly. 'catch (PersistenceException)' still works and 'catch (HibernateException)' now catches it "
                    + "too, but code that unwraps e.getCause() breaks -> check 'e instanceof JDBCException' instead.");
            }

            Out.row("37", "session.doWork(..) failing -> HibernateException (JDBCException) thrown directly");
            try {
                session.doWork(conn -> conn.createStatement().executeQuery("SELECT * FROM NO_SUCH_TABLE"));
            } catch (HibernateException e) {
                Out.kv("caught (catch HibernateException)", e.getClass().getName());
                Out.kv("e instanceof PersistenceException", e instanceof PersistenceException);
                printChainAndHierarchy(e);
            }
        }
    }

    private static void printChainAndHierarchy(Throwable e) {
        Out.line("cause chain:");
        Throwable firstHibernate = null;
        for (Throwable t = e; t != null; t = t.getCause()) {
            Out.line("  " + t.getClass().getName() + ": " + t.getMessage());
            if (firstHibernate == null && t instanceof HibernateException) {
                firstHibernate = t;
            }
        }
        if (firstHibernate != null) {
            Out.line("class hierarchy of " + firstHibernate.getClass().getSimpleName() + ":");
            for (Class<?> c = firstHibernate.getClass(); c != RuntimeException.class; c = c.getSuperclass()) {
                Out.line("  " + c.getName());
            }
        }
    }
}
