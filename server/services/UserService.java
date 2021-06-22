package services;


import persistence.PhotosDB;
import persistence.UserDB;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Makes the connection from the server interface with the persistence layer
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class UserService {

    private final UserDB userDB;
    private final PhotosDB photosDB;
    private final String currentUser;
    private final LoginService loginService;

    public UserService(String user, SecretKey secretKey) throws IOException {
        this.currentUser = user;
        this.userDB = new UserDB(secretKey);
        this.photosDB = new PhotosDB();
        this.loginService = new LoginService(secretKey);
    }

    /**
     * Follow user
     * @param followerID
     * @return status code
     * @throws IOException
     */
    public int followUser(String followerID) throws IOException {
        if(loginService.userExists(followerID) == 0)
            return -1;
        if(viewFollowing().contains(followerID))
            return -2;

        try {
            userDB.addFollower(currentUser, followerID);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Unfollow user
     * @param followerID
     * @return status code
     * @throws IOException
     */
    public int unfollowUser(String followerID) throws IOException {
        if(loginService.userExists(followerID) != 1)
            return -1;
        if(!viewFollowing().contains(followerID))
            return -2;

        try {
            userDB.removeFollower(currentUser, followerID);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * View users that user is following
     * @return
     * @throws FileNotFoundException
     */
    public String viewFollowing() throws FileNotFoundException {
        StringBuilder sb = new StringBuilder("Segue: ");

        try {
            userDB.getFollowing(currentUser).forEach(u -> {
                sb.append(u+", ");
            });
        } catch (IOException | InvalidKeyException ioException) {
            ioException.printStackTrace();
        }

        return sb.substring(0, sb.length()-2);
    }

    /**
     * Post photo
     * @param photoID
     * @throws IOException
     */
    public void postPhoto(String photoID) throws IOException {
        userDB.addPhotoUser(currentUser, photoID);
    }

    /**
     * Show wall
     * @param number
     * @return wall
     * @throws FileNotFoundException
     */
    public List<String> showWall(int number) {
        List<String> followers = null;
        try {
            followers = userDB.getFollowing(currentUser);
        } catch (IOException | InvalidKeyException ioException) {
            ioException.printStackTrace();
        }

        List<String> wall = new ArrayList<>();

        followers.forEach(f -> {
            List<String> photos;
            try {
                photos = userDB.getAllPhotos(f);
                photos.forEach(p ->{
                    try {
                        int likes = photosDB.getNumberLikes(p);
                        wall.add("UserID: "+f+" | PhotoID: "+p+" | Likes: "+(likes-1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

        return wall.stream().limit(number).collect(Collectors.toList());
    }

    /**
     * Likes photo
     * @param photoID
     * @return 1 like
     * @throws IOException
     */
    public int likePhoto(String photoID) throws IOException {
        return photosDB.likePhoto(photoID);
    }

    /**
     * View followers
     * @return followers
     * @throws FileNotFoundException
     */
    public String viewFollowers() {
        StringBuilder sb = new StringBuilder("");
        sb.append("Seguidores de "+currentUser+":  ");
        try {
            userDB.getFollowers(currentUser).forEach(u -> {
                sb.append(u+", ");
            });
        } catch (IOException | InvalidKeyException ioException) {
            ioException.printStackTrace();
        }

        return sb.substring(0, sb.length()-2);
    }
}
