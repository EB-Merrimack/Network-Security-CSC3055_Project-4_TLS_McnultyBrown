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
        
        // Wrapper class for the posts array
        PostWrapper postWrapper = new PostWrapper(posts);
        boardObj.put("posts", postWrapper.toJSONType());
        
        return boardObj;
    }

    public void loadFromFile() {
        try {
            JSONObject boardObj = JsonIO.readObject(new File(BOARD_FILE));
    
            if (!boardObj.containsKey("posts")) {
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
            }
    
    
        } catch (IOException  e) {
            System.err.println("[ERROR] Failed to load board data: " + e.getMessage());
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
/**
 * Loads existing posts from file, adds a new post, and saves the updated list.
 */
public void loadAndAddPost(Post newPost) {

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
        try {
            // Use PostWrapper to serialize posts as a JSONArray
            PostWrapper postWrapper = new PostWrapper(posts);
            JsonIO.writeFormattedObject(postWrapper, new File(BOARD_FILE));
        } catch (IOException e) {
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
