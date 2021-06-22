package services;

import persistence.GroupsDB;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the connection from the server interface with the persistence layer
 * @author Luis Ferreira 49495
 * @author Xavier Cordeiro 46365
 */
public class GroupService {

    private GroupsDB groupsDB;
    private SecretKey secretKey;
    private String groupId;
    private LoginService loginService;


    public GroupService(SecretKey secretKey, String groupId) throws IOException {
      this.loginService = new LoginService(secretKey);
      this.secretKey = secretKey;
      this.groupId = groupId;
      groupsDB = new GroupsDB();
    }

    /**
     * Creates group
     * @param owner
     * @return status code
     * @throws IOException
     */
    public int newGroup(String owner, byte[] groupKey) throws IOException {
        if(groupsDB.exist(groupId))
            return 0;
        else{
            groupsDB = new GroupsDB(secretKey, groupId);
            groupsDB.createGroup(owner, groupKey);
            return 1;
        }
    }

    /**
     * Get group participants
     * @return
     * @throws IOException
     */
    public List<String> getGroupParticipants() throws IOException {
        List<String> list = new ArrayList<>();
        if(!groupsDB.exist(groupId)){
            list.add("nope");
            return list;
        }

        return new GroupsDB(secretKey, groupId).getParticipants();
    }

    /**
     * Adds user to group
     *
     * @param newGroupKey
     * @param currentUser
     * @param userID
     * @return status code
     * @throws IOException
     */
    public int addUser(byte[] newGroupKey, String currentUser, String userID) throws IOException {
        if(!groupsDB.exist(groupId))
            return -1;
        groupsDB = new GroupsDB(secretKey, groupId);
        if(!groupsDB.isOwner(currentUser))
            return -2;
        if(loginService.userExists(userID) == 0)
            return -4;
        if(groupsDB.getParticipants().contains(userID))
            return -3;
        updateGroupKey(newGroupKey, currentUser);
        groupsDB.addToGroup(userID);
        return 1;
    }

    /**
     * Update group key
     * @param newGroupKey
     * @param currentUser
     * @throws IOException
     */
    public void updateGroupKey(byte[] newGroupKey, String currentUser) throws IOException {
        groupsDB = new GroupsDB(secretKey, groupId);
        groupsDB.updateGroupKey(newGroupKey, currentUser);
    }

    /**
     * Get group ID
     * @param out
     * @throws IOException
     */
    public void getGroupId(ObjectOutputStream out) throws IOException {
        if(!groupsDB.exist(groupId)){
            out.writeObject("");
            out.flush();
            return;
        }

        groupsDB = new GroupsDB(secretKey, groupId);
        out.writeObject(groupsDB.getGroupId());
        out.flush();
    }

    /**
     * Removes user from group
     * @param currentUser
     * @param userID
     * @return status code
     * @throws IOException
     */
    public int removeUser(byte[] newGroupKey, String currentUser, String userID) throws IOException {

        if(!groupsDB.exist(groupId))
            return -1;
        groupsDB = new GroupsDB(secretKey, groupId);
        if(!groupsDB.isOwner(currentUser))
            return -2;
        if(loginService.userExists(userID) == 0)
            return -4;
        if(!groupsDB.getParticipants().contains(userID))
            return -3;
        groupsDB.removeFromGroup(userID);
        updateGroupKey(newGroupKey, currentUser);

        return 1;
    }

    /**
     * Gets group info of the user
     * @param currentUser
     * @return info
     * @throws FileNotFoundException
     */
    public String getGroupInfoDefault(String currentUser) throws IOException {
        StringBuilder sb = new StringBuilder("");
        groupsDB = new GroupsDB(secretKey, groupId);

        List<String> owner = groupsDB.getOwnerGroups(currentUser);
        List<String> participant = groupsDB.getParticipantGroups(currentUser);

        if(owner.isEmpty() && participant.isEmpty()){
            sb.append("\nDono: Nenhum grupo\n\n");
            sb.append("Participa: Nenhum grupo");
        }else if(owner.isEmpty()){
            sb.append("Dono: Nenhum grupo\n\n");
            sb.append("Paticipa: \n");
            participant.forEach(l -> sb.append(" - "+l+"\n"));
        }else if(participant.isEmpty()){
            sb.append("\nDono: \n");
            owner.forEach(l -> sb.append(" - "+l+"\n"));
            sb.append("\nPaticipa: Nenhum grupo\n\n");
        }else{
            sb.append("\nDono: \n");
            owner.forEach(o -> sb.append(" - "+o+"\n"));
            sb.append("\nParticipa: \n");
            participant.forEach(p -> sb.append(" - "+p+"\n"));
        }

        return sb.toString();
    }

    /**
     * Gets group info from group
     * @param currentUser
     * @param groupID
     * @return info
     * @throws IOException
     */
    public String getGroupInfo(String currentUser, String groupID) throws IOException {

        if(!groupsDB.exist(groupID))
            return "-1";

        groupsDB = new GroupsDB(secretKey, groupID);
        if(!groupsDB.isOwner(currentUser))
            return "-2";

        StringBuilder sb = new StringBuilder("");

        sb.append("\nDono do Grupo: "+currentUser+"\n");
        sb.append("Membros do Grupo: \n");

        List<String> participants = groupsDB.getParticipants();

        if(participants.isEmpty())
            sb.append("Nao existem participantes no grupo "+groupID+"\n");
        else
            participants.forEach(p -> sb.append(" - "+p+"\n"));

        return sb.toString();
    }

    /**
     * Send message
     * @param currentUser
     * @param groupID
     * @param message
     * @param id
     * @return
     * @throws IOException
     */
    public int sendMessage(String currentUser, String groupID, byte[] message, String id) throws IOException {
        groupsDB = new GroupsDB(secretKey, groupID);

        List<String> membros = groupsDB.getParticipants();

        if(!groupsDB.exist(groupID))
            return -1;
        if(!groupsDB.getParticipants().contains(currentUser))
            return -2;
        else{
            groupsDB.writeNewMessage(currentUser, message, membros, id);
            return 1;
        }
    }

    /**
     * Collects unread messages
     * @param currentUser
     * @param groupID
     * @return messages
     * @throws IOException
     */
    public String collect(String currentUser, String groupID) throws IOException {
        StringBuilder sb = new StringBuilder("");

        if(!groupsDB.exist(groupID))
            return "-1";
        groupsDB = new GroupsDB(secretKey, groupId);
        if(!groupsDB.getParticipants().contains(currentUser))
            return "-2";

        List<String> userMessages = groupsDB.getGroupMessagesByUser(currentUser);
        groupsDB.removeReadedMessages(currentUser);
        groupsDB.cleanChat();

        if(userMessages.isEmpty())
            return "-3";

        userMessages.forEach(m -> sb.append(m+"\n"));

        return sb.toString();
    }

    /**
     * Gets group message history
     * @param currentUser
     * @return history
     * @throws FileNotFoundException
     */
    public String getHistory(String currentUser) throws IOException {
        StringBuilder sb = new StringBuilder("");

        if(!groupsDB.exist(groupId))
            return "-1";
        groupsDB = new GroupsDB(secretKey, groupId);
        if(!groupsDB.getParticipants().contains(currentUser))
            return "-2";

        List<String> userMessages = groupsDB.getHistory(groupId);

        if(userMessages.isEmpty()){
            return "-3";
        }

        userMessages.forEach(m -> sb.append(m+"\n"));

        return sb.toString();
    }

    /**
     * Save keys
     * @param usersKeys
     * @param groupId
     * @throws IOException
     */
    public void saveKeys(List<GroupClientKey> usersKeys, String groupId) throws IOException {
        groupsDB = new GroupsDB(secretKey, groupId);
        groupsDB.saveNewKeys(usersKeys);
    }

    /**
     * Get group key by user
     * @param user
     * @return
     * @throws IOException
     */
    public GroupClientKey getGroupKeyUser(String user) throws IOException {
        groupsDB = new GroupsDB(secretKey, groupId);

        if(!groupsDB.getParticipants().contains(user)){
            return new GroupClientKey("nope", "", null);
        }

        return groupsDB.getGroupKeyUser(user);
    }

    /**
     * Get all user keys
     * @param user
     * @return
     * @throws IOException
     */
    public List<GroupClientKey> getUserKeys(String user) throws IOException {
        groupsDB = new GroupsDB(secretKey, groupId);

        return groupsDB.getAllUserKeys(user);
    }
}
