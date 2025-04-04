/* 
 * Copyright (C) 2023 - 2025  Zachary A. Kissel 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package common;

import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.JSONSerializable;
import java.io.InvalidObjectException;
import java.util.HashMap;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jcajce.spec.ScryptKeySpec;
import java.security.Security;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * This class provides a simple secret store.
 * @author Zach Kissel
 */
 public class SecretStore implements JSONSerializable
 {
   private HashMap<String, String> secrets;

   // Constants for key derivation for easy maintainability.
  private final static int COST = 2048;          // A.K.A Iterations
  private final static int BLK_SIZE = 8;
  private final static int PARALLELIZATION = 1;  // Number of parallel threads to use.
  private final static int KEY_SIZE=128;  // In bits.

   /**
    * Constructs a new empty secret store.
    */
   public SecretStore()
   {
     secrets = new HashMap<>();
   }

   /**
    * Construct a secret store from a given JSON object.
    * @param obj the JSON object representing the secret store.
    * @throws InvalidObjectException if {@code obj} is not a valid
    * secret store object.
    */
    public SecretStore(JSONObject obj) throws InvalidObjectException
    {
      secrets = new HashMap<>();
      deserialize(obj);
    }

   /**
    * Look up the secret associated with a username
    * {@code uname}.
    * @param uname name of user to lookup secret for.
    * @return the secret or {@code null} if the secret is not found.
    */
   public String lookupSecret(String uname)
   {
     if (!secrets.containsKey(uname))
      return null;
     return secrets.get(uname);
   }

   /**
    * Add a secret to the secret store.
    * @param uname the name of the user.
    * @param secret the secret to associate with user {@code uname}.
    * @return true if the the entry was added successfully; otherwise, false.
    */
    public boolean addSecret(String uname, String secret)
    {
      if (secrets.containsKey(uname))
        return false;
      secrets.put(uname, secret);
      return true;
    }

    /**
     * Derive the key from the secret in the secret store. The salt for
     * the password is the user id.
     * @param id the user id.
     * @return a key
     */
     public byte[] deriveKey(String id)
     {
       String secret = lookupSecret(id);
       return SecretStore.deriveKey(id, secret);
     }

   /**
    * Derive the key from the secret and user id.
    * @param id the user id.
    * @param secret the secret.
    * @return the key.
    */
    public static byte[] deriveKey(String id, String secret)
    {
      // Add the bouncy castle provider.
      Security.addProvider(new BouncyCastleProvider());

      SecretKey key = null;

      if (secret == null)
        return null;

      try
      {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("SCRYPT");
          ScryptKeySpec spec = new ScryptKeySpec(secret.toCharArray(),
            id.getBytes(), COST, BLK_SIZE, PARALLELIZATION, KEY_SIZE);
          key = skf.generateSecret(spec);
      }
      catch (NoSuchAlgorithmException nae)
      {
          nae.printStackTrace();
      }
      catch (InvalidKeySpecException kse)
      {
          kse.printStackTrace();
      }

      return key.getEncoded();
    }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject config;
     JSONArray secrets;
     String[] keys = {"secrets"};

     if (obj.isObject())
     {
       config = (JSONObject) obj;

       config.checkValidity(keys);
       secrets = config.getArray("secrets");
     }
     else
       throw new InvalidObjectException(
          "Secrets Store -- recieved array, expected Object.");

    // Load all secret objects into the hash map.
    for(int i = 0; i < secrets.size(); i++)
    {
      JSONObject entry = secrets.getObject(i);
      String[] secretKeys = {"user", "secret"};
      entry.checkValidity(secretKeys);
      
      addSecret(entry.getString("user"), entry.getString("secret"));
    }

   }

   /**
    * Converts the object to a JSON type.
    * @return a JSON type either JSONObject or JSONArray.
    */
   public JSONType toJSONType()
   {
     JSONObject secretStore = new JSONObject();
     JSONArray array = new JSONArray();
     for (String key : secrets.keySet())
     {
       JSONObject obj = new JSONObject();
       obj.put("user", key);
       obj.put("secret", secrets.get(key));
       array.add(obj);
     }
     secretStore.put("secrets", array);

     return secretStore;
   }
 }
