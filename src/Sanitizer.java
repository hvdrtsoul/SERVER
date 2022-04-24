
import java.util.Base64;

public class Sanitizer {

    public String sanitize(byte[] input){
        return Base64.getEncoder().encodeToString(input);
    }

    public byte[] unSanitize(String input){
        return Base64.getDecoder().decode(input);
    }
}
