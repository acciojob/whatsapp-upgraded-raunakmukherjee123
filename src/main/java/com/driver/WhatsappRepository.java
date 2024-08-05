package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most once group
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }


    public String createUser(String name, String mobile) throws Exception {
        //If the mobile number exists in database, throw "User already exists" exception
        //Otherwise, create the user and return "SUCCESS"
        //your code here
        //If the mobile number exists in database, throw "User already exists" exception
        if(userMobile.contains(mobile)) {
            throw new Exception("User already exists");
        }
        //Otherwise, create the user and return "SUCCESS"
        User newUser = new User(name, mobile);
        userMobile.add(mobile);
        return "SUCCESS";
    }



    public Group createGroup(List<User> users){
        // The list contains at least 2 users where the first user is the admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group customGroupCount". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // If group is successfully created, return group.
        //your code here

        // Check if the users list is not null and has at least 2 users
        if(users == null || users.size() < 2){
            // You can throw an exception or return null
            return null;
        }

        // If the users list has only 2 users, set the group name as the name of the second user
        // Otherwise, increment customGroupCount and set the group name as "Group " + customGroupCount
        String groupName;
        if(users.size() == 2){
            groupName = users.get(1).getName();
        } else {
            customGroupCount++;
            groupName = "Group " + customGroupCount;
        }

        // Create a new Group with the group name and the number of participants
        Group group = new Group(groupName, users.size());

        // Add the new group and the users list to the groupUserMap
        groupUserMap.put(group, users);

        // Add the new group and the admin user (the first user in the list) to the adminMap
        adminMap.put(group, users.get(0));

        // Finally, return the new group
        return group;
    }


    public int createMessage(String content){
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        //your code here
        // Increment the messageId
        messageId++;

        // Create a new Message with the messageId, content, and current timestamp
        Message message = new Message(messageId, content);

        // You might want to store the created message in a suitable data structure

        // Finally, return the messageId
        return messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        //your code here
        if(!groupUserMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }
        List<User> groupUsers = groupUserMap.get(group);
        if(!groupUsers.contains(sender)) {
            throw new Exception("You are not allowed to send message");
        }
        List<Message> groupMessages = groupMessageMap.getOrDefault(group, new ArrayList<>());
        groupMessages.add(message);
        groupMessageMap.put(group, groupMessages);
        senderMap.put(message, sender);
        return groupMessages.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS".

        //your code here

        if(!groupUserMap.containsKey(group)) {
            throw new Exception("Group does not exist");
        }
        User currentAdmin = adminMap.get(group);
        if(!currentAdmin.equals(approver)) {
            throw new Exception("Approver does not have rights");
        }
        List<User> groupUsers = groupUserMap.get(group);
        if(!groupUsers.contains(user)) {
            throw new Exception("User is not a participant");
        }
        adminMap.put(group, user);
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        //your code here

        boolean userFound = false;
        Group groupToRemoveUser = null;
        for(Map.Entry<Group, List<User>> entry : groupUserMap.entrySet()){
            List<User> users = entry.getValue();
            if(users.contains(user)){
                userFound = true;
                groupToRemoveUser = entry.getKey();
                break;
            }
        }
        if(!userFound) {
            throw new Exception("User not found");
        }
        for(User admin : adminMap.values()){
            if(admin.equals(user)){
                throw new Exception("Cannot remove admin");
            }
        }
        if(groupToRemoveUser != null){
            groupUserMap.get(groupToRemoveUser).remove(user);
        }
        Iterator<Map.Entry<Message, User>> iterator = senderMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Message, User> entry = iterator.next();
            if(entry.getValue().equals(user)){
                Message message = entry.getKey();
                iterator.remove(); // remove from senderMap
                for(List<Message> messages : groupMessageMap.values()){ // remove from groupMessageMap
                    messages.remove(message);
                }
            }
        }
        int updatedNumberOfUsers = 0;
        int updatedNumberOfMessagesInGroup = 0;
        if(groupToRemoveUser != null){
            updatedNumberOfUsers = groupUserMap.get(groupToRemoveUser).size();
            updatedNumberOfMessagesInGroup = groupMessageMap.get(groupToRemoveUser).size();
        }
        int updatedNumberOfOverallMessages = senderMap.size();
        return updatedNumberOfUsers + updatedNumberOfMessagesInGroup + updatedNumberOfOverallMessages;
    }

    public String findMessage(Date start, Date end, int K) throws Exception{
        // Find the Kth latest message between start and end (excluding start and end)
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception
        //your code here

        List<Message> messagesBetweenStartAndEnd = new ArrayList<>();
        for(List<Message> messages : groupMessageMap.values()){
            for(Message message : messages){
                Date messageTimestamp = message.getTimestamp();
                if(messageTimestamp.after(start) && messageTimestamp.before(end)){
                    messagesBetweenStartAndEnd.add(message);
                }
            }
        }
        if(messagesBetweenStartAndEnd.size() < K) {
            throw new Exception("K is greater than the number of messages");
        }
        messagesBetweenStartAndEnd.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));
        return messagesBetweenStartAndEnd.get(K-1).getContent();
    }
    //
}