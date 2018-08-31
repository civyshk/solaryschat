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

package net.project104.chat.solaris.gui;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.text.SimpleAttributeSet;

import net.project104.chat.solaris.UserEventsListener;

import java.awt.BorderLayout;

public class PanelRoom extends JPanel {
	private UserEventsListener eventsListener;
	private int roomID;
	private String roomName;
	private int unreadCount;
	
	private PanelUsers panelUsers;
	private PanelChat panelChat;
	private ArrayList<Integer> nodeIDs;
	private Random rand;
	
	public PanelRoom(UserEventsListener eventsListener, int roomID, String roomName) {
		super();
		this.eventsListener = eventsListener;
		this.roomID = roomID;		
		this.roomName = roomName;
		this.unreadCount = 0;
		
		setLayout(new BorderLayout(0, 0));
		
		panelUsers = new PanelUsers(this);
		panelChat = new PanelChat(this);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panelUsers, panelChat);
		splitPane.setOneTouchExpandable(false);
		splitPane.setDividerLocation(150);
		add(splitPane);
		
		Dimension minimumSize = new Dimension(100, 50);
		panelUsers.setMinimumSize(minimumSize);
		panelChat.setMinimumSize(minimumSize);

		splitPane.setLeftComponent(panelUsers);
		splitPane.setRightComponent(panelChat);
				
		nodeIDs = new ArrayList<>();
		rand = new Random();
	}

	public UserEventsListener getEventsListener() {
		return eventsListener;
	}
	
	private int generateNodeID() {
		int nodeID;
		do{
			nodeID = rand.nextInt();
		}while(nodeIDs.contains(nodeID));
		return nodeID;
	}

	protected int getUserIndexByID(int userID) {
		return nodeIDs.indexOf(userID);
	}
	
	protected int getUserIDByIndex(int userIndex) {
		return nodeIDs.get(userIndex);
	}
	
	protected int getRoomID() {
		return roomID;
	}

	public int addUser(String name) {
		int nodeID = generateNodeID();
		nodeIDs.add(nodeID);
		panelUsers.addUser(name);
		return nodeID;
	}

	public void removeUser(int userID) {
		int userIndex = getUserIndexByID(userID);
		panelUsers.removeUser(userIndex);
	}

	public void renameUser(int userID, String newName) {
		int userIndex = getUserIndexByID(userID);
		panelUsers.renameUser(userIndex, newName);		
	}
	
	public void appendText(String text, boolean newLine, SimpleAttributeSet attr) {
		panelChat.appendText(text, newLine, attr);
	}
	
//	public void addMessage(String name, String time, String message, 
//			Color userColor, Color messageColor) 
//	{
//		panelChat.addMessage(name, time, message, userColor, messageColor);
//	}

	public void clearUsers() {
		nodeIDs.clear();
		panelUsers.clearUsers();
	}

	public void clearMessages() {
		panelChat.clearMessages();
	}

	public void resetUnreadCount() {
		unreadCount = 0;
	}
	
	public void incrementUnreadCount() {
		unreadCount++;
	}
	
	public String getTitle() {
		if(unreadCount == 0) {
			return roomName;
		}else if(unreadCount == 1){
			return "(*)" + roomName;
		}else {
			return String.format("(%d)%s", unreadCount, roomName);
		}
	}

	public void setName(String roomName) {
		this.roomName = roomName;
	}
	
}
