import services.*;

import javax.crypto.SecretKey;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.security.sasl.AuthenticationException;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Random;
import services.GroupClientKey;

/**
 * SeiTchiz Server
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class SeiTchizServer {

    private static final String databasePath = "../database/";
    private static final String pubKeysPath = "PubKeys/";

    private PrivateKey privateKeyServer;
    private PublicKey publicKeyServer;

    private SecretKey secretKey;


    /**
     * Main function
     *
     * @param args arguments from command line (port)
     * @throws Exception
     */
    public static void main(String[] args) {
        //<port> <keystore> <keystore-password>

        System.out.println("Servidor SeiTchiz");

        if (args.length == 3) {

            int port = Integer.parseInt(args[0]);
            String keyStorePath = args[1];
            String keyStorePassword = args[2];

            System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");
            System.setProperty("javax.net.ssl.keyStore", keyStorePath);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);

            SeiTchizServer server = new SeiTchizServer(keyStorePath, keyStorePassword);
            server.startServer(port);
        }
    }

    private SeiTchizServer(String keyStorePath, String keyStorePassword) {

        try {
            FileInputStream keyStoreStream = new FileInputStream(keyStorePath);
            KeyStore ks = KeyStore.getInstance("JCEKS");

            ks.load(keyStoreStream, keyStorePassword.toCharArray());

            privateKeyServer = (PrivateKey) ks.getKey("server", keyStorePassword.toCharArray());

            Certificate cert = ks.getCertificate("server");
            publicKeyServer = cert.getPublicKey();

            secretKey = (SecretKey) ks.getKey("secKey", keyStorePassword.toCharArray());

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException
                | CertificateException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts server
     *
     * @param porto socket port
     */
    public void startServer(int porto) {

        verifyFiles();

        try {

            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(porto);


            System.out.println("Servidor iniciado no porto " + porto);

            while (true) {
                try {

                    ServerThread newServerThread = new ServerThread(ss.accept());
                    newServerThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Each thread talks to a client
     */
    class ServerThread extends Thread {

        private Socket socket = null;

        ServerThread(Socket inSoc) throws IOException {
            socket = inSoc;
            System.out.println("thread do java.client.server para cada cliente");
        }

        /**
         * Run thread
         */
        public void run() {
            try {
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

                String userID = (String) inStream.readObject();

                login(userID, outStream, inStream);

                String request;
                while (!(request = (String) inStream.readObject()).equals("exit|e")) {
                    outStream.flush();
                    String[] splitRequest = request.split(" ");

                    runCommand(splitRequest, outStream, inStream, userID);
                }

                outStream.close();
                inStream.close();

                socket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /**
         * Run command sent from client
         *
         * @param request     Request received from the client
         * @param out         outstream
         * @param inStream    instream
         * @param currentUser Current user connected
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void runCommand(String[] request, ObjectOutputStream out, ObjectInputStream inStream, String currentUser) throws IOException, ClassNotFoundException {
            UserService userService = new UserService(currentUser, secretKey);
            PhotoService photoService = new PhotoService();
            GroupService groupService;

            String command = request[0];

            switch (command) {
                case "f":
                case "follow":
                    int a = userService.followUser(request[1]);
                    if (a == -1){
                        out.writeObject("Utilizador " + request[1] + " nao existe");
                    }else if(a == -2){
                        out.writeObject("Ja segue " + request[1]);
                    } else{
                        out.writeObject("Passou a seguir " + request[1]);
                    }
                    out.flush();
                    break;
                case "u":
                case "unfollow":
                    a = userService.unfollowUser(request[1]);
                    if (a == -1){
                        out.writeObject("Utilizador " + request[1] + " nao existe");
                    }else if (a == -2){
                        out.writeObject("Nao segue " + request[1]);
                    } else{
                        out.writeObject("Deixou de seguir " + request[1]);
                    }
                    out.flush();
                    break;
                case "v":
                case "viewfollowers":
                    out.writeObject(userService.viewFollowers());
                    out.flush();
                    break;
                case "p":
                case "post":
                    String photoName = (String) inStream.readObject();
                    String photoID = photoService.getNewId();

                    photoService.addPhoto(inStream, photoName, photoID);
                    userService.postPhoto(photoID);
                    out.writeObject("Foto postada com sucesso");
                    out.flush();
                    break;
                case "w":
                case "wall":
                    out.writeObject(userService.showWall(Integer.parseInt(request[1])));
                    out.flush();
                    break;
                case "l":
                case "like":
                    if (userService.likePhoto(request[1]) == 0)
                        out.writeObject("Fotografia nao existe");
                    else
                        out.writeObject("Gostou da fotografia '" + request[1] + "'");
                    out.flush();
                    break;
                case "n":
                case "newgroup":
                    groupService = new GroupService(secretKey, request[1]);
                    byte[] secretGroupKey = (byte[]) inStream.readObject();
                    if (groupService.newGroup(currentUser, secretGroupKey) == 0)
                        out.writeObject("Grupo com o id '" + request[1] + "' ja existe");
                    else
                        out.writeObject("Grupo '" + request[1] + "' criado com sucesso");
                    out.flush();
                    break;
                case "a":
                case "addu":
                    groupService = new GroupService(secretKey, request[2]);
                    groupService.getGroupId(out);
                    byte[] newGroupKey = (byte[]) inStream.readObject();

                    int answer = groupService.addUser(newGroupKey, currentUser, request[1]);

                    if (answer == -1)
                        out.writeObject("-1");
                    if (answer == -2)
                        out.writeObject("-2");
                    if (answer == -3)
                        out.writeObject("-3");
                    if (answer == -4)
                        out.writeObject("-4");
                    if (answer == 1)
                        out.writeObject("1");
                    out.flush();
                    break;
                case "r":
                case "removeu":
                    groupService = new GroupService(secretKey, request[2]);
                    groupService.getGroupId(out);
                    newGroupKey = (byte[]) inStream.readObject();

                    answer = groupService.removeUser(newGroupKey, currentUser, request[1]);

                    if (answer == -1)
                        out.writeObject("-1");
                    if (answer == -2)
                        out.writeObject("-2");
                    if (answer == -3)
                        out.writeObject("-3");
                    if (answer == -4)
                        out.writeObject("-4");
                    if (answer == 1)
                        out.writeObject("1");
                    out.flush();
                    break;
                case "g":
                case "ginfo":
                    groupService = new GroupService(secretKey, request[1]);
                    if (request[1].equals("0")) {
                        out.writeObject(groupService.getGroupInfoDefault(currentUser));
                        out.flush();
                    } else {
                        String result = groupService.getGroupInfo(currentUser, request[1]);
                        if (result.equals("-1"))
                            out.writeObject("Grupo nao existe");
                        else if (result.equals("-2"))
                            out.writeObject(currentUser + " nao eh dono do grupo " + request[1]);
                        else
                            out.writeObject(result);
                        out.flush();
                    }
                    break;
                case "m":
                case "msg":
                    groupService = new GroupService(secretKey, request[1]);
                    byte[] message = (byte[]) inStream.readObject();
                    answer = groupService.sendMessage(currentUser, request[1], message, request[2]);

                    if (answer == -1)
                        out.writeObject("Grupo nao existe");
                    if (answer == -2)
                        out.writeObject("Utilizador " + currentUser + " nao esta no grupo");
                    if (answer == 1)
                        out.writeObject("Mensagem enviada com sucesso");
                    out.flush();
                    break;
                case "c":
                case "collect":
                    groupService = new GroupService(secretKey, request[1]);
                    String result = groupService.collect(currentUser, request[1]);
                    if (result.equals("-1")){
                        out.writeObject("-1");
                    }else if(result.equals("-2")){
                        out.writeObject("-2");
                    }else if (result.equals("-3")){
                        out.writeObject("-3");
                    } else{
                        out.writeObject("1");
                        out.writeObject(result.getBytes());
                    }
                    out.flush();
                    break;
                case "h":
                case "history":
                    groupService = new GroupService(secretKey, request[1]);
                    result = groupService.getHistory(currentUser);
                    if (result.equals("-1")){
                        out.writeObject("-1");
                    }else if(result.equals("-2")){
                        out.writeObject("-2");
                    }else if (result.equals("-3")){
                        out.writeObject("-3");
                    } else{
                        out.writeObject("1");
                        out.writeObject(result.getBytes());
                    }
                    out.flush();
                    break;
                case "getParticipants":
                    groupService = new GroupService(secretKey, request[1]);
                    out.writeObject(groupService.getGroupParticipants());
                    out.flush();
                    break;
                case "usersGroupKeys":
                    groupService = new GroupService(secretKey, request[1]);
                    groupService.saveKeys((List<GroupClientKey>) inStream.readObject(), request[1]);
                    break;
                case "getGroupKeyUser":
                    groupService = new GroupService(secretKey, request[1]);
                    String user = request[2];
                    groupService.getGroupId(out);
                    out.writeObject(groupService.getGroupKeyUser(user));
                    out.flush();
                    break;
                case "getUserKeys":
                    groupService = new GroupService(secretKey, request[1]);
                    out.writeObject(groupService.getUserKeys(request[2]));
                    out.flush();
            }
        }

        /**
         * Login helper function
         *
         * @param userID userID
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void login(String userID, ObjectOutputStream outputStream, ObjectInputStream inputStream) throws IOException, ClassNotFoundException {

            LoginService loginService = new LoginService(secretKey);
            int flag = loginService.userExists(userID);

            long nonce = new Random().nextLong();


            if (flag == 0) {
                outputStream.writeLong(nonce);
                outputStream.writeObject(String.valueOf(flag));

                long receivedNonce = inputStream.readLong();

                if (receivedNonce != nonce) {
                    outputStream.writeObject("-1");
                    outputStream.flush();
                    throw new AuthenticationException();
                }

                byte[] sign = (byte[]) inputStream.readObject();

                FileInputStream fis = new FileInputStream(pubKeysPath + userID + ".cer");

                try {

                    CertificateFactory cf = CertificateFactory.getInstance("X509");
                    Certificate cert = cf.generateCertificate(fis);
                    PublicKey userPublicKey = cert.getPublicKey();

                    Signature s = Signature.getInstance("MD5withRSA");
                    s.initVerify(userPublicKey);

                    ByteBuffer bf = ByteBuffer.allocate(Long.BYTES);
                    bf.putLong(receivedNonce);

                    s.update(bf.array());

                    if (!s.verify(sign)) {
                        outputStream.writeObject("-1");
                        outputStream.flush();
                        throw new AuthenticationException();
                    } else {
                        loginService.register(userID);
                        outputStream.writeObject("1");
                        outputStream.flush();
                    }
                } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
                }
            } else {
                outputStream.writeLong(nonce);
                outputStream.writeObject("noflag");


                byte[] sign = (byte[]) inputStream.readObject();


                FileInputStream fis = new FileInputStream(pubKeysPath + userID + ".cer");

                try {

                    CertificateFactory cf = CertificateFactory.getInstance("X509");
                    Certificate cert = cf.generateCertificate(fis);
                    PublicKey userPublicKey = cert.getPublicKey();

                    Signature s = Signature.getInstance("MD5withRSA");
                    s.initVerify(userPublicKey);

                    ByteBuffer bf = ByteBuffer.allocate(Long.BYTES);
                    bf.putLong(nonce);

                    s.update(bf.array());

                    if (!s.verify(sign)) {
                        outputStream.writeObject("-2");
                        outputStream.flush();
                        throw new AuthenticationException();
                    } else {
                        outputStream.writeObject("2");
                        outputStream.flush();
                    }
                } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void verifyFiles() {
        try {

            if (!Files.exists(Paths.get(databasePath)))
                new File(databasePath).mkdirs();
            if (!Files.exists(Paths.get(databasePath + "groups/")))
                new File(databasePath + "groups").mkdirs();
            if (!Files.exists(Paths.get(databasePath + "photos/")))
                new File(databasePath + "photos").mkdirs();

            if(!Files.exists(Paths.get(databasePath + "users.cif"))) {
                PrintWriter w = new PrintWriter(databasePath + "users.cif");
                w.close();
            }
            if(!Files.exists(Paths.get(databasePath + "user_followers.cif"))) {
                PrintWriter w = new PrintWriter(databasePath + "user_followers.cif");
                w.close();
            }
            if(!Files.exists(Paths.get(databasePath + "user_groups.cif"))) {
                PrintWriter w = new PrintWriter(databasePath + "user_groups.cif");
                w.close();
            }
            if(!Files.exists(Paths.get(databasePath + "owner_groupos.cif"))) {
                PrintWriter w = new PrintWriter(databasePath + "owner_groups.cif");
                w.close();
            }
            if (!Files.exists(Paths.get(databasePath + "user_photos.txt")))
                new File(databasePath + "user_photos.txt").createNewFile();

            if (!Files.exists(Paths.get(databasePath + "photos.txt")))
                new File(databasePath + "photos.txt").createNewFile();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}


