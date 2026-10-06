package h5.concepts.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Rows 19, 20. The collection side uses {@code FetchType.LAZY} and cascades {@code PERSIST}, {@code MERGE} and
 * {@code REMOVE}. wmstdappdbimpl's code generator ({@code RelationProperty}) writes Hibernate's
 * {@code @Cascade({SAVE_UPDATE, REMOVE})} into generated entities on 5.x. {@code SAVE_UPDATE} is removed in 7, and
 * {@code org.hibernate.annotations.Cascade} / {@code CascadeType} are deprecated for removal, so 7 uses the JPA
 * {@code cascade} attribute.
 */
@Entity
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY,
        cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private List<Employee> employees = new ArrayList<>();

    protected Department() {
    }

    public Department(String name) {
        this.name = name;
    }

    public Employee addEmployee(String employeeName, int salary) {
        Employee employee = new Employee(employeeName, salary, this);
        employees.add(employee);
        return employee;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Employee> getEmployees() {
        return employees;
    }
}
