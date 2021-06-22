package persistence;

import services.GroupClientKey;

import javax.crypto.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class responsible with writing and reading from the groups files
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class GroupsDB {

    private final String groupsPath = "../database/groups";
    private String groupDirPath = groupsPath+File.separator;

    private String messagesPath;
    private String groupInfoPath;
    private String membersPath;
    private String historyPath;
    private String usersKeysPath;
    private String usersInfo;
    private String userGroupsPath;
    private String groupOwnersPath;

    private SecretKey secretKey;
    private Cipher cipher;

    private String groupID;

    public GroupsDB(){

    }

    public GroupsDB(SecretKey secretKey, String groupID) throws IOException {

        if(groupID == null && secretKey == null)
            return;

        this.secretKey = secretKey;
        this.groupID = groupID;

        try {
            cipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        if (!Files.exists(Paths.get(groupDirPath+groupID))){
            new File(groupDirPath+groupID).mkdirs();
        }
        userGroupsPath = "../database/user_groups.cif";
        groupOwnersPath = "../database/owner_groups.cif";
        messagesPath = groupDirPath+groupID+File.separator+"messages.cif";
        groupInfoPath = groupDirPath+groupID+File.separator+ "groupInfo.txt";
        membersPath = groupDirPath+groupID+File.separator+"members.cif";
        historyPath = groupDirPath+groupID+File.separator+ "history.cif";
        usersKeysPath = groupDirPath+groupID+File.separator+ "usersKeys.txt";
        usersInfo = groupDirPath+groupID+File.separator+ "usersInfo";

        if (!Files.exists(Paths.get(usersInfo)))
            new File(usersInfo).mkdir();

        if (!Files.exists(Paths.get(messagesPath)))
            new File(messagesPath).createNewFile();

        if (!Files.exists(Paths.get(historyPath)))
            new File(historyPath).createNewFile();

        if (!Files.exists(Paths.get(membersPath)))
            new File(membersPath).createNewFile();

        if (!Files.exists(Paths.get(groupInfoPath)))
            new File(groupInfoPath).createNewFile();

        if (!Files.exists(Paths.get(usersKeysPath)))
            new File(usersKeysPath).createNewFile();

    }

    /**
     * Creates group
     * @param owner group owner
     * @throws IOException
     */
    public void createGroup(String owner, byte[] groupKey) throws IOException {
        //BufferedWriter w1 = writeToFile(groupInfoPath);
        BufferedWriter w1 = new BufferedWriter(new FileWriter(groupInfoPath));
        w1.write(groupID+":"+0+":"+Base64.getEncoder().encodeToString(groupKey));
        w1.newLine();
        w1.close();

        BufferedWriter w2 = writeToFile(membersPath);
        w2.write(owner);
        w2.close();

        BufferedWriter w3 = new BufferedWriter(new FileWriter(usersKeysPath));
        w3.write(owner+","+0+","+ Base64.getEncoder().encodeToString(groupKey));
        w3.close();



        saveUsersKeysHistory();
        addToOwnerPartGroups(owner, groupOwnersPath);
        addToOwnerPartGroups(owner, userGroupsPath);
    }

    /**
     * Update group key
     * @param groupKey
     * @param user
     * @throws IOException
     */
    public void updateGroupKey(byte[] groupKey, String user) throws  IOException{
        //BufferedReader br = readFromFile(groupInfoPath);
        BufferedReader br = new BufferedReader(new FileReader(groupInfoPath));
        String line = br.readLine();
        br.close();
        int newID = Integer.parseInt(getGroupId())+1;

        BufferedWriter w1 = new BufferedWriter(new FileWriter(groupInfoPath));
        //BufferedWriter w1 = writeToFile(groupInfoPath);
        w1.write(groupID+":"+newID+":"+ Base64.getEncoder().encodeToString(groupKey));
        w1.close();
    }

    /**
     * Write to file
     * @param path
     * @return
     */
    private BufferedWriter writeToFile(String path){
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(cos));

        return writer;
    }

    /**
     * Read from file
     * @param path
     * @return
     */
    private BufferedReader readFromFile(String path){
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CipherInputStream cos = new CipherInputStream(fis, cipher);
        BufferedReader reader = new BufferedReader(new InputStreamReader(cos));

        return reader;
    }


    /**
     * Checks if group exists
     * @return true if exists
     */
    public boolean exist(String groupID) {
        File f = new File(groupDirPath+"/"+groupID);
        return f.exists();
    }

    /**
     * Check if is owner
     * @param currentUser
     * @return
     * @throws IOException
     */
    public boolean isOwner(String currentUser) throws IOException {
        BufferedReader reader = readFromFile(membersPath);
        boolean result = reader.readLine().split(":")[0].equals(currentUser);
        reader.close();
        return result;
    }


    /**
     * Get participants
     * @return
     * @throws IOException
     */
    public List<String> getParticipants() throws IOException {
        BufferedReader reader = readFromFile(membersPath);
        List<String> result = Arrays.asList(reader.readLine().split(":").clone());
        return result;
    }


    /**
     * Add to group
     * @param userID
     * @throws IOException
     */
    public void addToGroup(String userID) throws IOException {
        BufferedReader reader = readFromFile(membersPath);
        String members = reader.readLine();
        reader.close();

        BufferedWriter writer = writeToFile(membersPath);
        writer.write(members+":"+userID);
        writer.close();


        addToOwnerPartGroups(userID, userGroupsPath);
    }

    /**
     * Save new keys
     * @param userKeys
     * @throws IOException
     */
    public void saveNewKeys(List<GroupClientKey> userKeys) throws IOException {
        //BufferedWriter writer = writeToFile(usersKeysPath);

        BufferedWriter writer = new BufferedWriter(new FileWriter(usersKeysPath));

        for (GroupClientKey userKey : userKeys) {

            String clientId = userKey.getClientId();
            String idKey = userKey.getId();
            byte[] key = userKey.getKey();

            writer.write(clientId+","+idKey+","+Base64.getEncoder().encodeToString(key));
            writer.newLine();
        }
        writer.close();

        BufferedWriter w2 = new BufferedWriter(new FileWriter(groupInfoPath));

        for (GroupClientKey userKey : userKeys) {


            String clientId = userKey.getClientId();
            String idKey = userKey.getId();
            byte[] key = userKey.getKey();
            if(isOwner(clientId)){
                BufferedReader br = new BufferedReader(new FileReader(groupInfoPath));
                String line = br.readLine();
                br.close();
                w2.write(groupID+":"+idKey+":"+Base64.getEncoder().encodeToString(key));
            }
        }
        w2.close();

        saveUsersKeysHistory();
    }

    /**
     * Save users keys history
     * @throws IOException
     */
    public void saveUsersKeysHistory() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(usersKeysPath));

        List<String> users = br.lines().collect(Collectors.toList());
        br.close();

        createUsersFiles(users);

        users.forEach(u -> {
            String filePath = usersInfo+File.separator + u.split(",")[0] + ".txt";
            try {
                updateUserFile(filePath, u);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
    }

    /**
     * Create users files
     * @param users
     */
    private void createUsersFiles(List<String> users){
        users.forEach(u -> {

            String filePath = usersInfo+File.separator + u.split(",")[0] + ".txt";

            if (!Files.exists(Paths.get(filePath))) {
                try {
                    new File(filePath).createNewFile();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

    /**
     * Update user fike
     * @param filePath
     * @param data
     * @throws IOException
     */
    private void updateUserFile(String filePath, String data) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        List<String> content = br.lines().collect(Collectors.toList());
        br.close();

        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));

        content.forEach(c -> {
            try {
                bw.write(c);
                bw.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        bw.write(data);
        bw.newLine();
        bw.close();
    }

    /**
     * Get group id
     * @return
     * @throws IOException
     */
    public String getGroupId() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(groupInfoPath));
        String id = br.readLine().split(":")[1];
        br.close();
        return id;
    }

    /**
     * Remove from group
     * @param userID
     * @throws IOException
     */
    public void removeFromGroup(String userID) throws IOException {
        BufferedReader br = readFromFile(membersPath);

        String members = br.readLine();

        String[] listMembers = members.split(":");
        StringBuilder sb = new StringBuilder("");
        sb.append(listMembers[0]);

        for (int i = 1; i < listMembers.length; i++) {
            if(!listMembers[i].equals(userID))
                sb.append(":"+listMembers[i]);
        }

        BufferedWriter bw = writeToFile(membersPath);
        bw.write(sb.toString());
        bw.close();

        removeFromGroupParticipants(userID);
    }

    /**
     * Remove from group participants
     * @param clientID
     * @throws IOException
     */
    private void removeFromGroupParticipants(String clientID) throws IOException {
        BufferedReader br = readFromFile(userGroupsPath);
        List<String> result = br.lines().collect(Collectors.toList());
        br.close();


        List<String> newResult = new ArrayList<>();
        result.forEach(l -> {
            newResult.add(l);
        });
        newResult.remove(groupID+":"+clientID);

        BufferedWriter bw = writeToFile(userGroupsPath);

        newResult.forEach(l -> {
            try {
                bw.write(l);
                bw.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        bw.close();
    }

    /**
     * Get groups of which user is owner
     * @param currentUser user
     * @return group list
     * @throws FileNotFoundException
     */
    public List<String> getOwnerGroups(String currentUser) throws IOException {
        BufferedReader reader = readFromFile(groupOwnersPath);

        List<String> fromStream = reader.lines().collect(Collectors.toList());

        List<String> result = fromStream.stream()
                .filter(l -> l.split(":")[1].contains(currentUser))
                .map(l -> l.split(":")[0])
                .collect(Collectors.toList());
        reader.close();

        return result;
    }

    /**
     * Get groups of which user is participant
     * @param currentUser user
     * @return group list
     * @throws FileNotFoundException
     */
    public List<String> getParticipantGroups(String currentUser) throws IOException {
        BufferedReader br = readFromFile(userGroupsPath);

        List<String> fromStream = br.lines().collect(Collectors.toList());

        List<String> result = fromStream.stream()
                .filter(l -> l.split(":")[1].equals(currentUser))
                .map(l -> l.split(":")[0])
                .collect(Collectors.toList());

        br.close();

        return result;
    }

    /**
     * Add to owner or participants
     * @param clientID
     * @param path
     * @throws IOException
     */
    private void addToOwnerPartGroups(String clientID, String path) throws IOException {
        BufferedReader br = readFromFile(path);
        List<String> result = br.lines().collect(Collectors.toList());
        br.close();


        List<String> newResult = new ArrayList<>();
        result.forEach(l -> {
            newResult.add(l);
        });
        newResult.add(groupID+":"+clientID);

        BufferedWriter bw = writeToFile(path);

        newResult.forEach(l -> {
            try {
                bw.write(l);
                bw.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        bw.close();
    }

    /**
     * Write new message to group
     * @param currentUser user writing
     * @param message message
     * @param participants group participants
     * @throws IOException
     */
    public void writeNewMessage(String currentUser, byte[] message, List<String> participants, String keyID) throws IOException {

        String decodedMessage = Base64.getMimeEncoder().encodeToString(message);

        StringBuilder sb = new StringBuilder("");
        participants.forEach(m -> sb.append("|"+m+"|"));

        BufferedReader br = readFromFile(messagesPath);
        List<String> result = br.lines().collect(Collectors.toList());
        br.close();

        List<String> newResult = new ArrayList<>();
        result.forEach(l -> {
            newResult.add(l);
        });

        newResult.add("> "+currentUser+": "+decodedMessage+":"+keyID);
        newResult.add(sb.toString());

        BufferedWriter bw = writeToFile(messagesPath);

        newResult.forEach(l -> {
            try {
                bw.write(l);
                bw.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        bw.close();

        BufferedReader brh = readFromFile(historyPath);
        List<String> hResult = brh.lines().collect(Collectors.toList());
        brh.close();

        List<String> hNewResult = new ArrayList<>();
        hResult.forEach(l -> {
            hNewResult.add(l);
        });

        hNewResult.add("> "+currentUser+": "+decodedMessage+":"+keyID);

        BufferedWriter bhw =writeToFile(historyPath);

        hNewResult.forEach(l -> {
            try {
                bhw.write(l);
                bhw.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        bhw.close();
    }

    /**
     * Get group key user
     * @param user
     * @return
     * @throws FileNotFoundException
     */
    public GroupClientKey getGroupKeyUser(String user) throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(usersKeysPath));

        List<String> listStream = br.lines().collect(Collectors.toList());

        List<String> finalList = listStream.stream()
                .filter(l -> l.split(",")[0].equals(user))
                .collect(Collectors.toList());


        return  new GroupClientKey(finalList.get(0).split(",")[0], finalList.get(0).split(",")[1],
                Base64.getDecoder().decode(finalList.get(0).split(",")[2]));
    }

    /**
     * Get group messages by user
     * @param currentUser
     * @return
     * @throws IOException
     */
    public List<String> getGroupMessagesByUser(String currentUser) throws IOException {
        BufferedReader br = readFromFile(messagesPath);

        List<String> messages = br.lines().collect(Collectors.toList());
        br.close();

        List<String> result = new ArrayList<>();

        for (int i = 0; i < messages.size()-1; i++) {
            if(messages.get(i+1).contains("|"+currentUser) || messages.get(i+1).contains(currentUser+"|"))
                result.add(messages.get(i));
        }

        return result;
    }

    /**
     * Remove reader messages from group
     * @param currentUser user
     * @throws IOException
     */
    public void removeReadedMessages(String currentUser) throws IOException {
        BufferedReader br = readFromFile(messagesPath);
        List<String> list = br.lines().collect(Collectors.toList());

        List<String> result = list.stream()
                .map(l -> l.replace("|"+currentUser+"|", ""))
                .collect(Collectors.toList());


        BufferedWriter writer = writeToFile(messagesPath);

        result.forEach(l -> {
            try {
                writer.write(l);
                writer.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        writer.close();
        br.close();
    }

    /**
     * Removes full readed messages
     * @throws IOException
     */
    public void cleanChat() throws IOException {
        BufferedReader br = readFromFile(messagesPath);

        List<String> messages = br.lines().collect(Collectors.toList());

        List<String> result = new ArrayList<>();

        for (int i = 0; i < messages.size()-1; i+=2) {
            if(!messages.get(i+1).equals("")){
                result.add(messages.get(i));
                result.add(messages.get(i+1));
            }
        }

        BufferedWriter writer = writeToFile(messagesPath);

        result.forEach(l -> {
            try {
                writer.write(l);
                writer.newLine();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        writer.close();
        br.close();
    }

    /**
     * Get group message history
     * @param groupID groupID
     * @return messages list
     * @throws FileNotFoundException
     */
    public List<String> getHistory(String groupID) throws FileNotFoundException {
        BufferedReader br = readFromFile(historyPath);
        return br.lines().collect(Collectors.toList());
    }

    /**
     * Get all user keys
     * @param user
     * @return
     * @throws IOException
     */
    public List<GroupClientKey> getAllUserKeys(String user) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(usersInfo+File.separator+user+".txt"));
        List<String> result = br.lines().collect(Collectors.toList());
        br.close();

        List<GroupClientKey> finalResult = new ArrayList<>();

        result.forEach(l -> {
            finalResult.add(new GroupClientKey(l.split(",")[0], l.split(",")[1], Base64.getDecoder().decode(l.split(",")[2])));
        });

        return finalResult;
    }
}
