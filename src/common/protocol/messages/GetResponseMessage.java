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

    /**
     * Returns the list of posts returned in response to a GetMessage
     * @return The list of posts returned in response to a GetMessage
     */
    public List<PostMessage> getPosts() {
        return posts;
    }

    /**
     * Gets the message type as a string.
     * @return the message type as a string.
     */
    @Override
    public String getType() {
        return "GetResponseMessage";
    }

    /**
     * Converts the object to a JSON type.
     * @return a JSON type either JSONObject or JSONArray.
     * The returned JSONObject contains the type and posts fields.
     * The posts field is an JSONArray of PostMessage JSONTypes.
     */
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

/**
 * Deserializes a JSONType object into a GetResponseMessage instance.
 * 
 * @param obj the JSONType object to deserialize, expected to be a JSONObject.
 * @throws InvalidObjectException if the object is not a JSONObject or 
 *                                if the "posts" field is missing.
 * 
 * This method extracts the "posts" field from the JSONObject, which 
 * should be a JSONArray of PostMessage objects. Each PostMessage is 
 * deserialized and added to the list of posts.
 */

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

    /**
     * Decodes a JSON object into a GetResponseMessage instance.
     * 
     * @param obj the JSON object to decode, expected to be a JSONObject.
     * @return a deserialized GetResponseMessage instance.
     * @throws InvalidObjectException if the object is not a JSONObject or 
     *                                if the "posts" field is missing.
     */
    @Override
    public Message decode(JSONObject obj) throws InvalidObjectException {
        GetResponseMessage response = new GetResponseMessage();
        response.deserialize(obj);
        return response;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "[GetResponseMessage] with " + posts.size() + " posts";
    }
}