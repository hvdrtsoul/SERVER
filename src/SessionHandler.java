import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class SessionHandler {

    private List<Character> appropriateChars;

    SessionHandler(){
        appropriateChars = new ArrayList<>();
        for(int i = 'a';i <= 'z';++i){
            appropriateChars.add(Character.valueOf((char) i));
        }

        for(int i = '0';i <= '9';++i){
            appropriateChars.add(Character.valueOf((char) i));
        }
    }

    public String generateSession(){
        SecureRandom random = new SecureRandom();
        StringBuilder resultBuilder = new StringBuilder();

        for (int i = 0; i < Constants.SESSION_LENGTH; ++i)
            resultBuilder.append(appropriateChars.get(random.nextInt(0, appropriateChars.size())));

        return resultBuilder.toString();
    }

    public boolean checkAuth(String session, String userName){
        DatabaseHandler database = new DatabaseHandler();

        String currentUserSession = database.getUserSession(userName, System.currentTimeMillis() / 1000L);

        if(currentUserSession == Constants.CONNECTION_NOT_FOUND_MESSAGE) // if there's some error occurred or user does not exist
            return false;

        if(currentUserSession.equals(session))
            return true;
        else
            return false;
    }
}
