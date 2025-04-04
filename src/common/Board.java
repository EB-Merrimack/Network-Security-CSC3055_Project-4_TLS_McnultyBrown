package common;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import common.protocol.post.Post;

/**
 * This class represents a board of encrypted posts.
 */
public class Board implements JSONSerializable {
    private List<Post> posts;
    private static final String BOARD_FILE = "board.json";

    public Board() {
        posts = new ArrayList<>();
    }

    public Board(JSONObject obj) throws InvalidObjectException {
        posts = new ArrayList<>();
        deserialize(obj);
    }

    public void addPost(Post post) {
        posts.add(post);
    }

    public List<Post> getPosts() {
        return posts;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Board expects a JSONObject.");
        }

        JSONObject boardObj = (JSONObject) obj;
        boardObj.checkValidity(new String[]{"posts"});

        JSONArray postArray = boardObj.getArray("posts");
        for (int i = 0; i < postArray.size(); i++) {
            JSONObject postObj = postArray.getObject(i);
            posts.add(new Post(postObj));
        }
    }

    @Override
    public JSONType toJSONType() {
        JSONObject boardObj = new JSONObject();
        JSONArray postArray = new JSONArray();

        for (Post post : posts) {
            postArray.add(post.toJSONType());
        }

        boardObj.put("posts", postArray);
        return boardObj;
    }

    public void loadFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(BOARD_FILE))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
    
            // Read the file line by line and build the JSON string
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
    
            // Create a JSONObject from the string content
            JSONObject jsonObj = new JSONObject();
    
            // Assuming the content is a valid JSON string, now put the data into the JSONObject
            // Using put to populate the JSONObject with a key-value pair
            jsonObj.put("boardData", jsonContent.toString());
    
            // Deserialize the JSON content into the Board object
            deserialize(jsonObj);
        } catch (IOException  e) {
            e.printStackTrace();
        }
    }
    

    public void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BOARD_FILE))) {
            JSONObject jsonObject = (JSONObject) toJSONType();
            writer.write(jsonObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
