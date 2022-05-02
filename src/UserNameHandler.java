import java.security.SecureRandom;
import java.util.ArrayList;

public class UserNameHandler {

    private ArrayList<Character> appropriateChars;

    UserNameHandler(){
        appropriateChars = new ArrayList<>();
        for(int i = 'a';i <= 'z';++i){
            appropriateChars.add(Character.valueOf((char) i));
        }

        for(int i = '0';i <= '9';++i){
            appropriateChars.add(Character.valueOf((char) i));
        }
    }

    public boolean isCorrect(String userName){
        if(userName.length() != Constants.USERNAME_LENGTH)
            return false;

        for(int i = 0;i < userName.length();++i){
            if(!appropriateChars.contains(Character.valueOf(userName.charAt(i)))){
                return false;
            }
        }
        return true;
    }

    public boolean isCorrectNickname(String nickName){
        if(!(Constants.NICKNAME_MIN_LENGTH <= nickName.length() && nickName.length() <= Constants.NICKNAME_MAX_LENGTH)){
            return false;
        }

        for(int i = 0;i < nickName.length();++i){
            Character character = Character.valueOf(nickName.charAt(i));
            if(!appropriateChars.contains(character)){
                if(!('A' <= character && character <= 'Z')) // we add UPPERCASE letters as possible in nicknames
                    return false;
            }
        }

        return true;
    }

    public String generateUserName() {
        SecureRandom random = new SecureRandom();
        StringBuilder resultBuilder = new StringBuilder();

        for (int i = 0; i < Constants.USERNAME_LENGTH; ++i)
            resultBuilder.append(appropriateChars.get(random.nextInt(0, appropriateChars.size())));

        return resultBuilder.toString();
    }

    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        StringBuilder resultBuilder = new StringBuilder();

        for (int i = 0; i < Constants.SECRET_LENGTH; ++i)
            resultBuilder.append(appropriateChars.get(random.nextInt(0, appropriateChars.size())));

        return resultBuilder.toString();
    }
}
