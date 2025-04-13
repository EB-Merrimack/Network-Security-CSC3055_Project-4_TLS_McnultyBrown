package common;

import merrimackutil.json.*;
import merrimackutil.json.types.*;
import server.Configuration;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import common.protocol.post.Post;

/**
 * This class represents a board of encrypted posts.
 */
public class Board implements JSONSerializable {
    private List<Post> posts;
    private static final String BOARD_FILE = Configuration.getBoardFile();

    public Board() {
        posts = new ArrayList<>();
        System.out.println("[DEBUG] Board created with an empty post list.");
    }

    public Board(JSONObject obj) throws InvalidObjectException {
        posts = new ArrayList<>();
        System.out.println("[DEBUG] Board deserialization from JSONObject started.");
        deserialize(obj);
        System.out.println("[DEBUG] Board deserialization completed.");
    }

    public void addPost(Post post) {
        posts.add(post);
        System.out.println("[DEBUG] Added post: " + post);
    }

    public List<Post> getPosts() {
        System.out.println("[DEBUG] Retrieving posts, total posts: " + posts.size());
        return posts;
    }

    @Override
    public JSONType toJSONType() {
        System.out.println("[DEBUG] Serializing Board to JSON.");
        JSONObject boardObj = new JSONObject();
        
        // Wrapper class for the posts array
        PostWrapper postWrapper = new PostWrapper(posts);
        boardObj.put("posts", postWrapper.toJSONType());
        
        System.out.println("[DEBUG] Board serialized to JSON: " + boardObj);
        return boardObj;
    }

    public void loadFromFile() {
        System.out.println("[DEBUG] Loading board data from file: " + BOARD_FILE);
        try {
            JSONObject boardObj = JsonIO.readObject(new File(BOARD_FILE));
            System.out.println("[DEBUG] Board JSON loaded: " + boardObj);
    
            if (!boardObj.containsKey("posts")) {
                System.err.println("[ERROR] 'posts' key not found in board JSON.");
                return;
            }
    
            JSONArray postArray = boardObj.getArray("posts");
    
            if (postArray == null) {
                System.err.println("[ERROR] 'posts' key exists but is not an array.");
                return;
            }
    
            posts.clear();
            for (int i = 0; i < postArray.size(); i++) {
                JSONObject postObj = postArray.getObject(i);
                posts.add(new Post(postObj));
                System.out.println("[DEBUG] Deserialized post: " + postObj);
            }
    
            System.out.println("[DEBUG] Successfully loaded " + posts.size() + " posts.");
    
        } catch (IOException  e) {
            System.err.println("[ERROR] Failed to load board data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        System.out.println("[DEBUG] Deserializing board from JSON.");
        if (!obj.isObject()) {
            throw new InvalidObjectException("Board expects a JSONObject.");
        }

        JSONObject boardObj = (JSONObject) obj;
        boardObj.checkValidity(new String[]{"posts"});

        // Get the array of posts from the board JSON object
        JSONArray postArray = boardObj.getArray("posts");
        System.out.println("[DEBUG] Found " + postArray.size() + " posts to deserialize.");
        for (int i = 0; i < postArray.size(); i++) {
            JSONObject postObj = postArray.getObject(i);

            // Assuming you have a Post constructor that takes a JSONObject
            Post post = new Post(postObj);
            posts.add(post);
            System.out.println("[DEBUG] Deserialized post: " + post);
        }
    }
/**
 * Loads existing posts from file, adds a new post, and saves the updated list.
 */
public void loadAndAddPost(Post newPost) {
    System.out.println("[DEBUG] Loading board, adding post, and saving...");

    // Step 1: Load from file if it exists
    File boardFile = new File(BOARD_FILE);
    if (boardFile.exists()) {
        loadFromFile();  // This will populate 'posts'
    } else {
        System.out.println("[DEBUG] Board file does not exist. Starting with empty post list.");
    }

    // Step 2: Add the new post
    addPost(newPost);

    // Step 3: Save updated board to file
    saveToFile();
}
    public void saveToFile() {
        System.out.println("[DEBUG] Saving board data to file: " + BOARD_FILE);
        try {
            // Use PostWrapper to serialize posts as a JSONArray
            PostWrapper postWrapper = new PostWrapper(posts);
            JsonIO.writeFormattedObject(postWrapper, new File(BOARD_FILE));
            System.out.println("[DEBUG] Board data saved to file.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save board data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Wrapper class to serialize the posts array
    private static class PostWrapper implements JSONSerializable {
        private final JSONArray entries;

        public PostWrapper(List<Post> posts) {
            entries = new JSONArray();
            for (Post post : posts) {
                entries.add(post.toJSONType());
            }
        }

        @Override
        public JSONType toJSONType() {
            JSONObject root = new JSONObject();
            root.put("posts", entries);
            return root;
        }

        @Override
        public void deserialize(JSONType obj) {
            // unused
        }
    }
}
