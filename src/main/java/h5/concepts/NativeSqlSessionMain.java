package h5.concepts;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
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
import org.hibernate.internal.scrollable.AbstractScrollableResults;
import org.hibernate.internal.SessionImpl;
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
            Out.note("CHANGED in 6: the untyped createNativeQuery(String) is deprecated -> createNativeQuery(sql, Class); "
                + "a native COUNT(*) now comes back as Long (5.x: BigInteger).");
        }
    }

    /** Rows 33, 34, 35 — query.scroll(), and reading the result columns' JDBC metadata. */
    static void scrollableResults(SessionFactory sessionFactory) throws Exception {
        try (Session session = sessionFactory.openSession()) {
            Out.row("34", "ScrollableResults — query.scroll() (cursor; rows are not all loaded into memory)");
            String sql = "SELECT EMP_ID, FIRST_NAME, HIRED_ON FROM EMPLOYEE ORDER BY EMP_ID";
            Query<Object[]> query = session.createNativeQuery(sql, Object[].class);
            ScrollableResults<Object[]> scroll = query.scroll(ScrollMode.FORWARD_ONLY);
            try {
                Out.kv("scroll class", Out.simpleName(scroll));
                while (scroll.next()) {
                    Object[] row = scroll.get(); // typed ScrollableResults<Object[]> in 6 (raw Object[] in 5.x)
                    Out.line("  position " + scroll.getPosition() + ": " + row[0] + " " + row[1] + " " + row[2]);
                }
            } finally {
                scroll.close();
            }
            Out.note("CHANGED in 7: getRowNumber() (0-based) is deprecated for removal -> getPosition() (1-based); "
                + "ScrollableResults is AutoCloseable but no longer Closeable.");

            Out.row("35", "AbstractScrollableResults.getResultSet() — REMOVED in 6");
            try (ScrollableResults<Object[]> metaScroll = query.scroll(ScrollMode.FORWARD_ONLY)) {
                Out.kv("scroll instanceof AbstractScrollableResults", metaScroll instanceof AbstractScrollableResults);
                Out.kv("AbstractScrollableResults declares getResultSet()", declaresMethod(AbstractScrollableResults.class, "getResultSet"));
            }
            Out.note("REMOVED in 6: the protected getResultSet() is gone, so the reflective call wmstdappdbimpl "
                + "(QueryProcedureTester) uses to read the result columns' metadata no longer works. Also, query.scroll() "
                + "without a ScrollMode now throws AssertionFailure while JDBC metadata access is off (FORWARD_ONLY works).");
            Out.kv("AbstractScrollableResults package (moved in 7)", AbstractScrollableResults.class.getPackageName());
            Out.line("Supported replacement — run the SQL on the Session's connection and read ResultSetMetaData:");
            session.doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        Out.kv("column " + i + " " + metaData.getColumnLabel(i),
                            metaData.getColumnTypeName(i) + " (java " + metaData.getColumnClassName(i) + ")");
                    }
                }
            });
        }
    }

    private static boolean declaresMethod(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods()).anyMatch(m -> m.getName().equals(name));
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
            Out.note("CHANGED in 6: native INSERT/UPDATE/DELETE go through createNativeMutationQuery(sql).");
        }
    }

    private static Object salaryOfAda(Session session) {
        return session.createNativeQuery("SELECT SALARY FROM EMPLOYEE WHERE FIRST_NAME = 'Ada'", Object.class).uniqueResult();
    }

    /** Rows 30, 31 — getting the JDBC Connection: SessionImpl internals vs the public session.doWork(..). */
    static void jdbcConnectionAccess(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Out.row("31", "SessionImpl.connection() — REMOVED in 6; closest drop-in: SessionImpl.getJdbcCoordinator()");
            Connection connection = ((SessionImpl) session).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
            try (CallableStatement call = connection.prepareCall("{call EMPLOYEE_COUNT(?, ?)}")) {
                call.setInt(1, 0);
                call.registerOutParameter(2, Types.INTEGER);
                call.execute();
                Out.kv("{call EMPLOYEE_COUNT(0, OUT)}", call.getInt(2));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            Out.note("REMOVED in 6: SessionImpl.connection(). wmstdappdbimpl (QueryProcedureTester) tests stored procedures "
                + "through that connection. getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() still reaches it "
                + "(internal SPI); the supported public API is session.doWork(..) / doReturningWork(..) below.");

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
            Out.row("36/37", "Query.list() on a missing table -> SQLGrammarException (a HibernateException and a PersistenceException)");
            Out.line("(the WARN/ERROR SqlExceptionHelper lines are Hibernate logging the failure — expected)");
            try {
                session.createNativeQuery("SELECT * FROM NO_SUCH_TABLE", Object.class).list();
            } catch (PersistenceException e) {
                Out.kv("caught (catch PersistenceException)", e.getClass().getName());
                Out.kv("e instanceof HibernateException", e instanceof HibernateException);
                Out.kv("e.getCause() instanceof HibernateException", e.getCause() instanceof HibernateException);
                printChainAndHierarchy(e);
                Out.note("CHANGED in 6: the SQLGrammarException is thrown directly. 5.x wrapped it in a plain "
                    + "PersistenceException, so 'catch (HibernateException)' did not catch it there; now it does, and "
                    + "code that unwrapped getCause() sees the JDBC SQLException instead.");
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
