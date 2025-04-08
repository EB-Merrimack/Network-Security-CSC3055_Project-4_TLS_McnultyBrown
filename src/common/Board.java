package common;
import common.protocol.messages.Post;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
    
            // Convert the JSON content into a JSONObject
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("boardData", jsonContent.toString());
    
            // Deserialize the board data
            deserialize(jsonObj);
    
        } catch (IOException  e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!obj.isObject()) {
            throw new InvalidObjectException("Board expects a JSONObject.");
        }
    
        JSONObject boardObj = (JSONObject) obj;
        boardObj.checkValidity(new String[]{"posts"});
    
        // Get the array of posts from the board JSON object
        JSONArray postArray = boardObj.getArray("posts");
        for (int i = 0; i < postArray.size(); i++) {
            JSONObject postObj = postArray.getObject(i);
    
            // Assuming you have a Post constructor that takes a JSONObject
            Post post = new Post(postObj);
            posts.add(post);
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
