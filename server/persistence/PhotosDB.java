package persistence;

import java.io.*;

/**
 * Class responsible with writing and reading from the photos files
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class PhotosDB {
    private final String photosPath = "../database/photos.txt";

    /**
     * Get number of likes from photo
     * @param photoId photoID
     * @return number of likes
     * @throws IOException
     */
    public int getNumberLikes(String photoId) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(photosPath));

        int likes = (int) bf.lines().filter(l -> l.equals(photoId)).count();
        bf.close();
        return likes;
    }

    /**
     * Adds new photo
     * @param photosID
     * @throws IOException
     */
    public void addPhoto(String photosID) throws IOException{
        BufferedWriter bw = new BufferedWriter(new FileWriter(photosPath, true));

        bw.write(photosID);
        bw.newLine();
        bw.close();
    }

    /**
     * Likes photo
     * @param photoID
     * @return
     * @throws IOException
     */
    public int likePhoto(String photoID) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(photosPath, true));
        BufferedReader br = new BufferedReader(new FileReader(photosPath));

        if(br.lines().noneMatch(l -> l.equals(photoID))){
            br.close();
            bw.close();
            return 0;
        }else{
            bw.write(photoID);
            bw.newLine();
            bw.close();
            br.close();
            return 1;
        }
    }
}
