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
package common.protocol;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONObject;
import java.io.InvalidObjectException;

/**
 * This interface describes a basic protocol message.
 * @author Zach Kissel
 */
 public interface Message extends JSONSerializable
 {
   /**
    * Gets the message type as a string.
    * @return the message type as a string.
    */
   public String getType();

   /**
    * Builds a new message from the given
    * JSON object if the message type matches.
    * @param obj the JSON object to decode.
    * @return the message built for the type.
    * @throws InvalidObjectException if {@code obj} is not of the
    * correct type.
    */
   public Message decode(JSONObject obj) throws InvalidObjectException;
 }
