public class Constants {
    public static final String USERS_TABLE = "users";
    public static final String USERS_USER = "user";
    public static final String USERS_PUBLIC_KEY = "publicKey";
    public static final String USERS_SESSION = "session";
    public static final String USERS_LOGGED_UNTIL = "logged_until";

    public static final String CONNECTIONS_TABLE = "connections";
    public static final String CONNECTIONS_ADRESS = "adress";
    public static final String CONNECTIONS_SHARED_KEY = "sharedKey";
    public static final String CONNECTIONS_ALIVE_UNTIL = "aliveUntil";
    public static final String CONNECTION_NOT_FOUND_MESSAGE = "not_found";


    public static final String CREATING_SESSIONS_TABLE = "creatingsessions";
    public static final String CREATING_SESSIONS_IP = "user";
    public static final String CREATING_SESSIONS_SECRET = "secret";
    public static final String CREATING_SESSIONS_ALIVE_UNTIL = "aliveUntil";


    public static final String NICKNAMES_TABLE = "nicknames";
    public static final String NICKNAMES_NICKNAME = "nickname";
    public static final String NICKNAMES_USER = "user";


    public static final String PENDING_MESSAGES_TABLE = "pendingmessages";
    public static final String PENDING_MESSAGES_TO = "to";
    public static final String PENDING_MESSAGES_FROM = "from";
    public static final String PENDING_MESSAGES_ID = "id";

    public static final int ADDITIONAL_UPDATE_TIME = 300;
    public static final long CLEAN_CONNECTIONS_TIME = 600000L;

    public static final String RESPONSE_HEADER_NAME = "result";
    public static final String RESPONSE_HEADER_OKAY = "OK";
    public static final String RESPONSE_HEADER_ERROR = "ERR";

    public static final String RESPONSE_PUBLIC_DFH_KEY_HEADER = "publicKey";

}
