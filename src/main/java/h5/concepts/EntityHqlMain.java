package h5.concepts;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.query.Query;
import org.hibernate.stat.Statistics;

import h5.concepts.entities.Department;
import h5.concepts.entities.Employee;
import h5.concepts.support.Out;

/**
 * Rows 19–20 (FetchType, Hibernate CascadeType) shown <b>in action</b> on mapped entities, plus the
 * Session / Transaction / Query APIs (rows 29–34) used with entities and HQL.
 * <p>
 * wmstdappdbimpl itself never runs entities: it only writes {@code FetchType} / {@code @Cascade} into the
 * Java code it generates for user apps ({@code RelationProperty}). This Main shows what those settings do
 * at runtime. It uses its own in-memory database ({@code entity_demo}) with tables created by hbm2ddl.
 * SQL is printed ({@code hibernate.show_sql}) so that lazy loading and cascades are visible.
 */
public class EntityHqlMain {

    public static void main(String[] args) {
        Out.banner("EntityHqlMain — @Entity + HQL: FetchType LAZY/EAGER, @Cascade SAVE_UPDATE/REMOVE");
        try (SessionFactory sessionFactory = createSessionFactory()) {
            Integer engineeringId = cascadeSaveUpdate(sessionFactory);
            cascadeOnPersistFlush(sessionFactory);
            fetchTypes(sessionFactory, engineeringId);
            hql(sessionFactory);
            cascadeRemove(sessionFactory, engineeringId);
        }
    }

    static SessionFactory createSessionFactory() {
        Out.row("1/29", "Configuration.addAnnotatedClass(..) -> SessionFactory with entities");
        Configuration cfg = new Configuration()
            .addAnnotatedClass(Department.class)
            .addAnnotatedClass(Employee.class)
            .setProperty("hibernate.dialect", HSQLDialect.class.getName())
            .setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver")
            .setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:entity_demo")
            .setProperty("hibernate.connection.username", "SA")
            .setProperty("hibernate.connection.password", "")
            .setProperty("hibernate.hbm2ddl.auto", "create-drop")
            .setProperty("hibernate.show_sql", "true")
            .setProperty("hibernate.generate_statistics", "true");
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
        SessionFactory sessionFactory = cfg.buildSessionFactory(registry);
        Out.kv("mapped entities", sessionFactory.getMetamodel().getEntities().size());
        return sessionFactory;
    }

    /** Row 20: SAVE_UPDATE — saveOrUpdate(department) also inserts the new employees in its collection. */
    static Integer cascadeSaveUpdate(SessionFactory sessionFactory) {
        Out.row("20", "@Cascade(SAVE_UPDATE) — session.saveOrUpdate(dept) cascades to new employees");
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Department engineering = new Department("Engineering");
            engineering.addEmployee("Ada", 9100);
            engineering.addEmployee("Alan", 9500);
            engineering.addEmployee("Grace", 8700);
            session.saveOrUpdate(engineering);
            tx.commit();
            Out.kv("employees saved through the cascade", countEmployees(session));
            return engineering.getId();
        }
    }

    /** Row 20: with the native bootstrap, SAVE_UPDATE is also applied when a persist()-ed entity is flushed. */
    static void cascadeOnPersistFlush(SessionFactory sessionFactory) {
        Out.row("20", "@Cascade(SAVE_UPDATE) + session.persist(dept) — cascade happens at flush");
        try (Session session = sessionFactory.openSession()) {
            long before = countEmployees(session);
            Transaction tx = session.beginTransaction();
            Department sales = new Department("Sales");
            sales.addEmployee("Linus", 7000);
            session.persist(sales);
            tx.commit();
            Out.kv("employees before / after persist(sales)", before + " / " + countEmployees(session));
            Out.note("The new employee was inserted too: with the native Configuration bootstrap, Hibernate 5's "
                + "flush-time cascade honours SAVE_UPDATE even for persist(). (SAVE_UPDATE is deprecated in 6 and removed in 7.)");
        }
    }

    /** Row 19: LAZY collection vs EAGER to-one. */
    static void fetchTypes(SessionFactory sessionFactory, Integer departmentId) {
        Statistics stats = sessionFactory.getStatistics();

        Out.row("19", "FetchType.LAZY — Department.employees is loaded only when first used");
        try (Session session = sessionFactory.openSession()) {
            stats.clear();
            Department department = session.get(Department.class, departmentId);
            Out.kv("Hibernate.isInitialized(employees)", Hibernate.isInitialized(department.getEmployees()));
            Out.kv("SQL statements so far", stats.getPrepareStatementCount());
            Out.line("calling department.getEmployees().size() ...");
            int size = department.getEmployees().size();
            Out.kv("size / isInitialized", size + " / " + Hibernate.isInitialized(department.getEmployees()));
            Out.kv("SQL statements so far", stats.getPrepareStatementCount());
        }

        Out.row("19", "FetchType.EAGER — Employee.department is loaded together with the employee");
        try (Session session = sessionFactory.openSession()) {
            stats.clear();
            Employee employee = session.createQuery("from Employee where name = :name", Employee.class)
                .setParameter("name", "Ada")
                .uniqueResult();
            Out.kv("Hibernate.isInitialized(department)", Hibernate.isInitialized(employee.getDepartment()));
            Out.kv("department name (no extra SQL)", employee.getDepartment().getName());
            Out.kv("SQL statements", stats.getPrepareStatementCount());
            Out.note("HQL loads the EAGER to-one right away with a second SELECT.");
        }
        try (Session session = sessionFactory.openSession()) {
            stats.clear();
            Employee employee = session.get(Employee.class, 1);
            Out.kv("session.get(Employee, 1): isInitialized(department)", Hibernate.isInitialized(employee.getDepartment()));
            Out.kv("SQL statements", stats.getPrepareStatementCount());
            Out.note("session.get() loads the EAGER to-one in the same SELECT via an outer join.");
        }
    }

    /** Rows 30, 33: HQL with typed results, projections, join fetch, paging and bulk update. */
    static void hql(SessionFactory sessionFactory) {
        try (Session session = sessionFactory.openSession()) {
            Out.row("33", "Typed HQL — session.createQuery(hql, Employee.class).list()");
            Query<Employee> query = session.createQuery(
                "from Employee e where e.salary > :min order by e.salary desc", Employee.class);
            query.setParameter("min", 8000);
            query.list().forEach(e -> Out.line("  " + e));

            Out.row("33", "HQL projection + paging — select e.name, e.salary ... setMaxResults(2)");
            List<Object[]> top = session.createQuery(
                    "select e.name, e.salary from Employee e order by e.salary desc", Object[].class)
                .setMaxResults(2)
                .list();
            top.forEach(r -> Out.line("  " + r[0] + "  " + r[1]));

            Out.row("19/33", "join fetch — load departments + their LAZY employees in one query");
            List<Department> departments = session.createQuery(
                    "select distinct d from Department d left join fetch d.employees order by d.name", Department.class)
                .list();
            departments.forEach(d -> Out.line("  " + d.getName() + " -> initialized="
                + Hibernate.isInitialized(d.getEmployees()) + " " + d.getEmployees()));

            Out.row("32/33", "HQL bulk update — executeUpdate() inside a transaction, then rollback");
            Transaction tx = session.beginTransaction();
            int updated = session.createQuery("update Employee e set e.salary = e.salary + 100").executeUpdate();
            Out.kv("executeUpdate() rows", updated);
            tx.rollback();
        }
    }

    /** Row 20: REMOVE — deleting the department deletes its employees. */
    static void cascadeRemove(SessionFactory sessionFactory, Integer departmentId) {
        Out.row("20", "@Cascade(REMOVE) — session.delete(dept) also deletes its employees");
        try (Session session = sessionFactory.openSession()) {
            long before = countEmployees(session);
            Transaction tx = session.beginTransaction();
            session.delete(session.get(Department.class, departmentId));
            tx.commit();
            Out.kv("employees before / after delete", before + " / " + countEmployees(session));
        }
    }

    private static long countEmployees(Session session) {
        return session.createQuery("select count(e) from Employee e", Long.class).uniqueResult();
    }
}
