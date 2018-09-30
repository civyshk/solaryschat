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

import java.util.Collection;

public interface View {
	
	//From Presenter --------------------------------------\	
	public void setPresenter(Presenter presenter);
	public void show();
	public void setConnected();
	public void setDisconnected();
	
	public int getNewRoomID();
	
	public void showBroadcasts(Collection<String> broadcasts);
	
	/**
	 * @param roomID ID of the new room, previously got from getNewRoomID()
	 * @param name Name of the room
	 * @return The ID of the room in the view
	 */
	public void addRoom(int roomID, String name);
	
	/**
	 * @param name The name of the new user
	 * @param roomId The ID of the room that the user is in
	 * @return The ID of the user in the view
	 */
	public int addUser(String name, int roomID);
	
	public void renameRoom(int roomID, String newName);
	public void renameUser(int userID, String newName);
	public void removeRoom(int roomID);
	public void removeUser(int userID, int roomID);
	public void clearUsers(int roomID);
	public void clearMessages(int roomID);
	public void alertRoom(int roomID);
	public void showRoom(int roomID);
	public void appendTime(int roomID, String time);
	public void appendUserName(int roomID, int userID, String userName);
	public void appendMessage(int roomID, int userID, String text, boolean newLine);
	public void appendSystemMessage(String message, String time, int roomID);
	//From Presenter --------------------------------------/
	
	/*
	Lo que el controlador le manda a la vista:
		 + setPresenter
		 + Show
		 + Estamos conectados
		 + Estamos desconectados
		 + Nueva sala disponible
		 + Nuevo usuario en una determinada sala
		 + Sala cambi� de nombre
		 + Usuario cambi� de nombre
		 + Sala ya no disponible
		 + Usuario sali� de sala
		 + Sala requiere atenci�n
		 + Nuevo mensaje en una determinada sala
		 */
}
