package persistence;

import javax.crypto.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class responsible with writing and reading from the users files
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class UserDB {

    private final String userFollowersPath = "../database/user_followers.cif";
    private final String userPhotosPath = "../database/user_photos.txt";

    private SecretKey secretKey;
    private Cipher cipher;

    public UserDB(SecretKey secretKey) {
        this.secretKey = secretKey;

        try {
            cipher = Cipher.getInstance("AES");

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

    }

    /**
     * Adds new follower
     * @param currentUser
     * @param followerID
     * @throws IOException
     */
    public void addFollower(String currentUser, String followerID) throws IOException, InvalidKeyException {

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        FileOutputStream fos = new FileOutputStream(userFollowersPath, true);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(cos));

        writer.write(currentUser+":"+followerID);
        writer.newLine();
        writer.close();
        cos.close();
        fos.close();
    }

    /**
     * Removes follower
     * @param currentUser
     * @param followerID
     * @throws IOException
     */
    public void removeFollower(String currentUser, String followerID) throws IOException, InvalidKeyException {
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        FileInputStream fis = new FileInputStream(userFollowersPath);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        BufferedReader reader = new BufferedReader(new InputStreamReader(cis));


        String line = currentUser+":"+followerID;

        List<String> list = processaReader(reader).stream()
                .filter(l -> !l.contains(line))
                .collect(Collectors.toList());

        reader.close();
        cis.close();
        fis.close();

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        FileOutputStream fos = new FileOutputStream(userFollowersPath);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(cos));

        list.forEach(l -> {
            try {
                writer.write(l);
                writer.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });


        writer.close();
        cos.close();
        fos.close();
    }

    /**
     * Gets all users that user is following
     * @param currentUser
     * @return list of followers
     * @throws FileNotFoundException
     */
    public List<String> getFollowing(String currentUser) throws IOException, InvalidKeyException {
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        FileInputStream fis = new FileInputStream(userFollowersPath);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        BufferedReader reader = new BufferedReader(new InputStreamReader(cis));

        List<String> readList = processaReader(reader).stream().collect(Collectors.toList());

        List<String> result = new ArrayList<>();

        for (String s : readList) {
            if(s.split(":")[0].contains(currentUser))
                result.add(s.split(":")[1]);
        }


        reader.close();
        cis.close();
        fis.close();

        return result;
    }

    /**
     * Add photoID plus userID
     * @param currentUser
     * @param photoID
     * @throws IOException
     */
    public void addPhotoUser(String currentUser, String photoID) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(userPhotosPath, true));
        writer.write(currentUser+":"+photoID);
        writer.newLine();
        writer.close();
    }

    /**
     * Get all photos from user
     * @param userId
     * @return
     * @throws FileNotFoundException
     */
    public List<String> getAllPhotos(String userId) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(userPhotosPath));
        return processaReader(reader).stream()
                .filter(p -> p.split(":")[0].equals(userId))
                .map(p -> p.split(":")[1])
                .collect(Collectors.toList());
    }

    /**
     * Get all users following current user
     * @param currentUser
     * @return
     * @throws FileNotFoundException
     */
    public List<String> getFollowers(String currentUser) throws IOException, InvalidKeyException {

        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        FileInputStream fis = new FileInputStream(userFollowersPath);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        BufferedReader reader = new BufferedReader(new InputStreamReader(cis));

        List<String> result = processaReader(reader).stream()
                .filter(l -> l.split(":")[1].equals(currentUser))
                .map(l -> l.split(":")[0])
                .collect(Collectors.toList());

        reader.close();
        cis.close();
        fis.close();

        return result;
    }

    public List<String> processaReader (BufferedReader reader){
        return reader.lines().map(l -> l.replace("\f", "")).collect(Collectors.toList());
    }
}
