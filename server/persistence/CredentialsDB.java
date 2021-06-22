package persistence;

import javax.crypto.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class responsible with writing and reading from the credentials file
 *
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class CredentialsDB {

    private final String registerPath = "../database/users.cif";
    private SecretKey secretKey;


    public CredentialsDB(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Adds user data to credentials
     *
     * @param userID userID
     * @throws IOException
     */
    public void registerUser(String userID) throws InvalidKeyException, IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher  cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        FileOutputStream fos = new FileOutputStream(registerPath, true);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(cos));

        writer.write(userID + ":" + userID + ".cer");
        writer.newLine();

        writer.close();
        cos.close();
        fos.close();
    }

    /**
     * True if user is registered
     *
     * @param userID userID
     * @return true if registered
     * @throws IOException
     */
    public boolean userExists(String userID) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher  cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        FileInputStream fis = new FileInputStream(registerPath);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        BufferedReader reader = new BufferedReader(new InputStreamReader(cis));

        boolean result = false;

        List<String> list = processaReader(reader);

        for (String s : list) {
            if(s.split(":")[0].equals(userID)) {
                result = true;
                break;
            }
        }

        reader.close();
        cis.close();
        fis.close();

        return result;
    }

    public List<String> processaReader (BufferedReader reader){
        return reader.lines().map(l -> l.replace("\f", "")).collect(Collectors.toList());
    }
}
