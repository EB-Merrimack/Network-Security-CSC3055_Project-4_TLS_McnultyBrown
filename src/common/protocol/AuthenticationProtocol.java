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

/**
 * This interface defines an authentication protocol.
 * This is a specialization of a protocol that supports an
 * authentication method.
 */
 public abstract class AuthenticationProtocol extends Protocol
 {
   /**
    * Sets the role of the user for the authentication protocol
    * @param role a protocol role.
    */
   public AuthenticationProtocol(ProtocolRole role)
   {
     super(role);
   }

   /**
    * Runs the authentication protocol using the specified {@code channel}
    * @param channel the channel to run the protocol over.
    * @return true if the user is authenticated; otherwise, false.
    */
   public abstract boolean authenticate(ProtocolChannel channel);
 }
