
import java.math.BigInteger;

public class Connection {

    private final int ADDITIONAL_TIME = 300; // 5 minutes

    private BigInteger sharedKey;
    private int aliveUntil;

    public Connection(BigInteger sharedKey, int aliveUntil) {
        this.sharedKey = sharedKey;
        this.aliveUntil = aliveUntil;
    }

    public BigInteger getSharedKey() {
        return sharedKey;
    }

    public void keepAlive(){
        this.aliveUntil += ADDITIONAL_TIME;
        return;
    }
}
