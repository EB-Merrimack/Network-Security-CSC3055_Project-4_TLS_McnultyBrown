package common;

import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.JSONSerializable;
import merrimackutil.json.JsonIO;

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
        try {
            File file = new File(BOARD_FILE);
            if (!file.exists()) {
                System.out.println("[Board] board.json not found. Initializing empty board.");
                posts = new ArrayList<>();
                saveToFile();  // write empty structure with "posts": []
                return;
            }

            JSONType raw = JsonIO.readObject(file);
            if (!(raw instanceof JSONObject)) {
                throw new InvalidObjectException("board.json is not a valid JSON object.");
            }

            JSONObject boardData = (JSONObject) raw;
            deserialize(boardData);

            System.out.println("[Board] Loaded " + posts.size() + " post(s).");
        } catch (IOException e) {
            System.err.println("[Board] Error loading board.json: " + e.getMessage());
            posts = new ArrayList<>(); // fallback to empty board
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
