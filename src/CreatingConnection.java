

import java.math.BigInteger;

public class CreatingConnection {

    private final int ADDITIONAL_TIME = 300; // 5 minutes

    private BigInteger sharedKey;
    private int aliveUntil;

    public CreatingConnection(BigInteger sharedKey, int aliveUntil) {
        this.sharedKey = sharedKey;
        this.aliveUntil = aliveUntil;
    }

    public BigInteger getSharedKey() {
        return sharedKey;
    }

    public int getAliveUntil() {
        return aliveUntil;
    }

    public void keepAlive(){
        this.aliveUntil += ADDITIONAL_TIME;
        return;
    }
}
