/*	Solarys Chat - Local network UDP broadcaster chat
 * 	Copyright 2018 Yeshe Santos García <civyshk@gmail.com>
 *	
 *	This file is part of Solarys Chat
 *	
 *	Solarys Chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.project104.chat.solaris;

public interface UserEventsListener {
	public void connect(); 
	public void disconnect();
	public void setClientName(String name);
	public void selectChat(int roomID);
	public void closeChat(int roomIndex);
	public void openChatFor(int userID);
	public void sendMessage(int roomID, String message);
	public void setBroadcastIP(String broadcast);
}
