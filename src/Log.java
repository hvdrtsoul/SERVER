import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void write(String message){
        LocalDateTime now = LocalDateTime.now();
        String toLog = formatter.format(now) + " " + message;
        System.out.println(toLog);

        try {
            FileWriter writer = new FileWriter("OUTSiDE_LOG.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write(toLog);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            System.out.println("FAILED TO LOG INFO: " + toLog);
        }
    }
}
