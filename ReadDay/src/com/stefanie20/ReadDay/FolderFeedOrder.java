package com.stefanie20.ReadDay;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by F317 on 16/2/22.
 */
public class FolderFeedOrder {
    public static void getOrder() {
        String userId = UserInfo.getUserId();
        //get the streamprefs
        String streamprefs = null;
        try (BufferedReader reader = ConnectServer.connectServer(ConnectServer.streamPreferenceListURL)) {
            streamprefs = reader.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        //parse the streamprefs nad get the folder order
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(streamprefs);
        JsonObject object = element.getAsJsonObject();
        JsonArray root = object.getAsJsonObject("streamprefs").getAsJsonArray("user/" + userId + "/state/com.google/root");
        String folderOrderString = root.get(0).getAsJsonObject().get("value").getAsString();
        System.out.println(folderOrderString);
        List<String> folderOrderList = sortOrder(folderOrderString);

        //get the folderTagList and the subscriptionList
        Gson gson = new Gson();
        FoldersTagsList foldersTagsList = gson.fromJson(ConnectServer.connectServer(ConnectServer.folderTagListURL), FoldersTagsList.class);
        SubscriptionsList subscriptionsList = gson.fromJson(ConnectServer.connectServer(ConnectServer.subscriptionListURL), SubscriptionsList.class);

        //sort the folder order
        for (String s : folderOrderList) {
            boolean isFolder = false;
            for (Tag tag : foldersTagsList.getTags()) {
                if (s.equals(tag.getSortid())) {
                    System.out.println("sortid = " + s + " id = " + tag.getId());
                    isFolder = true;
                    //get the feed order in the folder
                    getFeedOrder(object,tag.getId(),subscriptionsList);

                    continue;
                }
            }
            if (isFolder == false) {
                for (Subscription subscription : subscriptionsList.getSubscriptions()) {
                    if (s.equals(subscription.getSortid())) {
                        System.out.println("sortid = " + s + " id = " + subscription.getTitle());
                        continue;
                    }
                }
            }
        }


    }

    public static List<String> sortOrder(String s) { //maybe using stream
        List<String> list = new ArrayList<>();
        for (int i = 0; i < s.length(); i+=8) {
            list.add(s.substring(i, i + 8));
        }
        return list;
    }

    public static void getFeedOrder(JsonObject streamprefs, String folderId, SubscriptionsList subscriptionsList) {
        String feedOrderString = streamprefs.getAsJsonObject("streamprefs").getAsJsonArray(folderId).get(1).getAsJsonObject().get("value").getAsString();
        List<String> feedOrderList = sortOrder(feedOrderString);
        for (String s : feedOrderList) {
            for (Subscription subscription : subscriptionsList.getSubscriptions()) {
                if (s.equals(subscription.getSortid())) {
                    System.out.println("        "+subscription.getTitle());
                }
            }
        }
    }


    public static void main(String[] args) {
        getOrder();
    }

}
