import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

public class MainTest {

    @Test
    void testAddClient() {
        Client client = new Client();
        client.setClient_id(1);
        client.setClient_name("John Pork");

        assertEquals(1, client.getClient_id());
        assertEquals("John Pork", client.getClient_name());
    }

    @Test
    void testAddEmployee() {
        Employee employee = new Employee();
        employee.setEmployee_id(1);
        employee.setEmployee_name("Alice Johnson");

        assertEquals(1, employee.getEmployee_id());
        assertEquals("Alice Johnson", employee.getEmployee_name());
    }

    @Test
    void testAddProject() {
        Project project = new Project();
        project.setProject_id(1);
        project.setProject_name("Website Redesign");
        project.setProject_status("In Progress");
        Date dueDate = new Date(2024 - 1900, 11, 31); // 31 декабря 2024
        Date endDate = new Date(2025 - 1900, 0, 15);  // 15 января 2025
        project.setProject_due_date(dueDate);
        project.setProject_end_date(endDate);

        assertEquals(1, project.getProject_id());
        assertEquals("Website Redesign", project.getProject_name());
        assertEquals("In Progress", project.getProject_status());
        assertEquals(dueDate, project.getProject_due_date());
        assertEquals(endDate, project.getProject_end_date());
    }

    @Test
    void testAddTask() {
        Task task = new Task();
        task.setTask_id(1);
        task.setTask_name("Create Landing Page");
        task.setTask_description("Develop a responsive design for the landing page.");
        task.setTask_status("Not Started");

        assertEquals(1, task.getTask_id());
        assertEquals("Create Landing Page", task.getTask_name());
        assertEquals("Develop a responsive design for the landing page.", task.getTask_description());
        assertEquals("Not Started", task.getTask_status());
    }

    @Test
    void testExceptionMessage() {
        MyException exception = new MyException("Custom exception message");
        assertEquals("Custom exception message", exception.getMessage());
    }

    @BeforeAll // Выполняется один раз перед всеми тестами
    public static void allTestsStarted() {
        System.out.println("Начало тестирования");
    }

    @AfterAll // Выполняется один раз после всех тестов
    public static void allTestsFinished() {
        System.out.println("Конец тестирования");
    }

    @BeforeEach // Выполняется перед каждым тестом
    public void testStarted() {
        System.out.println("Запуск теста");
    }

    @AfterEach // Выполняется после каждого теста
    public void testFinished() {
        System.out.println("Завершение теста");
    }
}
