import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

// Сущность клиента с его проектами
@Entity
@Table(name = "Client")
class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int client_id;

    @Column(name = "client_name")
    private String client_name;

    // Связь один-ко-многим с проектами
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Project> projects = new ArrayList<>();

    // Геттеры и сеттеры
    // Получение и установка значений полей
    public int getClient_id() {
        return client_id;
    }

    public void setClient_id(int client_id) {
        this.client_id = client_id;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    // Вспомогательные методы
    public void addProject(Project project) {
        projects.add(project);
        project.setClient(this);
    }
}

// Сущность сотрудника с его задачами и проектами
@Entity
@Table(name = "Employee")
class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int employee_id;

    @Column(name = "employee_name")
    private String employee_name;

    // Связь многие-ко-многим с проектами
    @ManyToMany(mappedBy = "employees", fetch = FetchType.LAZY)
    private List<Project> projects = new ArrayList<>();

    // Связь один-ко-многим с задачами
    @OneToMany(mappedBy = "assignedEmployee", cascade = CascadeType.ALL)
    private List<Task> tasks = new ArrayList<>();

    // Геттеры и сеттеры
    // Получение и установка значений полей
    public int getEmployee_id() {
        return employee_id;
    }

    public void setEmployee_id(int employee_id) {
        this.employee_id = employee_id;
    }

    public String getEmployee_name() {
        return employee_name;
    }

    public void setEmployee_name(String employee_name) {
        this.employee_name = employee_name;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    // Вспомогательные методы
    public void addTask(Task task) {
        tasks.add(task);
        task.setAssignedEmployee(this);
    }
}

// Сущность проекта со связями к клиенту, сотрудникам и задачам
@Entity
@Table(name = "Project")
class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int project_id;

    @Column(name = "project_name")
    private String project_name;

    @Column(name = "project_status")
    private String project_status;

    // Даты начала и окончания проекта
    @Column(name = "project_due_date")
    @Temporal(TemporalType.DATE)
    private Date project_due_date;

    @Column(name = "project_end_date")
    @Temporal(TemporalType.DATE)
    private Date project_end_date;

    // Связь с клиентом
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;

    // Связь со списком сотрудников
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "employee_project",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "employee_id")
    )
    private List<Employee> employees = new ArrayList<>();

    // Связь со списком задач
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    // Геттеры и сеттеры
    // Получение и установка значений полей
    public int getProject_id() {
        return project_id;
    }

    public void setProject_id(int project_id) {
        this.project_id = project_id;
    }

    public String getProject_name() {
        return project_name;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }

    public String getProject_status() {
        return project_status;
    }

    public void setProject_status(String project_status) {
        this.project_status = project_status;
    }

    public Date getProject_due_date() {
        return project_due_date;
    }

    public void setProject_due_date(Date project_due_date) {
        this.project_due_date = project_due_date;
    }

    public Date getProject_end_date() {
        return project_end_date;
    }

    public void setProject_end_date(Date project_end_date) {
        this.project_end_date = project_end_date;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Employee> getEmployees() {
        if (employees == null) {
            employees = new ArrayList<>();
        }
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    public List<Task> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    // Вспомогательные методы для управления связями
    public void addEmployee(Employee employee) {
        if (employee != null) {
            if (employees == null) {
                employees = new ArrayList<>();
            }
            employees.add(employee);
            employee.getProjects().add(this);
        }
    }

    public void removeEmployee(Employee employee) {
        if (employee != null && employees != null) {
            employees.remove(employee);
            employee.getProjects().remove(this);
        }
    }

    public void addTask(Task task) {
        if (task != null) {
            if (tasks == null) {
                tasks = new ArrayList<>();
            }
            tasks.add(task);
            task.setProject(this);
        }
    }

    public void removeTask(Task task) {
        if (task != null && tasks != null) {
            tasks.remove(task);
            task.setProject(null);
        }
    }
}

// Сущность задачи со связями к проекту и сотруднику
@Entity
@Table(name = "Task")
class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int task_id;

    @Temporal(TemporalType.DATE)
    private Date due_date;

    @Column(name = "task_name")
    private String task_name;

    @Column(name = "task_description")
    private String task_description;

    @Column(name = "task_status")
    private String task_status;

    // Связь с проектом
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    // Связь с назначенным сотрудником
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee assignedEmployee;

    // Геттеры и сеттеры
    // Получение и установка значений полей
    public Date getDueDate() {
        return due_date;
    }

    public void setDueDate(Date due_date) {
        this.due_date = due_date;
    }

    public int getTask_id() {
        return task_id;
    }

    public void setTask_id(int task_id) {
        this.task_id = task_id;
    }

    public String getTask_name() {
        return task_name;
    }

    public void setTask_name(String task_name) {
        this.task_name = task_name;
    }

    public String getTask_description() {
        return task_description;
    }

    public void setTask_description(String task_description) {
        this.task_description = task_description;
    }

    public String getTask_status() {
        return task_status;
    }

    public void setTask_status(String task_status) {
        this.task_status = task_status;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Employee getAssignedEmployee() {
        return assignedEmployee;
    }

    public void setAssignedEmployee(Employee assignedEmployee) {
        this.assignedEmployee = assignedEmployee;
    }

    public Date getDue_date() {
        return due_date;
    }

    public void setDue_date(Date due_date) {
        this.due_date = due_date;
    }
}
