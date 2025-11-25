// Импорты для работы с базой данных
import javax.persistence.*;
// Импорты для GUI компонентов
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
// Импорты для работы с датами и форматированием
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
// Импорты для работы с XML
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.util.HashMap;
// Импорты для генерации отчетов
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.apache.log4j.Logger;

// Основной класс пользовательского интерфейса для управления проектами
public class UI extends JFrame {
    // Логгер для отслеживания действий пользователя
    private static final Logger logger = Logger.getLogger(UI.class);
    // Основные компоненты интерфейса
    private JTabbedPane tabbedPane;
    private JTable clientTable;
    private JTable employeeTable;
    private JTable taskTable;
    private JTable projectTable;

    // Конструктор главного окна приложения
    public UI() {
        logger.info("Запуск приложения управления проектами");
        setTitle("Управление проектами");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Создаем панель с вкладками
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Проекты", createProjectsPanel());
        tabbedPane.addTab("Задачи", createTasksPanel());
        tabbedPane.addTab("Сотрудники", createEmployeesPanel());
        tabbedPane.addTab("Клиенты", createClientsPanel());

        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            String tabName = tabbedPane.getTitleAt(selectedIndex);
            logger.info("Пользователь перешел на вкладку: " + tabName);
        });

        // Кнопка обновления
        JButton refreshAllButton = new JButton("Обновить все таблицы");
        refreshAllButton.addActionListener(e -> {
            try {
                refreshAllTables();
            } catch (MyException ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        // Кнопка сохранения XML
        JButton saveAllToXMLButton = new JButton("Сохранить данные в XML");
        saveAllToXMLButton.addActionListener(e -> {
            try {
                saveAllToXML();
            } catch (Exception ex) {
                showErrorMessage(ex.getMessage());
            }
        });
        // Кнопка загрузки XML
        JButton loadFromXMLButton = new JButton("Загрузить данные из XML");
        loadFromXMLButton.addActionListener(e -> {
            try {
                loadFromXML();
            } catch (Exception ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        // панель для размещения кнопок
        JPanel topButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topButtonPanel.add(refreshAllButton);
        topButtonPanel.add(saveAllToXMLButton);
        topButtonPanel.add(loadFromXMLButton);

        // панель для размещения кнопки обновления
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(topButtonPanel, BorderLayout.NORTH);

        add(mainPanel);
    }

    // Отображение главного окна приложения
    public void createAndShowGUI() {
        setVisible(true);
    }

    // Панель клиентов
    private JPanel createClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        clientTable = new JTable();
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Установка слушателя для кликов мыши
        clientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = clientTable.getSelectedRow();
                if (selectedRow != -1) {
                    if (e.getClickCount() == 1) {
                        // Одинарный клик выбирает клиента
                        clientTable.setRowSelectionInterval(selectedRow, selectedRow);
                    } else if (e.getClickCount() == 2) {
                        // Двойной клик открывает проекты клиента
                        try {
                            showClientProjects((Integer) clientTable.getValueAt(selectedRow, 0));
                        } catch (MyException ex) {
                            showErrorMessage(ex.getMessage());
                        }
                    }
                }
            }
        });

        // Запрещаем редактирование ячеек таблицы
        clientTable.setDefaultEditor(Object.class, null);
        try {
            updateClientTable();
        } catch (MyException ex) {
            showErrorMessage(ex.getMessage());
        }

        // Добавляем панель поиска сверху
        panel.add(createSearchPanel(clientTable, "Поиск по имени клиента..."), BorderLayout.NORTH);
        panel.add(new JScrollPane(clientTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton addClientButton = new JButton("Добавить");
        addClientButton.addActionListener(e -> {
            try {
                addNewClient();
            } catch (MyException ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        // Кнопка для изменения клиента
        JButton editClientButton = new JButton("Изменить");
        editClientButton.addActionListener(e -> {
            int selectedRow = clientTable.getSelectedRow();
            if (selectedRow != -1) {
                try {
                    editClient((Integer)clientTable.getValueAt(selectedRow, 0));
                } catch (MyException ex) {
                    showErrorMessage(ex.getMessage());
                }
            } else {
                showErrorMessage("Выберите клиента для изменения");
            }
        });

        // Кнопка для удаления клиента
        JButton deleteClientButton = new JButton("Удалить");
        deleteClientButton.addActionListener(e -> {
            int selectedRow = clientTable.getSelectedRow();
            if (selectedRow != -1) {
                int clientId = (Integer)clientTable.getValueAt(selectedRow, 0);
                String clientName = (String)clientTable.getValueAt(selectedRow, 1);
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Вы действительно хотите удалить клиента \"" + clientName + "\"?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        deleteClient(clientId);
                    } catch (MyException ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
            } else {
                showErrorMessage("Выберите клиента для удаления");
            }
        });

        // Кнопка для создания PDF-отчета
        JButton generatePdfButton = new JButton("Сохранить в PDF");
        generatePdfButton.addActionListener(e -> {
            try {
                saveAllToXML(); // Сначала сохраняем в XML
                generatePdfReportClients();
            } catch (Exception ex) {
                showErrorMessage("Ошибка при генерации отчета: " + ex.getMessage());
            }
        });


        Dimension buttonSize = new Dimension(150, 35);
        addClientButton.setPreferredSize(buttonSize);
        editClientButton.setPreferredSize(buttonSize);
        deleteClientButton.setPreferredSize(buttonSize);
        generatePdfButton.setPreferredSize(buttonSize);

        buttonPanel.add(addClientButton);
        buttonPanel.add(editClientButton);
        buttonPanel.add(deleteClientButton);
        buttonPanel.add(generatePdfButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
        
    }
    
    //  окно с доп информацией о клиенте
    private void showClientProjects(int clientId) throws MyException {
        logger.info("Загрузка проектов клиента с ID: " + clientId);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw new MyException("Клиент не найден");
            }

            List<Project> projects = em.createQuery("SELECT p FROM Project p WHERE p.client.id = :clientId", Project.class)
                    .setParameter("clientId", clientId)
                    .getResultList();

            if (projects.isEmpty()) {
                JOptionPane.showMessageDialog(null, "У клиента нет проектов", "Информация", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            StringBuilder projectNames = new StringBuilder("Проекты клиента " + client.getClient_name() + ":\n");
            for (Project project : projects) {
                projectNames.append("- ").append(project.getProject_name()).append("\n");
            }

            JOptionPane.showMessageDialog(null, projectNames.toString(), "Проекты клиента", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            throw new MyException("Ошибка при загрузке проектов клиента: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }
    // Обновление таблицы клиентов
    private void updateClientTable() throws MyException {
        logger.info("Обновление таблицы клиентов");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            List<Client> clients = em.createQuery("SELECT c FROM Client c", Client.class).getResultList();
            String[] columnNames = {"ID", "Имя клиента"};
            Object[][] data = new Object[clients.size()][2];
            for (int i = 0; i < clients.size(); i++) {
                Client client = clients.get(i);
                data[i][0] = client.getClient_id();
                data[i][1] = client.getClient_name();
            }
            clientTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при обновлении таблицы клиентов: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Валидация имени
    private void validateName(String name) throws MyException {
        if (name == null || name.trim().isEmpty()) {
            throw new MyException("Имя не должно быть пустым");
        }
        if (!name.matches("[a-zA-Zа-яА-ЯёЁ ]+")) {
            throw new MyException("Имя может содержать только буквы");
        }
    }
    // Добавление нового клиента
    private void addNewClient() throws MyException {
        logger.info("Пользователь открыл форму создания нового клиента");
        JTextField clientNameField = new JTextField();

        Object[] fields = {"Имя клиента:", clientNameField};
        int result = JOptionPane.showConfirmDialog(null, fields, "Добавить нового клиента", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String clientName = clientNameField.getText();
            validateName(clientName); // Валидация имени клиента

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
            EntityManager em = emf.createEntityManager();

            try {
                em.getTransaction().begin();
                Client newClient = new Client();
                newClient.setClient_name(clientName);
                em.persist(newClient);
                em.getTransaction().commit();
                updateClientTable();
                logger.info("Создан новый клиент: " + clientName);
            } catch (MyException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw new MyException("Ошибка при добавлении клиента: " + e.getMessage());
            } finally {
                em.close();
                emf.close();
            }
        }
    }
    // Изменение клиента
    private void editClient(int clientId) throws MyException {
        logger.info("Пользователь открыл форму редактирования клиента");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw new MyException("Клиент не найден");
            }

            JTextField clientNameField = new JTextField(client.getClient_name());
            Object[] fields = {"Имя клиента:", clientNameField};

            int result = JOptionPane.showConfirmDialog(null, fields, "Изменить клиента",
                JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                String newName = clientNameField.getText();
                validateName(newName);

                em.getTransaction().begin();
                client.setClient_name(newName);
                em.getTransaction().commit();
                updateClientTable();
                logger.info("Клиент успешно отредактирован: " + newName);
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при изменении клиента: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }
    // Удаление клиента
    private void deleteClient(int clientId) throws MyException {
        logger.info("Попытка удаления клиента");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw new MyException("Клиент не найден");
            }

            em.getTransaction().begin();

            // Получаем все проекты клиента
            List<Project> projects = em.createQuery(
                "SELECT p FROM Project p WHERE p.client = :client", Project.class)
                .setParameter("client", client)
                .getResultList();

            // Обнуляем ссылки на клиента в проектах
            for (Project project : projects) {
                project.setClient(null);
                em.merge(project);
            }

            em.remove(client);
            em.getTransaction().commit();
            updateClientTable();
            updateProjectTable(projectTable); // Обновляем таблицу проектов
            logger.info("Клиент успешно удален");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при удалении клиента: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Генерация PDF-отчета по клиентам
    public void generatePdfReportClients() {
        logger.info("Пользователь начал генерацию PDF отчета");
        try {
            // Новый путь к файлу XML и обновленный XPath
            String datasource = "C:/MEGA_OOP_PROJECT/all_data.xml";
            String xpath = "/data/clients/client"; // Предполагаемая структура в all_data.xml
            String template = "C:/MEGA_OOP_PROJECT/clients.jrxml";
            String resultpath = "C:/MEGA_OOP_PROJECT/clients_report.pdf";

            // Указание источника XML-данных
            JRDataSource ds = new JRXmlDataSource(new File(datasource), xpath);
            // Создание отчета на базе шаблона
            JasperReport jasperReport = JasperCompileManager.compileReport(template);
            // Заполнение отчета данными
            JasperPrint print = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            // Генерация отчета в формате PDF
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(resultpath));
            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            JOptionPane.showMessageDialog(this, "Отчет успешно сохранен в " + resultpath);
            logger.info("PDF отчет успешно сгенерирован");
        } catch (JRException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при генерации отчета: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logger.error("Ошибка при генерации PDF отчета: " + e.getMessage());
        }
    }
    // Генерация PDF-отчета по сотрудникам
    private void generatePdfReportEmployees() {
        logger.info("Пользователь начал генерацию PDF отчета по сотрудникам");
        try {
            String datasource = "C:/MEGA_OOP_PROJECT/all_data.xml";
            String xpath = "/data/employees/employee";
            String template = "C:/MEGA_OOP_PROJECT/employees.jrxml";
            String resultpath = "C:/MEGA_OOP_PROJECT/employees_report.pdf";

            JRDataSource ds = new JRXmlDataSource(new File(datasource), xpath);
            JasperReport jasperReport = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(resultpath));
            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            JOptionPane.showMessageDialog(this, "Отчет успешно сохранен в " + resultpath);
            logger.info("PDF отчет по сотрудникам успешно сгенерирован");
        } catch (JRException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при генерации отчета: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logger.error("Ошибка при генерации PDF отчета по сотрудникам: " + e.getMessage());
        }
    }
    // Генерация PDF-отчета по проектам
    private void generatePdfReportProjects() {
        logger.info("Пользователь начал генерацию PDF отчета по проектам");
        try {
            String datasource = "C:/MEGA_OOP_PROJECT/all_data.xml";
            String xpath = "/data/projects/project"; // Предполагаемая структура в all_data.xml
            String template = "C:/MEGA_OOP_PROJECT/projects.jrxml";
            String resultpath = "C:/MEGA_OOP_PROJECT/projects_report.pdf";

            JRDataSource ds = new JRXmlDataSource(new File(datasource), xpath);
            JasperReport jasperReport = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(resultpath));
            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            JOptionPane.showMessageDialog(this, "Отчет успешно сохранен в " + resultpath);
            logger.info("PDF отчет по проектам успешно сгенерирован");
        } catch (JRException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при генерации отчета: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logger.error("Ошибка при генерации PDF отчета по проектам: " + e.getMessage());
        }
    }
    // Генерация PDF-отчета по задачам
    private void generatePdfReportTasks() {
        logger.info("Пользователь начал генерацию PDF отчета по задачам");
        try {
            String datasource = "C:/MEGA_OOP_PROJECT/all_data.xml";
            String xpath = "/data/tasks/task"; // Предполагаемая структура в all_data.xml
            String template = "C:/MEGA_OOP_PROJECT/tasks.jrxml";
            String resultpath = "C:/MEGA_OOP_PROJECT/tasks_report.pdf";

            JRDataSource ds = new JRXmlDataSource(new File(datasource), xpath);
            JasperReport jasperReport = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(jasperReport, new HashMap<>(), ds);
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(resultpath));
            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            JOptionPane.showMessageDialog(this, "Отчет успешно сохранен в " + resultpath);
            logger.info("PDF отчет по задачам успешно сгенерирован");
        } catch (JRException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка при генерации отчета: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            logger.error("Ошибка при генерации PDF отчета по задачам: " + e.getMessage());
        }
    }

    // Показать данные сотрудника
    private void showEmployeeData(int employeeId) throws MyException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            // Найти сотрудника по его ID
            Employee employee = em.find(Employee.class, employeeId);
            if (employee == null) {
                throw new MyException("Сотрудник с ID " + employeeId + " не найден.");
            }

            // Получить все проекты сотрудника через связь many-to-many
            List<Project> projects = em.createQuery(
                "SELECT p FROM Project p JOIN p.employees e WHERE e = :employee", Project.class)
                .setParameter("employee", employee)
                .getResultList();

            // Получить все задачи, связанные с сотрудником
            List<Task> tasks = em.createQuery(
                "SELECT t FROM Task t WHERE t.assignedEmployee = :employee", Task.class)
                .setParameter("employee", employee)
                .getResultList();

            // Формируем сообщение для отображения
            StringBuilder message = new StringBuilder("Информация о сотруднике: " + employee.getEmployee_name() + "\n\n");

            message.append("Проекты:\n");
            if (!projects.isEmpty()) {
                for (Project project : projects) {
                    message.append("- ").append(project.getProject_name()).append("\n");
                }
            } else {
                message.append("  - Нет связанных проектов\n");
            }

            message.append("\nЗадачи:\n");
            if (!tasks.isEmpty()) {
                for (Task task : tasks) {
                    message.append("- ").append(task.getTask_name())
                            .append(" (Проект: ").append(task.getProject().getProject_name()).append(")\n");
                }
            } else {
                message.append("  - Нет связанных задач\n");
            }

            // Показать сообщение пользователю
            JOptionPane.showMessageDialog(this, message.toString(), "Информация о сотруднике", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            throw new MyException("Ошибка при загрузке данных сотрудника: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }

    }
    // Создание панели для работы с сотрудниками
    private JPanel createEmployeesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        employeeTable = new JTable();
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Добавляем слушатель для мыши на employeeTable
        employeeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Обработка двойного клика
                    int selectedRow = employeeTable.getSelectedRow();
                    if (selectedRow != -1) {
                        try {
                            int employeeId = (Integer) employeeTable.getValueAt(selectedRow, 0);
                            showEmployeeData(employeeId);
                        } catch (MyException ex) {
                            showErrorMessage(ex.getMessage());
                        }
                    } else {
                        showErrorMessage("Выберите сотрудника для отображения данных");
                    }
                }
            }
        });
        // Запрещаем редактирование ячеек таблицы
        employeeTable.setDefaultEditor(Object.class, null);
        try {
            updateEmployeeTable();
        } catch (MyException ex) {
            showErrorMessage(ex.getMessage());
        }

        // Добавляем панель поиска сверху
        panel.add(createSearchPanel(employeeTable, "Поиск по имени сотрудника..."), BorderLayout.NORTH);
        panel.add(new JScrollPane(employeeTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addEmployeeButton = new JButton("Добавить");
        addEmployeeButton.addActionListener(e -> {
            try {
                addNewEmployee();
            } catch (MyException ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        JButton editEmployeeButton = new JButton("Изменить");
        editEmployeeButton.addActionListener(e -> {
            int selectedRow = employeeTable.getSelectedRow();
            if (selectedRow != -1) {
                try {
                    editEmployee((Integer)employeeTable.getValueAt(selectedRow, 0));
                } catch (MyException ex) {
                    showErrorMessage(ex.getMessage());
                }
            } else {
                showErrorMessage("Выберите сотрудника для изменения");
            }
        });

        JButton deleteEmployeeButton = new JButton("Удалить");
        deleteEmployeeButton.addActionListener(e -> {
            int selectedRow = employeeTable.getSelectedRow();
            if (selectedRow != -1) {
                int employeeId = (Integer)employeeTable.getValueAt(selectedRow, 0);
                String employeeName = (String)employeeTable.getValueAt(selectedRow, 1);
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Вы действительно хотите удалить сотрудника \"" + employeeName + "\"?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        deleteEmployee(employeeId);
                    } catch (MyException ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
            } else {
                showErrorMessage("Выберите сотрудника для удаления");
            }
        });

        // Добавляем кнопку для сохранения в PDF
        JButton savePdfButton = new JButton("Сохранить в PDF");
        savePdfButton.addActionListener(e -> {
            try {
                saveAllToXML(); // Сначала сохраняем в XML
                generatePdfReportEmployees();
            } catch (Exception ex) {
                showErrorMessage("Ошибка при сохранении отчета: " + ex.getMessage());
            }
        });


        Dimension buttonSize = new Dimension(150, 35);
        addEmployeeButton.setPreferredSize(buttonSize);
        editEmployeeButton.setPreferredSize(buttonSize);
        deleteEmployeeButton.setPreferredSize(buttonSize);
        savePdfButton.setPreferredSize(buttonSize);

        buttonPanel.add(addEmployeeButton);
        buttonPanel.add(editEmployeeButton);
        buttonPanel.add(deleteEmployeeButton);
        buttonPanel.add(savePdfButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Обновление данных в таблице сотрудников
    private void updateEmployeeTable() throws MyException {
        logger.info("Обновление таблицы сотрудников");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            List<Employee> employees = em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();
            String[] columnNames = {"ID", "Имя сотрудника"};
            Object[][] data = new Object[employees.size()][2];
            for (int i = 0; i < employees.size(); i++) {
                Employee employee = employees.get(i);
                data[i][0] = employee.getEmployee_id();
                data[i][1] = employee.getEmployee_name();
            }
            employeeTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при обновлении таблицы сотрудников: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Форма добавления нового сотрудника
    private void addNewEmployee() throws MyException {
        logger.info("Пользователь открыл форму создания нового сотрудника");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            JTextField employeeNameField = new JTextField();

            Object[] fields = {"Имя сотрудника:", employeeNameField};
            int result = JOptionPane.showConfirmDialog(null, fields, "Добавить нового сотрудника", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String employeeName = employeeNameField.getText();
                validateName(employeeName); // Валидация имени сотрудника

                em.getTransaction().begin();
                Employee newEmployee = new Employee();
                newEmployee.setEmployee_name(employeeName);
                em.persist(newEmployee);
                em.getTransaction().commit();
                updateEmployeeTable();
                logger.info("Создан новый сотрудник: " + employeeName);
            }
        } catch (MyException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при добавлении сотрудника: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Форма редактирования данных сотрудника
    private void editEmployee(int employeeId) throws MyException {
        logger.info("Пользователь открыл форму редактирования сотрудника");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Employee employee = em.find(Employee.class, employeeId);
            if (employee == null) {
                throw new MyException("Сотрудник не найден");
            }

            JTextField employeeNameField = new JTextField(employee.getEmployee_name());
            Object[] fields = {"Имя сотрудника:", employeeNameField};

            int result = JOptionPane.showConfirmDialog(null, fields, "Изменить сотрудника",
                JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                String newName = employeeNameField.getText();
                validateName(newName);

                em.getTransaction().begin();
                employee.setEmployee_name(newName);
                em.getTransaction().commit();
                updateEmployeeTable();
                logger.info("Сотрудник успешно отредактирован: " + newName);
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при изменении сотрудника: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Удаление сотрудника из базы данных
    private void deleteEmployee(int employeeId) throws MyException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            
            Employee employee = em.find(Employee.class, employeeId);
            if (employee == null) {
                throw new MyException("Сотрудник не найден");
            }

            // Обновляем все задачи, где был назначен этот сотрудник
            Query taskQuery = em.createQuery(
                "UPDATE Task t SET t.assignedEmployee = NULL WHERE t.assignedEmployee = :employee");
            taskQuery.setParameter("employee", employee);
            taskQuery.executeUpdate();

            // Сначала удаляем связи сотрудника с проектами
            TypedQuery<Project> projectQuery = em.createQuery(
                "SELECT p FROM Project p WHERE :employee MEMBER OF p.employees", Project.class);
            projectQuery.setParameter("employee", employee);
            List<Project> projects = projectQuery.getResultList();
            
            for (Project project : projects) {
                project.getEmployees().remove(employee);
                em.merge(project);
            }

            // Удаляем сотрудника
            em.remove(employee);
            em.getTransaction().commit();

            // Обновляем все таблицы
            updateEmployeeTable();
            updateTaskTable(taskTable);
            updateProjectTable(projectTable);
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при удалении сотрудника: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }
    // Создание панели для работы с задачами
    private JPanel createTasksPanel() {
        // Создание панели для работы с задачами
        JPanel panel = new JPanel(new BorderLayout());

        // Create the task table
        String[] columnNames = {"ID", "Название задачи", "Описание", "Статус", "Проект", "Сотрудник"};
        taskTable = new JTable();
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Добавляем слушатель для мыши на taskTable
        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    // Single click: select the row
                    int row = taskTable.getSelectedRow();
                    if (row != -1) {
                        // Optionally highlight the selected row or perform any other action
                    }
                }
            }
        });

        try {
            updateTaskTable(taskTable);
        } catch (MyException ex) {
            showErrorMessage(ex.getMessage());
        }

        // Добавляем панель поиска сверху
        panel.add(createSearchPanel(taskTable, "Поиск по названию задачи..."), BorderLayout.NORTH);
        panel.add(new JScrollPane(taskTable), BorderLayout.CENTER);


        // Кнопки управления задачами
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addTaskButton = new JButton("Добавить");
        addTaskButton.addActionListener(e -> {
            try {
                addNewTask(taskTable);
            } catch (MyException ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        JButton editTaskButton = new JButton("Изменить");
        editTaskButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                try {
                    editTask((Integer)taskTable.getValueAt(selectedRow, 0));
                } catch (MyException ex) {
                    showErrorMessage(ex.getMessage());
                }
            } else {
                showErrorMessage("Выберите задачу для изменения");
            }
        });

        JButton deleteTaskButton = new JButton("Удалить");
        deleteTaskButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                int taskId = (Integer)taskTable.getValueAt(selectedRow, 0);
                String taskName = (String)taskTable.getValueAt(selectedRow, 1);
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Вы действительно хотите удалить задачу \"" + taskName + "\"?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        deleteTask(taskId);
                    } catch (MyException ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
            } else {
                showErrorMessage("Выберите задачу для удаления");
            }
        });

        // Добавляем кнопку для сохранения в PDF
        JButton savePdfButton = new JButton("Сохранить в PDF");
        savePdfButton.addActionListener(e -> {
            try {
                saveAllToXML(); // Сначала сохраняем в XML
                generatePdfReportTasks();
            } catch (Exception ex) {
                showErrorMessage("Ошибка при сохранении отчета: " + ex.getMessage());
            }
        });


        Dimension buttonSize = new Dimension(150, 35);
        addTaskButton.setPreferredSize(buttonSize);
        editTaskButton.setPreferredSize(buttonSize);
        deleteTaskButton.setPreferredSize(buttonSize);
        savePdfButton.setPreferredSize(buttonSize);

        buttonPanel.add(addTaskButton);
        buttonPanel.add(editTaskButton);
        buttonPanel.add(deleteTaskButton);
        buttonPanel.add(savePdfButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Обновление данных в таблице задач
    private void updateTaskTable(JTable taskTable) throws MyException {
        logger.info("Обновление таблицы задач");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            List<Task> tasks = em.createQuery("SELECT t FROM Task t", Task.class).getResultList();
            String[] columnNames = {"ID", "Название задачи", "Описание", "Статус", "Срок", "Проект", "Сотрудник"};
            Object[][] data = new Object[tasks.size()][7];
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                data[i][0] = task.getTask_id();
                data[i][1] = task.getTask_name();
                data[i][2] = task.getTask_description();
                data[i][3] = task.getTask_status();
                DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                data[i][4] = task.getDueDate() != null ? dateFormat.format(task.getDueDate()) : "Не назначен";
                data[i][5] = task.getProject() != null ? task.getProject().getProject_name() : "Не назначен";
                data[i][6] = task.getAssignedEmployee() != null ? task.getAssignedEmployee().getEmployee_name() : "Не назначен";

                // Проверка срока выполнения
                if (task.getDueDate() != null && task.getDueDate().before(new Date()) && !task.getTask_status().equals("Завершено")) {
                    task.setTask_status("Просрочено");
                    em.merge(task);
                }
            }
            taskTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при обновлении таблицы задач: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Форма добавления новой задачи
    private void addNewTask(JTable taskTable) throws MyException {
        logger.info("Пользователь открыл форму создания новой задачи");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            // Получаем списки проектов и сотрудников
            List<Project> projects = em.createQuery("SELECT p FROM Project p", Project.class).getResultList();
            List<Employee> employees = em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();

            // Создаем диалоговое окно для ввода данных
            JTextField taskNameField = new JTextField();
            JTextField taskDescriptionField = new JTextField();
            JComboBox<String> taskStatusField = new JComboBox<>(new String[]{"В процессе", "Завершено", "Отложено", "Просрочено"});
            JTextField dueDateField = new JTextField();

            // Выбор проекта
            JComboBox<Project> projectComboBox = new JComboBox<>(projects.toArray(new Project[0]));
            projectComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Project) {
                        setText(((Project) value).getProject_name());
                    }
                    return this;
                }
            });

            // Выбор сотрудника
            JComboBox<Employee> employeeComboBox = new JComboBox<>(employees.toArray(new Employee[0]));
            employeeComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Employee) {
                        setText(((Employee) value).getEmployee_name());
                    }
                    return this;
                }
            });

            Object[] fields = {
                "Название задачи:", taskNameField,
                "Описание:", taskDescriptionField,
                "Статус:", taskStatusField,
                "Срок выполнения:", dueDateField,
                "Проект:", projectComboBox,
                "Назначить сотрудника:", employeeComboBox
            };

            int result = JOptionPane.showConfirmDialog(null, fields, "Добавить новую задачу",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String taskName = taskNameField.getText();
                String taskDescription = taskDescriptionField.getText();
                String taskStatus = (String) taskStatusField.getSelectedItem();
                Project selectedProject = (Project) projectComboBox.getSelectedItem();
                Employee selectedEmployee = (Employee) employeeComboBox.getSelectedItem();

                Date dueDate;
                try {
                    dueDate = new SimpleDateFormat("dd.MM.yyyy").parse(dueDateField.getText());
                } catch (ParseException e) {

                    showErrorMessage("Неверный формат даты. Пожалуйста, используйте формат ГГГГ-ММ-ДД.");
                    return;
                }
                if (dueDate == null) {
                    throw new MyException("Неверно указан срок выполнения");
                }

                if (taskName.trim().isEmpty()) {
                    throw new MyException("Название задачи не может быть пустым");
                }

                try {
                    em.getTransaction().begin();
                    Task newTask = new Task();
                    newTask.setTask_name(taskName);
                    newTask.setTask_description(taskDescription);
                    newTask.setTask_status(taskStatus);
                    newTask.setDueDate(dueDate);
                    newTask.setProject(selectedProject);
                    newTask.setAssignedEmployee(selectedEmployee);

                    em.persist(newTask);
                    em.getTransaction().commit();
                    updateTaskTable(taskTable);
                    logger.info("Создана новая задача: " + taskName);
                } catch (Exception e) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    throw new MyException("Ошибка при добавлении задачи: " + e.getMessage());
                }
            }
        } finally {
            em.close();
            emf.close();
        }
    }

    // Форма редактирования задачи
    private void editTask(int taskId) throws MyException {
        logger.info("Пользователь открыл форму редактирования задачи");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Task task = em.find(Task.class, taskId);
            if (task == null) {
                throw new MyException("Задача не найдена");
            }

            JTextField taskNameField = new JTextField(task.getTask_name());
            JTextField taskDescriptionField = new JTextField(task.getTask_description());
            JComboBox<String> taskStatusField = new JComboBox<>(new String[]{"В процессе", "Завершено", "Отложено", "Просрочено"});
            taskStatusField.setSelectedItem(task.getTask_status());
            JTextField dueDateField = new JTextField(new SimpleDateFormat("dd.MM.yyyy").format(task.getDueDate()));


            // Project selection
            List<Project> projects = em.createQuery("SELECT p FROM Project p", Project.class).getResultList();
            JComboBox<Project> projectComboBox = new JComboBox<>(projects.toArray(new Project[0]));
            projectComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Project) {
                        setText(((Project) value).getProject_name());
                    }
                    return this;
                }
            });
            if (task.getProject() != null) {
                projectComboBox.setSelectedItem(task.getProject());
            }

            // Выбор сотрудника
            List<Employee> employees = em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();
            JComboBox<Employee> employeeComboBox = new JComboBox<>(employees.toArray(new Employee[0]));
            employeeComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Employee) {
                        setText(((Employee) value).getEmployee_name());
                    }
                    return this;
                }
            });
            if (task.getAssignedEmployee() != null) {
                employeeComboBox.setSelectedItem(task.getAssignedEmployee());
            }

            Object[] fields = {
                "Название задачи:", taskNameField,
                "Описание:", taskDescriptionField,
                "Статус:", taskStatusField,
                "Срок выполнения:", dueDateField,
                "Проект:", projectComboBox,
                "Назначить сотрудника:", employeeComboBox
            };

            int result = JOptionPane.showConfirmDialog(null, fields, "Изменить задачу",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String newTaskName = taskNameField.getText();
                String newTaskDescription = taskDescriptionField.getText();
                String newTaskStatus = (String) taskStatusField.getSelectedItem();
                Project newProject = (Project) projectComboBox.getSelectedItem();
                Employee newEmployee = (Employee) employeeComboBox.getSelectedItem();

                Date newDueDate;
                try {
                    newDueDate = new SimpleDateFormat("dd.MM.yyyy").parse(dueDateField.getText());
                } catch (ParseException e) {

                    showErrorMessage("Неверный формат даты. Пожалуйста, используйте формат ГГГГ-ММ-ДД.");
                    return;
                }
                if (newDueDate == null) {
                    throw new MyException("Неверно указан срок выполнения");
                }

                if (newTaskName.trim().isEmpty()) {
                    throw new MyException("Название задачи не может быть пустым");
                }

                try {
                    em.getTransaction().begin();
                    task.setTask_name(newTaskName);
                    task.setTask_description(newTaskDescription);
                    task.setTask_status(newTaskStatus);
                    task.setDueDate(newDueDate);
                    task.setProject(newProject);
                    task.setAssignedEmployee(newEmployee);
                    em.getTransaction().commit();
                    updateTaskTable(taskTable);
                    logger.info("Задача успешно отредактирована: " + newTaskName);
                } catch (Exception e) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    throw new MyException("Ошибка при изменении задачи: " + e.getMessage());
                }
            }
        } finally {
            em.close();
            emf.close();
        }
    }

    // Удаление задачи из базы данных
    private void deleteTask(int taskId) throws MyException {
        logger.info("Попытка удаления задачи");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Task task = em.find(Task.class, taskId);
            if (task == null) {
                throw new MyException("Задача не найдена");
            }

            em.getTransaction().begin();

            // Удаляем связи
            if (task.getProject() != null) {
                task.getProject().getTasks().remove(task);
            }
            if (task.getAssignedEmployee() != null) {
                task.setAssignedEmployee(null);
            }

            em.remove(task);
            em.getTransaction().commit();
            updateTaskTable(taskTable);
            logger.info("Задача успешно удалена");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при удалении задачи: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }
    // Создание панели для работы с проектами
    private JPanel createProjectsPanel() {
        // Создание панели для работы с проектами
        JPanel panel = new JPanel(new BorderLayout());

        // Create the project table
        String[] columnNames = {"ID", "Название проекта", "Статус", "Дата сдачи", "Дата окончания", "Клиент", "Кол-во сотрудников", "Кол-во задач"};
        projectTable = new JTable();
        projectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add listener for checkbox
        JCheckBox overdueFilter = new JCheckBox("Показать проекты с просроченными задачами");
        overdueFilter.addActionListener(e -> {
            try {
                // Сбрасываем поисковый фильтр перед применением фильтра просроченных задач
                projectTable.setRowSorter(null);
                updateProjectTable(projectTable, overdueFilter.isSelected());
            } catch (MyException ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        // Добавляем слушатель выбора строки
        projectTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    // Single click: select the row
                    int row = projectTable.getSelectedRow();
                    if (row != -1) {
                        // Optionally highlight the selected row or perform any other action
                    }
                } else if (e.getClickCount() == 2) {
                    // Double click: show detailed information
                    int row = projectTable.getSelectedRow();
                    if (row != -1) {
                        int projectId = (int) projectTable.getValueAt(row, 0); // Assuming ID is in the first column
                        try {
                            showProjectDetails(projectId);
                        } catch (MyException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });

        try {
            updateProjectTable(projectTable);
        } catch (MyException ex) {
            showErrorMessage(ex.getMessage());
        }

        // Добавляем панель поиска сверху
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Поиск по названию проекта...");
        
        // Добавляем слушатель изменений в поле поиска
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // Сбрасываем чекбокс при поиске
                if (overdueFilter.isSelected()) {
                    overdueFilter.setSelected(false);
                    try {
                        updateProjectTable(projectTable, false);
                    } catch (MyException ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
                filterTable(searchField.getText(), projectTable);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("🔍"));
        searchPanel.add(searchField);
        panel.add(searchPanel, BorderLayout.NORTH);

        panel.add(new JScrollPane(projectTable), BorderLayout.CENTER);

        // Кнопки управления проектами
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addProjectButton = new JButton("Добавить");
        addProjectButton.addActionListener(e -> {
            try {
                addNewProject(projectTable);
            } catch (MyException ex) {
                showErrorMessage(ex.getMessage());
            }
        });

        JButton editProjectButton = new JButton("Изменить");
        editProjectButton.addActionListener(e -> {
            int selectedRow = projectTable.getSelectedRow();
            if (selectedRow != -1) {
                try {
                    editProject((Integer)projectTable.getValueAt(selectedRow, 0));
                    updateProjectTable(projectTable);
                } catch (MyException ex) {
                    showErrorMessage(ex.getMessage());
                }
            } else {
                showErrorMessage("Выберите проект для изменения");
            }
        });

        JButton deleteProjectButton = new JButton("Удалить");
        deleteProjectButton.addActionListener(e -> {
            int selectedRow = projectTable.getSelectedRow();
            if (selectedRow != -1) {
                int projectId = (Integer)projectTable.getValueAt(selectedRow, 0);
                String projectName = (String)projectTable.getValueAt(selectedRow, 1);
                
                // Проверяем связанные задачи
                EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
                EntityManager em = emf.createEntityManager();
                List<Task> tasks = em.createQuery(
                    "SELECT t FROM Task t WHERE t.project.project_id = :projectId", Task.class)
                    .setParameter("projectId", projectId)
                    .getResultList();
                em.close();
                emf.close();

                String message = "Вы действительно хотите удалить проект \"" + projectName + "\"?";
                if (!tasks.isEmpty()) {
                    message += "\nВнимание: при удалении проекта также будут удалены все связанные задачи (" + 
                             tasks.size() + " шт.).";
                }

                int choice = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    try {
                        deleteProject(projectId);
                    } catch (MyException ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
            } else {
                showErrorMessage("Выберите проект для удаления");
            }
        });

        // Добавляем кнопку для сохранения в PDF
        JButton savePdfButton = new JButton("Сохранить в PDF");
        savePdfButton.addActionListener(e -> {
            try {
                saveAllToXML(); // Сначала сохраняем в XML
                generatePdfReportProjects();
            } catch (Exception ex) {
                showErrorMessage("Ошибка при сохранении отчета: " + ex.getMessage());
            }
        });


        Dimension buttonSize = new Dimension(150, 35);
        addProjectButton.setPreferredSize(buttonSize);
        editProjectButton.setPreferredSize(buttonSize);
        deleteProjectButton.setPreferredSize(buttonSize);
        savePdfButton.setPreferredSize(buttonSize);

        buttonPanel.add(addProjectButton);
        buttonPanel.add(editProjectButton);
        buttonPanel.add(deleteProjectButton);
        buttonPanel.add(savePdfButton);
        buttonPanel.add(overdueFilter);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Отображение детальной информации о проекте
    private void showProjectDetails(int projectId) throws MyException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            Project project = em.find(Project.class, projectId);
            if (project == null) {
                throw new MyException("Проект не найден");
            }

            StringBuilder details = new StringBuilder();
            details.append("Проект: ").append(project.getProject_name()).append("\n\n");

            // Информация о клиенте
            if (project.getClient() != null) {
                details.append("Клиент: ").append(project.getClient().getClient_name()).append("\n\n");
            }

            // Список сотрудников
            details.append("Сотрудники проекта:\n");
            if (!project.getEmployees().isEmpty()) {
                for (Employee emp : project.getEmployees()) {
                    details.append("- ").append(emp.getEmployee_name()).append("\n");
                }
            } else {
                details.append("Нет назначенных сотрудников\n");
            }
            details.append("\n");

            // Список задач
            details.append("Задачи проекта:\n");
            if (!project.getTasks().isEmpty()) {
                for (Task task : project.getTasks()) {
                    details.append("- ").append(task.getTask_name())
                          .append(" (").append(task.getDueDate()).append(")")
                          .append(" - ").append(task.getAssignedEmployee() != null ?
                                  task.getAssignedEmployee().getEmployee_name() : "Не назначено")
                          .append("\n");
                }
            } else {
                details.append("Нет задач\n");
            }

            JOptionPane.showMessageDialog(this, details.toString(),
                "Детали проекта", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            throw new MyException("Ошибка при получении деталей проекта: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Обновление таблицы проектов с фильтром по просроченным
    private void updateProjectTable(JTable projectTable, boolean showOverdueOnly) throws MyException {
        logger.info("Обновление таблицы проектов");
        if (projectTable == null) {
            throw new MyException("Таблица проектов не инициализирована");
        }

        EntityManagerFactory emf = null;
        EntityManager em = null;

        try {
            emf = Persistence.createEntityManagerFactory("kursach_schema");
            em = emf.createEntityManager();

            // Сначала загружаем проекты с клиентами
            String jpql = "SELECT DISTINCT p FROM Project p LEFT JOIN FETCH p.client";
            if (showOverdueOnly) {
                jpql += " WHERE EXISTS (SELECT t FROM Task t WHERE t.project = p AND t.task_status = 'Просрочено')";
            }
            List<Project> projects = em.createQuery(jpql, Project.class).getResultList();

            if (projects == null) {
                throw new MyException("Не удалось получить список проектов");
            }

            // Затем для каждого проекта инициализируем коллекции
            for (Project project : projects) {
                // Инициализируем коллекции, чтобы избежать LazyInitializationException
                if (project.getEmployees() != null) {
                    project.getEmployees().size();
                }
                if (project.getTasks() != null) {
                    project.getTasks().size();
                }
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            String[] columnNames = {"ID", "Название проекта", "Статус", "Дата сдачи", "Дата окончания", "Клиент", "Кол-во сотрудников", "Кол-во задач"};
            Object[][] data = new Object[projects.size()][8];

            for (int i = 0; i < projects.size(); i++) {
                Project project = projects.get(i);
                if (project != null) {
                    data[i][0] = project.getProject_id();
                    data[i][1] = project.getProject_name() != null ? project.getProject_name() : "";
                    data[i][2] = project.getProject_status() != null ? project.getProject_status() : "";
                    data[i][3] = project.getProject_due_date() != null ? dateFormat.format(project.getProject_due_date()) : "";
                    data[i][4] = project.getProject_end_date() != null ? dateFormat.format(project.getProject_end_date()) : "";
                    data[i][5] = project.getClient() != null ? project.getClient().getClient_name() : "Не назначен";
                    data[i][6] = project.getEmployees() != null ? project.getEmployees().size() : 0;
                    data[i][7] = project.getTasks() != null ? project.getTasks().size() : 0;
                }
            }

            projectTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 0) {
                        return Integer.class;
                    }
                    return String.class;
                }
            });

            // Устанавливаем предпочтительную ширину столбцов
            projectTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
            projectTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Название
            projectTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Статус
            projectTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Дата сдачи
            projectTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Дата окончания
            projectTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Клиент
            projectTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Кол-во сотрудников
            projectTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Кол-во задач

        } catch (Exception e) {
            e.printStackTrace(); // Добавляем вывод стека ошибки для отладки
            throw new MyException("Ошибка при обновлении таблицы проектов: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
            if (emf != null) {
                emf.close();
            }
        }
    }

    // Перегруженный метод для обратной совместимости
    private void updateProjectTable(JTable projectTable) throws MyException {
        updateProjectTable(projectTable, false);
    }

    // Форма добавления нового проекта
    private void addNewProject(JTable projectTable) throws MyException {
        logger.info("Пользователь открыл форму создания нового проекта");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            List<Client> clients = em.createQuery("SELECT c FROM Client c", Client.class).getResultList();
            List<Employee> allEmployees = em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();
            List<Task> unassignedTasks = em.createQuery("SELECT t FROM Task t WHERE t.project IS NULL", Task.class).getResultList();

            // Create form components
            JTextField projectNameField = new JTextField();
            JComboBox<String> projectStatusField = new JComboBox<>(new String[]{"Активный", "Завершен", "Приостановлен"});
            JTextField dueDateField = new JTextField();
            JTextField endDateField = new JTextField();

            // Client selection
            JComboBox<Client> clientComboBox = new JComboBox<>(clients.toArray(new Client[0]));
            clientComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Client) {
                        setText(((Client) value).getClient_name());
                    }
                    return this;
                }
            });

            // Employee selection
            JList<Employee> employeeList = new JList<>(allEmployees.toArray(new Employee[0]));
            employeeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            employeeList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Employee) {
                        setText(((Employee) value).getEmployee_name());
                    }
                    return this;
                }
            });
            JScrollPane employeeScrollPane = new JScrollPane(employeeList);
            employeeScrollPane.setPreferredSize(new Dimension(200, 100));

            // Task selection
            JList<Task> taskList = new JList<>(unassignedTasks.toArray(new Task[0]));
            taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            taskList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Task) {
                        Task task = (Task) value;
                        setText(task.getTask_name() + " (" + task.getTask_status() + ")");
                    }
                    return this;
                }
            });
            JScrollPane taskScrollPane = new JScrollPane(taskList);
            taskScrollPane.setPreferredSize(new Dimension(200, 100));

            Object[] fields = {
                "Название проекта:", projectNameField,
                "Статус:", projectStatusField,
                "Дата сдачи (дд.мм.гггг):", dueDateField,
                "Дата окончания (дд.мм.гггг):", endDateField,
                "Клиент:", clientComboBox,
                "Сотрудники (можно выбрать несколько):", employeeScrollPane,
                "Задачи (можно выбрать несколько):", taskScrollPane
            };

            int result = JOptionPane.showConfirmDialog(null, fields, "Добавить новый проект",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String projectName = projectNameField.getText();
                String projectStatus = (String) projectStatusField.getSelectedItem();
                Client selectedClient = (Client) clientComboBox.getSelectedItem();
                List<Employee> selectedEmployees = employeeList.getSelectedValuesList();
                List<Task> selectedTasks = taskList.getSelectedValuesList();

                if (projectName.trim().isEmpty()) {
                    throw new MyException("Название проекта не может быть пустым");
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                Date dueDate;
                try {
                    dueDate = dateFormat.parse(dueDateField.getText());
                } catch (ParseException e) {

                    showErrorMessage("Неверный формат даты. Пожалуйста, используйте формат дд.мм.гггг.");
                    return;
                }
                if (dueDate == null) {
                    throw new MyException("Неверно указан срок выполнения");
                }

                try {
                    em.getTransaction().begin();
                    Project newProject = new Project();
                    newProject.setProject_name(projectName);
                    newProject.setProject_status(projectStatus);
                    newProject.setProject_due_date(dueDate);
                    newProject.setClient(selectedClient);

                    // Add selected employees
                    for (Employee emp : selectedEmployees) {
                        newProject.addEmployee(emp);
                    }

                    // Add selected tasks
                    for (Task task : selectedTasks) {
                        newProject.addTask(task);
                    }

                    em.persist(newProject);
                    em.getTransaction().commit();
                    updateProjectTable(projectTable);
                    logger.info("Создан новый проект: " + projectName);
                } catch (Exception e) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    throw new MyException("Ошибка при добавлении проекта: " + e.getMessage());
                }
            }
        } finally {
            em.close();
            emf.close();
        }
    }

    // Форма редактирования проекта
    private void editProject(int projectId) throws MyException {
        logger.info("Пользователь открыл форму редактирования проекта");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            // Загружаем проект с клиентом
            Project project = em.createQuery(
                "SELECT p FROM Project p LEFT JOIN FETCH p.client WHERE p.project_id = :id",
                Project.class)
                .setParameter("id", projectId)
                .getSingleResult();

            if (project == null) {
                throw new MyException("Проект не найден");
            }

            // Загружаем сотрудников проекта
            project = em.createQuery(
                "SELECT p FROM Project p LEFT JOIN FETCH p.employees WHERE p.project_id = :id",
                Project.class)
                .setParameter("id", projectId)
                .getSingleResult();

            // Загружаем задачи проекта
            project = em.createQuery(
                "SELECT p FROM Project p LEFT JOIN FETCH p.tasks WHERE p.project_id = :id",
                Project.class)
                .setParameter("id", projectId)
                .getSingleResult();

            JTextField projectNameField = new JTextField(project.getProject_name());
            JComboBox<String> projectStatusField = new JComboBox<>(new String[]{"Активный", "Завершен", "Приостановлен"});
            projectStatusField.setSelectedItem(project.getProject_status());
            JTextField dueDateField = new JTextField(new SimpleDateFormat("dd.MM.yyyy").format(project.getProject_due_date()));
            JTextField endDateField = new JTextField();
            if (project.getProject_end_date() != null) {
                endDateField.setText(new SimpleDateFormat("dd.MM.yyyy").format(project.getProject_end_date()));
            }

            // Загружаем все возможные связанные сущности
            List<Client> clients = em.createQuery("SELECT c FROM Client c", Client.class).getResultList();
            List<Employee> allEmployees = em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();
            List<Task> availableTasks = em.createQuery(
                "SELECT t FROM Task t WHERE t.project IS NULL OR t.project.project_id = :projectId",
                Task.class)
                .setParameter("projectId", projectId)
                .getResultList();

            // Создаем штуку для выбора клиента
            JComboBox<Client> clientComboBox = new JComboBox<>(clients.toArray(new Client[0]));
            clientComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Client) {
                        setText(((Client) value).getClient_name());
                    }
                    return this;
                }
            });
            if (project.getClient() != null) {
                clientComboBox.setSelectedItem(project.getClient());
            }

            // Создаем список для выбора сотрудников
            JList<Employee> employeeList = new JList<>(allEmployees.toArray(new Employee[0]));
            employeeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            employeeList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Employee) {
                        setText(((Employee) value).getEmployee_name());
                    }
                    return this;
                }
            });

            // Выбираем текущих сотрудников
            int[] selectedEmployeeIndices = new int[project.getEmployees().size()];
            int i = 0;
            for (Employee emp : project.getEmployees()) {
                for (int j = 0; j < allEmployees.size(); j++) {
                    if (allEmployees.get(j).getEmployee_id() == emp.getEmployee_id()) {
                        selectedEmployeeIndices[i++] = j;
                        break;
                    }
                }
            }
            employeeList.setSelectedIndices(selectedEmployeeIndices);
            JScrollPane employeeScrollPane = new JScrollPane(employeeList);
            employeeScrollPane.setPreferredSize(new Dimension(200, 100));

            // Создаем список для выбора задач
            JList<Task> taskList = new JList<>(availableTasks.toArray(new Task[0]));
            taskList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            taskList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Task) {
                        Task task = (Task) value;
                        setText(task.getTask_name() + " (" + task.getTask_status() + ")");
                    }
                    return this;
                }
            });

            // Выбираем текущие задачи
            int[] selectedTaskIndices = new int[project.getTasks().size()];
            i = 0;
            for (Task task : project.getTasks()) {
                for (int j = 0; j < availableTasks.size(); j++) {
                    if (availableTasks.get(j).getTask_id() == task.getTask_id()) {
                        selectedTaskIndices[i++] = j;
                        break;
                    }
                }
            }
            taskList.setSelectedIndices(selectedTaskIndices);
            JScrollPane taskScrollPane = new JScrollPane(taskList);
            taskScrollPane.setPreferredSize(new Dimension(200, 100));

            Object[] fields = {
                    "Название проекта:", projectNameField,
                    "Статус:", projectStatusField,
                    "Дата сдачи (дд.мм.гггг):", dueDateField,
                    "Дата окончания (дд.мм.гггг):", endDateField,
                    "Клиент:", clientComboBox,
                    "Сотрудники (можно выбрать несколько):", employeeScrollPane,
                    "Задачи (можно выбрать несколько):", taskScrollPane
            };

            int result = JOptionPane.showConfirmDialog(null, fields, "Изменить проект",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String newProjectName = projectNameField.getText();
                String newProjectStatus = (String) projectStatusField.getSelectedItem();
                Client newClient = (Client) clientComboBox.getSelectedItem();
                List<Employee> newEmployees = employeeList.getSelectedValuesList();
                List<Task> newTasks = taskList.getSelectedValuesList();

                if (newProjectName.trim().isEmpty()) {
                    throw new MyException("Название проекта не может быть пустым");
                }

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                Date newDueDate;
                try {
                    newDueDate = dateFormat.parse(dueDateField.getText());
                } catch (ParseException e) {

                    showErrorMessage("Неверный формат даты. Пожалуйста, используйте формат дд.мм.гггг.");
                    return;
                }
                if (newDueDate == null) {
                    throw new MyException("Неверно указан срок выполнения");
                }

                Date newEndDate = null; // Default to null
                if (!endDateField.getText().trim().isEmpty()) {
                    try {
                        newEndDate = new SimpleDateFormat("dd.MM.yyyy").parse(endDateField.getText());
                    } catch (ParseException e) {
                        showErrorMessage("Неверный формат даты окончания. Пожалуйста, используйте формат дд.мм.гггг.");
                        return;
                    }
                }


                em.getTransaction().begin();
                try {
                    // Automatically set end date when status changes to "Завершен"
                    if ("Завершен".equals(newProjectStatus)) {
                        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd.MM.yyyy");
                        Date currentDate = new Date();
                        project.setProject_end_date(currentDate);
                        endDateField.setText(dateFormat1.format(currentDate));
                    }

                    // Обновляем основные поля
                    project.setProject_name(newProjectName);
                    project.setProject_status(newProjectStatus);
                    project.setProject_due_date(newDueDate);
                    if (!endDateField.getText().isEmpty()) {
                        project.setProject_end_date(newEndDate);
                    }
                    project.setClient(newClient);

                    // Обновляем список сотрудников
                    List<Employee> currentEmployees = new ArrayList<>(project.getEmployees());
                    for (Employee emp : currentEmployees) {
                        if (!newEmployees.contains(emp)) {
                            project.removeEmployee(emp);
                        }
                    }
                    for (Employee emp : newEmployees) {
                        if (!project.getEmployees().contains(emp)) {
                            project.addEmployee(emp);
                        }
                    }

                    // Обновляем список задач
                    List<Task> currentTasks = new ArrayList<>(project.getTasks());
                    for (Task task : currentTasks) {
                        if (!newTasks.contains(task)) {
                            project.removeTask(task);
                        }
                    }
                    for (Task task : newTasks) {
                        if (!project.getTasks().contains(task)) {
                            project.addTask(task);
                        }
                    }

                    em.merge(project);
                    em.getTransaction().commit();

                    // Обновляем все таблицы
                    refreshAllTables();
                    logger.info("Проект успешно отредактирован: " + newProjectName);
                } catch (Exception e) {
                    if (em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    throw new MyException("Ошибка при изменении проекта: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new MyException("Ошибка при изменении проекта: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Удаление проекта из базы данных
    private void deleteProject(int projectId) throws MyException {
        logger.info("Попытка удаления проекта");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            // Сначала загружаем сам проект
            Project project = em.find(Project.class, projectId);
            if (project == null) {
                throw new MyException("Проект не найден");
            }

            // Отдельно загружаем сотрудников
            project = em.createQuery(
                "SELECT p FROM Project p LEFT JOIN FETCH p.employees WHERE p.project_id = :id",
                Project.class)
                .setParameter("id", projectId)
                .getSingleResult();

            // Отдельно загружаем задачи
            project = em.createQuery(
                "SELECT p FROM Project p LEFT JOIN FETCH p.tasks WHERE p.project_id = :id",
                Project.class)
                .setParameter("id", projectId)
                .getSingleResult();

            em.getTransaction().begin();

            // Удаляем связи с сотрудниками
            for (Employee emp : new ArrayList<>(project.getEmployees())) {
                project.removeEmployee(emp);
            }

            // Удаляем связи с задачами
            for (Task task : new ArrayList<>(project.getTasks())) {
                project.removeTask(task);
                // Удаляем саму задачу
                em.remove(task);
            }

            em.remove(project);
            em.getTransaction().commit();
            updateProjectTable(projectTable);
            logger.info("Проект успешно удален");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new MyException("Ошибка при удалении проекта: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }
    // Метод для отображения сообщения об ошибке
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
    // Обновление всех таблиц
    private void refreshAllTables() throws MyException {
        logger.info("Пользователь обновляет все таблицы");
        try {
            updateProjectTable(projectTable);
            updateTaskTable(taskTable);
            updateEmployeeTable();
            updateClientTable();
            logger.info("Все таблицы успешно обновлены");
        } catch (Exception e) {
            logger.error("Ошибка при обновлении таблиц: " + e.getMessage(), e);
            throw new MyException("Ошибка при обновлении таблиц: " + e.getMessage());
        }
    }

    // Функция для сохранения всех сущностей в XML
    private void saveAllToXML() {
        logger.info("Пользователь начал экспорт данных в XML");
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            List<Client> clients = em.createQuery("SELECT c FROM Client c", Client.class).getResultList();
            List<Employee> employees = em.createQuery("SELECT e FROM Employee e", Employee.class).getResultList();
            List<Project> projects = em.createQuery("SELECT p FROM Project p", Project.class).getResultList();
            List<Task> tasks = em.createQuery("SELECT t FROM Task t", Task.class).getResultList();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Создаем корневой элемент
            Element rootElement = doc.createElement("data");
            doc.appendChild(rootElement);

            // Добавляем клиентов
            Element clientsElement = doc.createElement("clients");
            rootElement.appendChild(clientsElement);
            for (Client client : clients) {
                Element clientElement = doc.createElement("client");

                Element id = doc.createElement("client_id");
                id.appendChild(doc.createTextNode(String.valueOf(client.getClient_id())));
                clientElement.appendChild(id);

                Element name = doc.createElement("client_name");
                name.appendChild(doc.createTextNode(client.getClient_name()));
                clientElement.appendChild(name);

                clientsElement.appendChild(clientElement);
            }

            // Добавляем сотрудников
            Element employeesElement = doc.createElement("employees");
            rootElement.appendChild(employeesElement);
            for (Employee employee : employees) {
                Element employeeElement = doc.createElement("employee");

                Element id = doc.createElement("employee_id");
                id.appendChild(doc.createTextNode(String.valueOf(employee.getEmployee_id())));
                employeeElement.appendChild(id);

                Element name = doc.createElement("employee_name");
                name.appendChild(doc.createTextNode(employee.getEmployee_name()));
                employeeElement.appendChild(name);

                employeesElement.appendChild(employeeElement);
            }

            // Добавляем проекты
            Element projectsElement = doc.createElement("projects");
            rootElement.appendChild(projectsElement);
            for (Project project : projects) {
                Element projectElement = doc.createElement("project");

                Element id = doc.createElement("project_id");
                id.appendChild(doc.createTextNode(String.valueOf(project.getProject_id())));
                projectElement.appendChild(id);

                Element name = doc.createElement("project_name");
                name.appendChild(doc.createTextNode(project.getProject_name()));
                projectElement.appendChild(name);

                Element status = doc.createElement("project_status");
                status.appendChild(doc.createTextNode(project.getProject_status()));
                projectElement.appendChild(status);

                // Добавляем даты проекта
                Element dueDate = doc.createElement("project_due_date");
                if (project.getProject_due_date() != null) {
                    dueDate.appendChild(doc.createTextNode(project.getProject_due_date().toString()));
                }
                projectElement.appendChild(dueDate);

                Element endDate = doc.createElement("project_end_date");
                if (project.getProject_end_date() != null) {
                    endDate.appendChild(doc.createTextNode(project.getProject_end_date().toString()));
                }
                projectElement.appendChild(endDate);

                // Добавляем информацию о клиенте
                Element clientElement = doc.createElement("client");
                Element clientId = doc.createElement("client_id");
                clientId.appendChild(doc.createTextNode(String.valueOf(project.getClient().getClient_id())));
                Element clientName = doc.createElement("client_name");
                clientName.appendChild(doc.createTextNode(project.getClient().getClient_name()));
                clientElement.appendChild(clientId);
                clientElement.appendChild(clientName);
                projectElement.appendChild(clientElement);

                // Добавляем список сотрудников проекта с полной информацией
                Element employeesElement2 = doc.createElement("employees");
                for (Employee emp : project.getEmployees()) {
                    Element employeeElement = doc.createElement("employee");
                    
                    Element empId = doc.createElement("employee_id");
                    empId.appendChild(doc.createTextNode(String.valueOf(emp.getEmployee_id())));
                    employeeElement.appendChild(empId);
                    
                    Element empName = doc.createElement("employee_name");
                    empName.appendChild(doc.createTextNode(emp.getEmployee_name()));
                    employeeElement.appendChild(empName);
                    
                    employeesElement2.appendChild(employeeElement);
                }
                projectElement.appendChild(employeesElement2);

                // Добавляем список задач проекта с полной информацией
                Element tasksElement = doc.createElement("tasks");
                for (Task task : project.getTasks()) {
                    Element taskElement = doc.createElement("task");
                    
                    Element taskId = doc.createElement("task_id");
                    taskId.appendChild(doc.createTextNode(String.valueOf(task.getTask_id())));
                    taskElement.appendChild(taskId);
                    
                    Element taskName = doc.createElement("task_name");
                    taskName.appendChild(doc.createTextNode(task.getTask_name()));
                    taskElement.appendChild(taskName);
                    
                    Element taskDesc = doc.createElement("task_description");
                    taskDesc.appendChild(doc.createTextNode(task.getTask_description() != null ? task.getTask_description() : ""));
                    taskElement.appendChild(taskDesc);
                    
                    Element taskStatus = doc.createElement("task_status");
                    taskStatus.appendChild(doc.createTextNode(task.getTask_status()));
                    taskElement.appendChild(taskStatus);
                    
                    Element taskDueDate = doc.createElement("due_date");
                    if (task.getDueDate() != null) {
                        taskDueDate.appendChild(doc.createTextNode(task.getDueDate().toString()));
                    }
                    taskElement.appendChild(taskDueDate);
                    
                    // Добавляем информацию о назначенном сотруднике
                    Element taskEmployeeElement = doc.createElement("assigned_employee");
                    Element taskEmpId = doc.createElement("employee_id");
                    taskEmpId.appendChild(doc.createTextNode(String.valueOf(task.getAssignedEmployee().getEmployee_id())));
                    Element taskEmpName = doc.createElement("employee_name");
                    taskEmpName.appendChild(doc.createTextNode(task.getAssignedEmployee().getEmployee_name()));
                    taskEmployeeElement.appendChild(taskEmpId);
                    taskEmployeeElement.appendChild(taskEmpName);
                    taskElement.appendChild(taskEmployeeElement);
                    
                    tasksElement.appendChild(taskElement);
                }
                projectElement.appendChild(tasksElement);

                projectsElement.appendChild(projectElement);
            }

            // Добавляем задачи
            Element tasksElement = doc.createElement("tasks");
            rootElement.appendChild(tasksElement);
            for (Task task : tasks) {
                Element taskElement = doc.createElement("task");

                Element id = doc.createElement("task_id");
                id.appendChild(doc.createTextNode(String.valueOf(task.getTask_id())));
                taskElement.appendChild(id);

                Element name = doc.createElement("task_name");
                name.appendChild(doc.createTextNode(task.getTask_name()));
                taskElement.appendChild(name);

                Element description = doc.createElement("task_description");
                description.appendChild(doc.createTextNode(task.getTask_description() != null ? task.getTask_description() : ""));
                taskElement.appendChild(description);

                Element status = doc.createElement("task_status");
                status.appendChild(doc.createTextNode(task.getTask_status()));
                taskElement.appendChild(status);

                Element taskDueDate = doc.createElement("due_date");
                if (task.getDueDate() != null) {
                    taskDueDate.appendChild(doc.createTextNode(task.getDueDate().toString()));
                }
                taskElement.appendChild(taskDueDate);

                // Добавляем информацию о проекте
                Element projectElement = doc.createElement("project");
                Element projectId = doc.createElement("project_id");
                projectId.appendChild(doc.createTextNode(String.valueOf(task.getProject().getProject_id())));
                Element projectName = doc.createElement("project_name");
                projectName.appendChild(doc.createTextNode(task.getProject().getProject_name()));
                projectElement.appendChild(projectId);
                projectElement.appendChild(projectName);
                taskElement.appendChild(projectElement);

                // Добавляем информацию о назначенном сотруднике
                Element employeeElement = doc.createElement("assigned_employee");
                Element employeeId = doc.createElement("employee_id");
                employeeId.appendChild(doc.createTextNode(String.valueOf(task.getAssignedEmployee().getEmployee_id())));
                Element employeeName = doc.createElement("employee_name");
                employeeName.appendChild(doc.createTextNode(task.getAssignedEmployee().getEmployee_name()));
                employeeElement.appendChild(employeeId);
                employeeElement.appendChild(employeeName);
                taskElement.appendChild(employeeElement);

                tasksElement.appendChild(taskElement);
            }
            // Сохраняем в файл
            String filePath = "C:\\MEGA_OOP_PROJECT\\all_data.xml";
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

            JOptionPane.showMessageDialog(this, "Все данные успешно сохранены в XML файл:\n" + filePath,
                "Успех", JOptionPane.INFORMATION_MESSAGE);
            logger.info("Данные успешно экспортированы в XML: " + filePath);
        } catch (Exception e) {
            logger.error("Ошибка при экспорте в XML: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Ошибка при сохранении в XML: " + e.getMessage());
        } finally {
            em.close();
            emf.close();
        }
    }

    // Функция для загрузки данных из XML
    private void loadFromXML() {
        logger.info("Пользователь начал импорт данных из XML");
        File xmlFile = new File("C:\\MEGA_OOP_PROJECT\\all_data.xml");
        if (!xmlFile.exists()) {
            showErrorMessage("Файл XML не найден: " + xmlFile.getPath());
            return;
        }

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("kursach_schema");
        EntityManager em = emf.createEntityManager();

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            em.getTransaction().begin();

            // Загружаем клиентов
            NodeList clientsList = doc.getElementsByTagName("client");
            for (int i = 0; i < clientsList.getLength(); i++) {
                Node node = clientsList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    NodeList idNodes = element.getElementsByTagName("client_id");
                    NodeList nameNodes = element.getElementsByTagName("client_name");
                    
                    if (idNodes.getLength() > 0 && nameNodes.getLength() > 0) {
                        Node idNode = idNodes.item(0);
                        Node nameNode = nameNodes.item(0);
                        
                        if (idNode != null && nameNode != null && 
                            idNode.getTextContent() != null && !idNode.getTextContent().isEmpty() &&
                            nameNode.getTextContent() != null && !nameNode.getTextContent().isEmpty()) {
                            
                            int clientId = Integer.parseInt(idNode.getTextContent());
                            String clientName = nameNode.getTextContent();

                            Client client = em.find(Client.class, clientId);
                            if (client == null) {
                                client = new Client();
                            }
                            client.setClient_id(clientId);
                            client.setClient_name(clientName);
                            em.merge(client);
                        }
                    }
                }
            }

            // Загружаем сотрудников
            NodeList employeesList = doc.getElementsByTagName("employee");
            for (int i = 0; i < employeesList.getLength(); i++) {
                Node node = employeesList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    NodeList idNodes = element.getElementsByTagName("employee_id");
                    NodeList nameNodes = element.getElementsByTagName("employee_name");
                    
                    if (idNodes.getLength() > 0 && nameNodes.getLength() > 0) {
                        Node idNode = idNodes.item(0);
                        Node nameNode = nameNodes.item(0);
                        
                        if (idNode != null && nameNode != null && 
                            idNode.getTextContent() != null && !idNode.getTextContent().isEmpty() &&
                            nameNode.getTextContent() != null && !nameNode.getTextContent().isEmpty()) {
                            
                            int employeeId = Integer.parseInt(idNode.getTextContent());
                            String employeeName = nameNode.getTextContent();

                            Employee employee = em.find(Employee.class, employeeId);
                            if (employee == null) {
                                employee = new Employee();
                            }
                            employee.setEmployee_id(employeeId);
                            employee.setEmployee_name(employeeName);
                            em.merge(employee);
                        }
                    }
                }
            }

            // Загружаем проекты
            NodeList projectsList = doc.getElementsByTagName("project");
            for (int i = 0; i < projectsList.getLength(); i++) {
                Node node = projectsList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    NodeList idNodes = element.getElementsByTagName("project_id");
                    NodeList nameNodes = element.getElementsByTagName("project_name");
                    NodeList statusNodes = element.getElementsByTagName("project_status");
                    NodeList clientIdNodes = element.getElementsByTagName("client_id");
                    
                    if (idNodes.getLength() > 0 && nameNodes.getLength() > 0 && 
                        statusNodes.getLength() > 0 && clientIdNodes.getLength() > 0) {
                        
                        Node idNode = idNodes.item(0);
                        Node nameNode = nameNodes.item(0);
                        Node statusNode = statusNodes.item(0);
                        Node clientIdNode = clientIdNodes.item(0);
                        
                        if (idNode != null && nameNode != null && statusNode != null && clientIdNode != null &&
                            idNode.getTextContent() != null && !idNode.getTextContent().isEmpty() &&
                            nameNode.getTextContent() != null && !nameNode.getTextContent().isEmpty() &&
                            statusNode.getTextContent() != null && !statusNode.getTextContent().isEmpty() &&
                            clientIdNode.getTextContent() != null && !clientIdNode.getTextContent().isEmpty()) {
                            
                            int projectId = Integer.parseInt(idNode.getTextContent());
                            String projectName = nameNode.getTextContent();
                            String projectStatus = statusNode.getTextContent();
                            int clientId = Integer.parseInt(clientIdNode.getTextContent());

                            Project project = em.find(Project.class, projectId);
                            if (project == null) {
                                project = new Project();
                            }
                            project.setProject_id(projectId);
                            project.setProject_name(projectName);
                            project.setProject_status(projectStatus);

                            // Загружаем даты проекта
                            NodeList dueDateNodes = element.getElementsByTagName("project_due_date");
                            if (dueDateNodes.getLength() > 0) {
                                Node dueDateNode = dueDateNodes.item(0);
                                if (dueDateNode != null && dueDateNode.getTextContent() != null && 
                                    !dueDateNode.getTextContent().isEmpty()) {
                                    try {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        Date dueDate = dateFormat.parse(dueDateNode.getTextContent());
                                        project.setProject_due_date(dueDate);
                                    } catch (ParseException e) {
                                        logger.warn("Не удалось разобрать дату сдачи проекта: " + e.getMessage());
                                    }
                                }
                            }

                            NodeList endDateNodes = element.getElementsByTagName("project_end_date");
                            if (endDateNodes.getLength() > 0) {
                                Node endDateNode = endDateNodes.item(0);
                                if (endDateNode != null && endDateNode.getTextContent() != null && 
                                    !endDateNode.getTextContent().isEmpty()) {
                                    try {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        Date endDate = dateFormat.parse(endDateNode.getTextContent());
                                        project.setProject_end_date(endDate);
                                    } catch (ParseException e) {
                                        logger.warn("Не удалось разобрать дату окончания проекта: " + e.getMessage());
                                    }
                                }
                            }

                            Client client = em.find(Client.class, clientId);
                            if (client != null) {
                                project.setClient(client);
                            }

                            em.merge(project);
                        }
                    }
                }
            }

            // Загружаем задачи
            NodeList tasksList = doc.getElementsByTagName("task");
            for (int i = 0; i < tasksList.getLength(); i++) {
                Node node = tasksList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    NodeList idNodes = element.getElementsByTagName("task_id");
                    NodeList nameNodes = element.getElementsByTagName("task_name");
                    NodeList statusNodes = element.getElementsByTagName("task_status");
                    NodeList projectIdNodes = element.getElementsByTagName("project_id");
                    NodeList employeeIdNodes = element.getElementsByTagName("employee_id");
                    
                    if (idNodes.getLength() > 0 && nameNodes.getLength() > 0 && 
                        statusNodes.getLength() > 0 && projectIdNodes.getLength() > 0 && 
                        employeeIdNodes.getLength() > 0) {
                        
                        Node idNode = idNodes.item(0);
                        Node nameNode = nameNodes.item(0);
                        Node statusNode = statusNodes.item(0);
                        Node projectIdNode = projectIdNodes.item(0);
                        Node employeeIdNode = employeeIdNodes.item(0);
                        
                        if (idNode != null && nameNode != null && statusNode != null && 
                            projectIdNode != null && employeeIdNode != null &&
                            idNode.getTextContent() != null && !idNode.getTextContent().isEmpty() &&
                            nameNode.getTextContent() != null && !nameNode.getTextContent().isEmpty() &&
                            statusNode.getTextContent() != null && !statusNode.getTextContent().isEmpty() &&
                            projectIdNode.getTextContent() != null && !projectIdNode.getTextContent().isEmpty() &&
                            employeeIdNode.getTextContent() != null && !employeeIdNode.getTextContent().isEmpty()) {
                            
                            int taskId = Integer.parseInt(idNode.getTextContent());
                            String taskName = nameNode.getTextContent();
                            String taskStatus = statusNode.getTextContent();
                            int projectId = Integer.parseInt(projectIdNode.getTextContent());
                            int employeeId = Integer.parseInt(employeeIdNode.getTextContent());

                            Task task = em.find(Task.class, taskId);
                            if (task == null) {
                                task = new Task();
                            }
                            task.setTask_id(taskId);
                            task.setTask_name(taskName);
                            task.setTask_status(taskStatus);

                            // Загружаем описание задачи
                            NodeList descNodes = element.getElementsByTagName("task_description");
                            if (descNodes.getLength() > 0) {
                                Node descNode = descNodes.item(0);
                                if (descNode != null) {
                                    String description = descNode.getTextContent();
                                    task.setTask_description(description != null ? description : "");
                                }
                            }

                            // Загружаем дату выполнения
                            NodeList dueDateNodes = element.getElementsByTagName("due_date");
                            if (dueDateNodes.getLength() > 0) {
                                Node dueDateNode = dueDateNodes.item(0);
                                if (dueDateNode != null && dueDateNode.getTextContent() != null && 
                                    !dueDateNode.getTextContent().isEmpty()) {
                                    try {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        Date dueDate = dateFormat.parse(dueDateNode.getTextContent());
                                        task.setDueDate(dueDate);
                                    } catch (ParseException e) {
                                        logger.warn("Не удалось разобрать дату выполнения задачи: " + e.getMessage());
                                    }
                                }
                            }

                            Project project = em.find(Project.class, projectId);
                            if (project != null) {
                                task.setProject(project);
                            }

                            Employee employee = em.find(Employee.class, employeeId);
                            if (employee != null) {
                                task.setAssignedEmployee(employee);
                            }

                            em.merge(task);
                        }
                    }
                }
            }

            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Данные успешно загружены из XML файла.", "Успех", JOptionPane.INFORMATION_MESSAGE);
            logger.info("Данные успешно импортированы из XML.");
        } catch (Exception e) {
            logger.error("Ошибка при импорте из XML: " + e.getMessage(), e);
            showErrorMessage("Ошибка при загрузке из XML: " + e.getMessage());
            em.getTransaction().rollback();
        } finally {
            em.close();
            emf.close();
        }
    }

    // Добавляем метод для создания панели поиска
    private JPanel createSearchPanel(JTable table, String placeholderText) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", placeholderText);
        
        // Добавляем слушатель изменений в поле поиска
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable(searchField.getText(), table);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable(searchField.getText(), table);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable(searchField.getText(), table);
            }
        });

        searchPanel.add(new JLabel("🔍"));
        searchPanel.add(searchField);
        return searchPanel;
    }

    // Метод для фильтрации таблицы
    private void filterTable(String searchText, JTable table) {
        if (searchText.length() == 0) {
            table.setRowSorter(null);
        } else {
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);

            try {
                // Создаем фильтр, который ищет текст в колонке с именем (обычно это колонка 1)
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 1));
            } catch (PatternSyntaxException pse) {
                System.err.println("Bad regex pattern");
            }
        }
    }
}