// Пользовательское исключение для обработки ошибок в приложении
public class MyException extends Exception {
    public MyException(String message) {
        super(message);
    }
}
