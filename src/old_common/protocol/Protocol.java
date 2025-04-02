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
 * A basic protocol class. It maintains the role associated with the instance
 * and provides a mechanism for computing the next message. The protocol
 * state machine must be managed by classes that inherit from {@code Protocol}.
 *
 * @author Zach Kissel
 */
public abstract class Protocol
{
  ProtocolRole role;        // The role of this instance of the protocol.

  /**
   * Construct a new protocol object with role {@code role}.
   * @param role a protocol role.
   */
  public Protocol(ProtocolRole role)
  {
    this.role = role;
  }

  /**
   * Gets the role.
   * @return the role for this instance of the protocol.
   */
   public ProtocolRole getRole()
   {
     return this.role;
   }

  /**
   * Performs the next phase of the protocol based on the
   * message {@code msg} and role.
   * @param msg the protocol message from the peer.
   */
  public abstract Message doPhase(Message msg);

}
