package services;

import persistence.CredentialsDB;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Makes the connection from the server interface with the persistence layer
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class LoginService {

    private CredentialsDB db;


    public LoginService(SecretKey secretKey){
        db = new CredentialsDB(secretKey);
    }

    /**
     * Login user
     * @param username
     * @return
     * @throws IOException
     */
    public int userExists(String username) throws IOException {
        try {
            if (db.userExists(username)) {
                return 1;
            } else {
                return 0;
            }
        }catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Register user
     * @param username
     * @throws IOException
     */
    public void register(String username) throws IOException {
        try {
            db.registerUser(username);
        } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
