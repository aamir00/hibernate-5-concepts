package h5.concepts.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

/**
 * Rows 19, 20. The collection side uses {@code FetchType.LAZY} and Hibernate's own {@code @Cascade}
 * with {@code SAVE_UPDATE} + {@code REMOVE}, the two cascade types wmstdappdbimpl's code generator
 * ({@code RelationProperty}) writes into generated entities.
 */
@Entity
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @Cascade({CascadeType.SAVE_UPDATE, CascadeType.REMOVE})
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
