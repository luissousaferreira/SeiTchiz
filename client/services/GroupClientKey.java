package services;

import java.io.Serializable;

public class GroupClientKey implements Serializable {

    private String clientId;
    private String id;
    private byte[] key;

    public GroupClientKey(String clientId, String id, byte[] key) {
        this.clientId = clientId;
        this.id = id;
        this.key = key;
    }

    public String getClientId() {
        return clientId;
    }

    public String getId() {
        return id;
    }

    public byte[] getKey() {
        return key;
    }
}
