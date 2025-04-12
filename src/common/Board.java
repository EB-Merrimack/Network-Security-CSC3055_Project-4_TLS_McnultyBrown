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
            System.out.println("[DEBUG] Board data loaded from file. Deserializing...");

            // Deserialize the board data
            deserialize(jsonObj);

        } catch (IOException e) {
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
            root.put("entries", entries);
            return root;
        }

        @Override
        public void deserialize(JSONType obj) {
            // unused
        }
    }
}
