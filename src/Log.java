import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void write(String message){
        LocalDateTime now = LocalDateTime.now();
        System.out.println(formatter.format(now) + " " + message);
    }
}
