package services;

import persistence.PhotosDB;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Makes the connection from the server interface with the persistence layer
 *
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class PhotoService {

    private PhotosDB photosDB;
    private String databasePath = "../database/";


    public PhotoService() {
        photosDB = new PhotosDB();
    }


    /**
     * Adds photo
     *
     * @param inStream
     * @param photoName
     * @param photoID
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void addPhoto(ObjectInputStream inStream, String photoName, String photoID) throws IOException, ClassNotFoundException {
        String photosDir = databasePath + "photos/";

        File f = new File(photosDir + photoName);
        byte[] content = (byte[]) inStream.readObject();
        byte origDig[] = (byte[]) inStream.readObject();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA");

            if (MessageDigest.isEqual(md.digest(content), origDig)) {
                Files.write(f.toPath(), content);
                photosDB.addPhoto(photoID);
            } else {
                System.out.println("File is corrupted");
            }


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get new photo ID
     *
     * @return
     * @throws IOException
     */
    public String getNewId() throws IOException {
        String photosFile = databasePath + "photos.txt";
        BufferedReader reader = new BufferedReader(new FileReader(photosFile));

        List<String> readerList = reader.lines().collect(Collectors.toList());
        reader.close();

        if (readerList.size() == 0) {
            return String.valueOf(1);

        } else {
            List<Integer> ids = readerList.stream()
                    .filter(l -> !l.equals(""))
                    .map(l -> l.split(":")[0])
                    .mapToInt(Integer::parseInt)
                    .sorted()
                    .boxed()
                    .collect(Collectors.toList());

            return String.valueOf(ids.get(ids.size() - 1) + 1);
        }
    }
}
