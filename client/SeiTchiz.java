import services.GroupClientKey;

import javax.crypto.*;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SeiTchiz Client
 *
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class SeiTchiz {


    private static final String pubKeysDir = "PubKeys/";
    private static String address;
    private static String serverIP;
    private static int serverPort;
    private static String trustStore;
    private static String keyStore;
    private static String keyStorePassword;
    private static String clientID;
    private static PrivateKey privateKeyClient;
    private static PublicKey publicKeyClient;

    /**
     * Process information to and from server
     *
     * @param args arguments from command line
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //SeiTchiz <serverAddress> <truststore> <keystore> <keystore-password> <clientID>

        System.out.print("\033[H\033[2J");
        System.out.flush();

        //Processa argumentos
        if (args.length == 5) {

            address = args[0];
            serverIP = address.split(":")[0];
            serverPort = Integer.parseInt(address.split(":")[1]);

            trustStore = args[1];
            keyStore = args[2];
            keyStorePassword = args[3];

            clientID = args[4];

            //System.setProperty("java.security.SecurityPermission", "putProviderProperty.SunJCE");

            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            System.setProperty("javax.net.ssl.keyStore", keyStore);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);

        } else {
            throw new IllegalArgumentException("Wrong number of arguments");
        }

        FileInputStream keyStoreStream = new FileInputStream(keyStore);
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        kstore.load(keyStoreStream, keyStorePassword.toCharArray());
        Certificate c = kstore.getCertificate(clientID);

        privateKeyClient = (PrivateKey) kstore.getKey(clientID, keyStorePassword.toCharArray());
        publicKeyClient = c.getPublicKey();


        FileInputStream trustStoreStream = new FileInputStream(trustStore);
        KeyStore tStore = KeyStore.getInstance("JKS");
        tStore.load(trustStoreStream, "password".toCharArray());

        keyStoreStream.close();
        trustStoreStream.close();

        //Ligacao ao servidor

        SocketFactory sf = SSLSocketFactory.getDefault();
        SSLSocket clientSocket = (SSLSocket) sf.createSocket(serverIP, serverPort);

        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

        int l = login(in, out, clientID);

        if (l == 1) {
            System.out.println("Registado e autenticado com Sucesso!");
        } else if (l == 2) {
            System.out.println("Autenticado com Sucesso!");
        } else if (l == -1) {
            System.out.println("Nao foi possivel registar e autenticar");
            System.exit(0);
        } else if (l == -2) {
            System.out.println("Nao foi possivel autenticar");
            System.exit(0);
        }

        System.out.println("\n--Welcome "+clientID+"--");
        Scanner sc = new Scanner(System.in);

        showMenu();
        String command;


        while (!(command = sc.nextLine()).matches("exit|e")) {
            sendCommand(command, in, out);
        }

        sc.close();
        in.close();
        out.close();
        clientSocket.close();
    }

    /**
     * Print menu
     */
    private static void showMenu() {
        System.out.println("-------------------\n");
        System.out.println("Insert one of the following:\n");
        String menu =
                "(f)ollow <userID>\n" +
                        "(u)nfollow <userID>\n" +
                        "(v)iewfollowers\n" +
                        "(p)ost <photo>\n" +
                        "(w)all <nPhotos>\n" +
                        "(l)ike <photoID>\n" +
                        "(n)ewgroup <groupID>\n" +
                        "(a)ddu <userID> <groupID>\n" +
                        "(r)emoveu <userID> <groupID>\n" +
                        "(g)info [groupID]\n" +
                        "(m)sg <groupID> <msg>\n" +
                        "(c)ollect <groupID>\n" +
                        "(h)istory <groupID>\n" +
                        "(s)how this menu\n" +
                        "(e)xit\n";

        System.out.println(menu);
    }

    /**
     * Processes login information from server
     *
     * @param in  instream
     * @param out outstream
     * @throws Exception
     */
    private static int login(ObjectInputStream in, ObjectOutputStream out, String clientID) throws Exception {
        out.writeObject(clientID);

        long nonce = in.readLong();
        String flag = (String) in.readObject();


        if (flag.equals("0")) {
            Signature s = Signature.getInstance("MD5withRSA");
            s.initSign(privateKeyClient);

            ByteBuffer bf = ByteBuffer.allocate(Long.BYTES);
            bf.putLong(nonce);

            s.update(bf.array());

            out.writeLong(nonce);
            out.writeObject(s.sign());

            out.flush();

            return Integer.parseInt((String) in.readObject());
        } else if (flag.equals("noflag")) {

            Signature s = Signature.getInstance("MD5withRSA");
            s.initSign(privateKeyClient);

            ByteBuffer bf = ByteBuffer.allocate(Long.BYTES);
            bf.putLong(nonce);
            s.update(bf.array());

            out.writeObject(s.sign());
            out.flush();

            return Integer.parseInt((String) in.readObject());
        }
        return 0;
    }


    /**
     * Sends comand to server
     *
     * @param command command
     * @param in      instream
     * @param out     outstream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void sendCommand(String command, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {

        String arg1 = "";
        String arg2 = "";
        String arg3 = "";

        String[] commandParsed = command.split(" ", 3);

        if (commandParsed.length == 3) {
            arg1 = commandParsed[0];
            arg2 = commandParsed[1];
            arg3 = commandParsed[2];
        } else if (commandParsed.length == 2) {
            arg1 = commandParsed[0];
            arg2 = commandParsed[1];
        } else {
            arg1 = commandParsed[0];
        }

        switch (arg1) {
            case "f":
            case "follow":
            case "u":
            case "unfollow":
            case "l":
            case "like":
                if (commandParsed.length != 2) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    out.writeObject(arg1+" "+arg2);
                    out.flush();
                    System.out.println(in.readObject());
                    break;
                }
            case "h":
            case "history":
                if (commandParsed.length != 2) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    readMessages(clientID, out, in, arg1, arg2);
                    break;
                }
            case "n":
            case "newgroup":
                if (commandParsed.length != 2) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    out.writeObject(arg1 + " " + arg2);
                    out.writeObject(genFirstGroupKey());
                    out.flush();
                    System.out.println(in.readObject());
                    break;
                }
            case "a":
            case "addu":
                if (commandParsed.length != 3) {
                    System.out.println("Comando mal escrito!");
                } else {
                    addUserToGroup(clientID, out, in, arg1, arg2, arg3);
                }
                break;
            case "r":
            case "removeu":
                if (commandParsed.length != 3) {
                    System.out.println("Comando mal escrito!");
                } else {
                    removeUserFromGroup(clientID, out, in, arg1, arg2, arg3);
                }
                break;
            case "m":
            case "msg":
                if (commandParsed.length != 3) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    message(clientID, out, in, arg1, arg2, arg3);
                    break;
                }
            case "c":
            case "collect":
                if (commandParsed.length != 2) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    readMessages(clientID, out, in, arg1, arg2);
                    break;
                }
            case "w":
            case "wall":
                if (commandParsed.length != 2) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    out.writeObject(arg1 + " " + arg2);
                    out.flush();

                    @SuppressWarnings("unchecked")
                    List<String> answer = (List<String>) in.readObject();
                    answer.forEach(System.out::println);
                    break;
                }
            case "v":
            case "viewfollowers":
                if (commandParsed.length != 1) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    out.writeObject(arg1);
                    out.flush();
                    System.out.println(in.readObject());
                    break;
                }

            case "p":
            case "post":
                if (commandParsed.length != 2) {
                    System.out.println("Comando mal escrito!");
                    break;
                } else {
                    sendPhoto(arg1, arg2, out);
                    System.out.println(in.readObject());
                }
                break;
            case "g":
            case "ginfo":
                if (arg2.equals("")) {
                    out.writeObject(arg1 + " " + "0");
                    out.flush();
                } else {
                    out.writeObject(arg1 + " " + arg2);
                    out.flush();
                }
                System.out.println(in.readObject());
                break;
            case "s":
            case "show menu":
                showMenu();
                break;
            default:
                System.out.println("Comando desconhecido");
                break;
        }
    }

    /**
     * Read messages from server
     * @param clientID
     * @param out
     * @param in
     * @param arg1
     * @param arg2
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private static void readMessages(String clientID, ObjectOutputStream out, ObjectInputStream in, String arg1, String arg2) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        out.writeObject("getParticipants " + arg2);
        out.flush();

        List<String> participants = (List<String>) in.readObject();

        if (participants.get(0).equals("nope")) {
            System.out.println("Grupo " + arg2 + " nao existe");
            return;
        }

        out.writeObject("getGroupKeyUser " + arg2 + " " + clientID);
        out.flush();
        String id = (String) in.readObject();
        GroupClientKey gck = (GroupClientKey) in.readObject();

        if(gck.getId().equals("nope")){
            System.out.println(clientID+" nao pertence ao grupo "+arg2);
            return;
        }

        out.writeObject(arg1 + " " + arg2);
        out.flush();

        String answer = (String) in.readObject();

        if (answer.equals("-2")) {
            System.out.println("Utilizador " + clientID + " nao esta no grupo");
            return;
        }
        if (answer.equals("-3")) {
            System.out.println("Nao ha mensagens por ler");
            return;
        }
        if (answer.equals("1")) {
            byte[] answerBytes = (byte[]) in.readObject();
            String[] processedAnswer = new String(answerBytes).split("\n");

            List<String> result = new ArrayList<>();

            List<GroupClientKey> userKeys = getUserGroupKeys(clientID, arg2, out, in);

            for (String line : processedAnswer) {

                String[] split = line.split(":");
                String part1 = split[0];

                for (GroupClientKey userKey : userKeys) {
                    SecretKey groupKey = (SecretKey) decipherGroupKey(userKey.getKey(), privateKeyClient);
                    Cipher c = Cipher.getInstance("AES");
                    c.init(Cipher.DECRYPT_MODE,groupKey);

                    if(userKey.getId().equals(split[2])){
                        byte[] encryptedMessage = Base64.getMimeDecoder().decode(split[1].substring(1));
                        byte[] eFinal = c.doFinal(encryptedMessage);
                        String part2 = new String(eFinal);
                        result.add(part1 + ": " + part2);
                    }
                }
            }

            StringBuilder sb = new StringBuilder("");

            result.forEach(r -> {
                sb.append(r + "\n");
            });

            System.out.println("\n" + sb.toString());
            return;
        }
    }

    /**
     * Get User group keys
     * @param clientID
     * @param groupID
     * @param out
     * @param in
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static List<GroupClientKey> getUserGroupKeys(String clientID, String groupID, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
        out.writeObject("getUserKeys " + groupID + " " + clientID);
        out.flush();

        List<GroupClientKey> userGroupKeys = (List<GroupClientKey>) in.readObject();
        return userGroupKeys;
    }

    /**
     * Send message
     * @param clientID
     * @param out
     * @param in
     * @param arg1
     * @param arg2
     * @param arg3
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private static void message(String clientID, ObjectOutputStream out, ObjectInputStream in, String arg1, String arg2, String arg3) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        out.writeObject("getGroupKeyUser " + arg2 + " " + clientID);

        String id = (String) in.readObject();

        if(id.equals("")){
            System.out.println("Grupo "+arg2+" nao existe");
            return;
        }

        GroupClientKey gck = (GroupClientKey) in.readObject();

        byte[] byteGroupKey = gck.getKey();

        Key groupKey = decipherGroupKey(byteGroupKey, privateKeyClient);

        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, groupKey);

        byte[] messageBytes = arg3.getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = c.doFinal(messageBytes);

        out.writeObject(arg1 + " " + arg2 + " " + id);
        out.writeObject(encrypted);
        out.flush();

        System.out.println(in.readObject());
    }

    /**
     * Decipher group key
     * @param key
     * @param privateKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     */
    private static Key decipherGroupKey(byte[] key, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.UNWRAP_MODE, privateKey);
        Key finalKey = c.unwrap(key, "AES", Cipher.SECRET_KEY);
        return finalKey;
    }

    /**
     * Remove user from group
     * @param currentUser
     * @param out
     * @param in
     * @param arg1
     * @param arg2
     * @param arg3
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    private static void removeUserFromGroup(String currentUser, ObjectOutputStream out, ObjectInputStream in, String arg1, String arg2, String arg3)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException {

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        Key newKey = kg.generateKey();

        byte[] ownerGroupKey = cipherKey(publicKeyClient, newKey);

        out.writeObject(arg1 + " " + arg2 + " " + arg3);
        String id = (String) in.readObject();
        out.writeObject(ownerGroupKey);
        out.flush();
        String answer = (String) in.readObject();

        if (answer.equals("-1")) {
            System.out.println("Grupo " + arg3 + " nao existe");
            return;
        }
        if (answer.equals("-2")) {
            System.out.println(currentUser + " nao eh dono do grupo " + arg3);
            return;
        }
        if (answer.equals("-3")) {
            System.out.println(arg2 + " nao pertence ao grupo " + arg3);
            return;
        }
        if (answer.equals("-4")) {
            System.out.println(arg2 + " nao esta registado");
            return;
        }

        out.writeObject("getParticipants" + " " + arg3);
        out.flush();

        List<GroupClientKey> usersGroupKeys = newMembersGroupKeys((List<String>) in.readObject(), newKey, id);

        out.writeObject("usersGroupKeys " + arg3);
        out.writeObject(usersGroupKeys);
        out.flush();

        if (answer.equals("1")) {
            System.out.println(arg2 + " removido com sucesso do grupo " + arg3);
            return;
        }
    }


    /**
     * Add user to group
     * @param currentUser
     * @param out
     * @param in
     * @param arg1
     * @param arg2
     * @param arg3
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    private static void addUserToGroup(String currentUser, ObjectOutputStream out, ObjectInputStream in, String arg1, String arg2, String arg3) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        Key newKey = kg.generateKey();

        byte[] ownerGroupKey = cipherKey(publicKeyClient, newKey);

        out.writeObject(arg1 + " " + arg2 + " " + arg3);
        String id = (String) in.readObject();
        out.writeObject(ownerGroupKey);
        out.flush();
        String answer = (String) in.readObject();

        if (answer.equals("-1")) {
            System.out.println("Grupo " + arg3 + " nao existe");
            return;
        }
        if (answer.equals("-2")) {
            System.out.println(currentUser + "nao eh dono do grupo " + arg3);
            return;
        }
        if (answer.equals("-3")) {
            System.out.println(arg2 + " ja se encontra no grupo " + arg3);
            return;
        }
        if (answer.equals("-4")) {
            System.out.println(arg2 + " nao esta registado");
            return;
        }

        out.writeObject("getParticipants" + " " + arg3);
        out.flush();
        List<String> participants = (List<String>) in.readObject();

        List<GroupClientKey> usersGroupKeys = newMembersGroupKeys(participants, newKey, id);

        out.writeObject("usersGroupKeys " + arg3);
        out.writeObject(usersGroupKeys);
        out.flush();

        if (answer.equals("1")) {
            System.out.println(arg2 + " adicionado com sucesso ao grupo " + arg3);
            return;
        }
    }

    /**
     * Generate new keys for the members
     * @param groupMembers
     * @param newGroupKey
     * @param id
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private static List<GroupClientKey> newMembersGroupKeys(List<String> groupMembers, Key newGroupKey, String id) throws IOException, NoSuchAlgorithmException {

        List<String> sortedGroupMembers = groupMembers
                .stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        List<PublicKey> membersPublicKeys = getAllPublicKeys(sortedGroupMembers);

        List<byte[]> keyList = new ArrayList<>();

        membersPublicKeys.forEach(key -> {
            try {
                Cipher c = Cipher.getInstance("RSA");
                c.init(Cipher.WRAP_MODE, key);

                keyList.add(c.wrap(newGroupKey));

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        });

        List<GroupClientKey> groupClientKeyList = new ArrayList<>();

        for (int i = 0; i < membersPublicKeys.size(); i++) {
            groupClientKeyList.add(new GroupClientKey(sortedGroupMembers.get(i), String.valueOf(Integer.parseInt(id) + 1), keyList.get(i)));
        }

        return groupClientKeyList;
    }

    /**
     * Get all public keys from group members
     * @param groupMembers
     * @return
     * @throws IOException
     */
    private static List<PublicKey> getAllPublicKeys(List<String> groupMembers) throws IOException {
        List<String> paths = Files.walk(Paths.get("PubKeys"))
                .map(l -> l.getFileName().toString())
                .filter(l -> !l.equals("truststore.ts"))
                .filter(l -> !l.equals("PubKeys"))
                .filter(l -> groupMembers.contains(l.substring(0, l.length() - 4)))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());



        List<PublicKey> result = new ArrayList<>();

        paths.forEach(file -> {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(pubKeysDir + file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            CertificateFactory cf = null;
            try {
                cf = CertificateFactory.getInstance("X509");
            } catch (CertificateException e) {
                e.printStackTrace();
            }
            try {
                Certificate cert = cf.generateCertificate(fis);
                result.add(cert.getPublicKey());
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        });

        return result;
    }

    /**
     * Sends photo to Server
     *
     * @param arg1      command
     * @param photoPath path
     * @param out       outstream
     * @throws IOException
     */
    private static void sendPhoto(String arg1, String photoPath, ObjectOutputStream out) throws IOException {

        out.writeObject(arg1);
        out.flush();

        String photoName = photoPath.split("/")[photoPath.split("/").length - 1];
        out.writeObject(photoName);
        out.flush();

        File f = new File(photoPath);

        byte[] content = Files.readAllBytes(f.toPath());

        try {
            MessageDigest md = MessageDigest.getInstance("SHA");

            out.writeObject(content);
            out.writeObject(md.digest(content));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        out.flush();
    }


    /**
     * Cipher key
     * @param publicKey
     * @param secretKey
     * @return
     */
    private static byte[] cipherKey(PublicKey publicKey, Key secretKey) {
        Cipher c = null;
        try {
            c = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            c.init(Cipher.WRAP_MODE, publicKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try {
            return c.wrap(secretKey);
        } catch (IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
        }
        //nao deve chegar aqui
        return new byte[0];
    }

    /**
     * Generate first group key
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] genFirstGroupKey() throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        Key secretKey = kg.generateKey();

        Cipher c = null;
        try {
            c = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            c.init(Cipher.WRAP_MODE, publicKeyClient);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try {
            return c.wrap(secretKey);
        } catch (IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
        }
        //nao deve chegar aqui
        return new byte[0];
    }
}
