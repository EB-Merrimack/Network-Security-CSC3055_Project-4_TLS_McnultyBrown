package common.protocol.messages;

import common.protocol.Message;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

public class GetResponseMessage implements Message {
    private List<PostMessage> posts;

    public GetResponseMessage() {
        this.posts = new ArrayList<>();
    }

    public GetResponseMessage(List<PostMessage> posts) {
        this.posts = posts;
    }

    public List<PostMessage> getPosts() {
        return posts;
    }

    @Override
    public String getType() {
        return "GetResponseMessage";
    }

    @Override
    public JSONType toJSONType() {
        JSONArray array = new JSONArray();
        for (PostMessage post : posts) {
            array.add(post.toJSONType());
        }

        JSONObject obj = new JSONObject();
        obj.put("type", getType());
        obj.put("posts", array);
        return obj;
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        if (!(obj instanceof JSONObject)) {
            throw new InvalidObjectException("Expected JSONObject.");
        }

        JSONObject json = (JSONObject) obj;
        if (!json.containsKey("posts")) {
            throw new InvalidObjectException("Missing posts field.");
        }

        JSONArray array = (JSONArray) json.get("posts");
        posts = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            PostMessage post = new PostMessage();
            post.deserialize((JSONType) array.get(i));
            posts.add(post);
        }
    }

    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        GetResponseMessage response = new GetResponseMessage();
        response.deserialize(obj);
        return response;
    }

    @Override
    public String toString() {
        return "[GetResponseMessage] with " + posts.size() + " posts";
    }
}