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

import java.awt.EventQueue;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import net.project104.chat.solaris.gui.MainFrame;

/**
 * Trying to implement the Model View Presenter, this class
 * transforms data from the Model to suitable data for the View
 * @author civyshk
 * @version 20180319
 */
public class Presenter {
	private View frame;
	private Model model;
	private Map<Integer, Room> rooms;//TODO use BidiMap
	private Map<Room, Integer> roomIDs;//TODO use custom & different types for indexes & IDs?
	private Map<Integer, Node> nodes;
	private Map<Node, Integer> nodeIDs;
	private LinkedList<Room> historyRooms;
	
	private Node selfNode;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Presenter presenter = new Presenter();
		presenter.init();
	}
	
	public Presenter() {
		frame = new MainFrame();
		model = new Model();
		
		rooms = new HashMap<>();
		roomIDs= new HashMap<>();
		nodes = new HashMap<>();
		nodeIDs = new HashMap<>();
		historyRooms = new LinkedList<>();
		
		selfNode = null;
	}
	
	public void init() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame.setPresenter(Presenter.this);
					model.setPresenter(Presenter.this);
					model.init(null);
					
					frame.show();
					connect();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	//From View -------------------------------------------\
	public void sendMessage(String message, int roomID) {
		Room room = rooms.get(roomID);
		model.sendMessage(message, room);
	}
	
	public void setClientName(String name) {
		if(Node.isValidName(name)){
			model.changeSelfName(name);
			frame.renameUser(nodeIDs.get(selfNode), String.format("%s (%s)", selfNode.getDisplayName(), selfNode.getAddress().toString()));
		}else {
			//TODO let view know this invalid name
		}
	}
	
	public void connect() {
		if(!model.isConnected()) {
			model.connect(); 
			frame.setConnected();
		}
	}
	
	public void disconnect() {
		if(model.isConnected()) {
			model.disconnect(); 
			frame.setDisconnected();
		}
	}
	
	public void selectRoom(int roomID) {
		Room room = rooms.get(roomID);
		
		if(room != null) {
			clearCurrentRoom();
			populateRoom(room);
			addRoomToHistory(room);
			frame.showRoom(roomID);
		}else {
			//TODO log a warning
		}
	}
	
	/**
	 * @param nodeID The ID of the node for which a room is to be opened
	 * @return The ID of the room already present or created for this node
	 */
	public int createRoomFor(int nodeID) {
		Node node = nodes.get(nodeID);
		if(node == selfNode) {
			Room publicRoom = model.getPublicRoom();
			if(publicRoom == null) {
				publicRoom = model.createPublicRoom();
			}
			return roomIDs.get(publicRoom);
		}else {
			Room room = model.getRoomOf(node);
			if(room == null) {
				room = model.createRoomFor(node);
			}
			return roomIDs.get(room);
		}
	}
	
	/**
	 * Called from View when the user closes a room
	 * @param roomID
	 */
	public void closeRoom(int roomID) {
		if(rooms.size() <= 1) {
			return;
		}
		
		Room room = rooms.get(roomID);
		if(room != null) {
			model.deleteRoom(room);
			rooms.remove(Integer.valueOf(roomID));
			roomIDs.remove(room);
		}
		
		removeRoomFromHistory(room);
		Room previousRoom = historyRooms.getLast();		
		if(previousRoom != room) {
			nodes.clear();
			nodeIDs.clear();
			populateRoom(previousRoom);
			frame.showRoom(roomIDs.get(previousRoom));
		}
		
		frame.removeRoom(roomID);
	}
	
	public String getRoomTitle(int roomID) {
		return rooms.get(roomID).getName();
	}
	
	public void setBroadcastIP(String broadcast) {
		model.setBroadcastIP(broadcast);
	}
	//From View -------------------------------------------/
	
	/**
	 * For private use, get the necessary data of a room from the Model
	 * and deliver it to the View. Also keep track of selected rooms
	 * to allow later go-back functionality
	 * @param room
	 */
	private void populateRoom(Room room) {
		int roomID = roomIDs.get(room);
		frame.renameRoom(roomID, room.getName());//TODO remove this unnecessary call
		
		nodes.clear();
		nodeIDs.clear();
		for(Node node : model.getUsers(room)) {
			int nodeID = frame.addUser(String.format("%s (%s)", node.getDisplayName(), node.getAddress().toString()), roomID);
			nodes.put(nodeID, node);
			nodeIDs.put(node, nodeID);
			if(model.isSelfNode(node)) {
				selfNode = node;
			}
		}
		
		model.getMessages(room)
			.stream()
			.forEach(m -> sendMessageToView(m, room));
	}

	private void sendMessageToView(Message message, Room room) {
		int roomID = roomIDs.get(room);

		Node origin = message.getOrigin();
		String nodeName;
		if(room == model.getPublicRoom()) {
			nodeName = origin.getUniqueName();
		}else {
			nodeName = origin.getDisplayName();
		}
		
		if(!room.previousMessageHasSameMinute(message)) {
			frame.appendTime(roomID, message.getHour() + ":" + message.getMinute());
			frame.appendUserName(roomID, nodeIDs.get(origin), nodeName);
		}else {
			if(!room.previousMessageHasSameAuthor(message)) {
				frame.appendUserName(roomID, nodeIDs.get(origin), nodeName);
			}
		}
		frame.appendMessage(roomID, nodeIDs.get(origin), message.getMessage(), true);
	}
	
	private void showSystemMessage(String message, String time, Room room) {
		int roomID = roomIDs.get(room);
		frame.appendSystemMessage(message, time, roomID);
	}
	
	private void showSystemMessage(String message, String time, Node node) {
		roomIDs.keySet()
			.stream()
			.filter(r -> r.hasParticipant(node))
			.forEach(r -> showSystemMessage( message, time, r));
	}

	private void clearCurrentRoom() {
		Room currentRoom = getCurrentRoom();
		if(currentRoom != null) {
			int roomID = roomIDs.get(currentRoom);
			frame.clearUsers(roomID);
			frame.clearMessages(roomID);
			nodes.clear();
			nodeIDs.clear();
		}
	}
	
	private Room getCurrentRoom() {
		return historyRooms.peekLast();
	}

	private void removeRoomFromHistory(Room room) {
		historyRooms.remove(room);
	}
	
	private void addRoomToHistory(Room room) {		
		try {
			historyRooms.remove(room);
		}catch(NoSuchElementException e) {
			;;
		}		
		historyRooms.addLast(room);
	}
	
	private void insertRoomToHistory(Room room) {
		try {
			historyRooms.remove(room);
		}catch(NoSuchElementException e) {
			;;
		}		
		historyRooms.addFirst(room);
	}
	
	/**
	 * @param room
	 * @return true if the room is the currently shown room, or if room is null
	 * and there is no currently shown room. Else, false
	 */
	private boolean isRoomShown(Room room) {
		return room == this.getCurrentRoom();
	}
	
	//From Model ------------------------------------------\
	public void roomAvailable(Room room) {
		int roomID = frame.getNewRoomID();
		roomIDs.put(room, roomID);
		rooms.put(roomID, room);

		insertRoomToHistory(room);
		frame.addRoom(roomID, room.getName());
	}

	public void roomNoLongerAvailable(Room room) {
		;;//Not used, not needed in current simple chat model
		throw new UnsupportedOperationException("Implement this");
	}
	
	public void userEnteredRoom(Node node, Room room) {
		Room lastRoom = historyRooms.peekLast();
		if(room == lastRoom) {
			int nodeID = frame.addUser(String.format("%s (%s)", node.getDisplayName(), node.getAddress().toString()), roomIDs.get(room));
			nodes.put(nodeID, node);
			nodeIDs.put(node, nodeID);
			showSystemMessage(
					String.format("%s entered to this room", node.getName()),
					formatHourMinute(System.currentTimeMillis()),
					room);
		}
	}

	public void userChangedName(Node node, String oldName) {
		Integer nodeID = nodeIDs.get(node);
		if(nodeID != null) {
			frame.renameUser(nodeID, String.format("%s (%s)", node.getDisplayName(), node.getAddress().toString()));
			showSystemMessage(
					String.format("%s renamed to %s", oldName, node.getName()),
					formatHourMinute(System.currentTimeMillis()),
					node);
		}
	}
	

	public void roomChangedName(Room room) {
		Integer roomID = roomIDs.get(room);
		if(roomID != null) {
			frame.renameRoom(roomID, room.getName());
		}else {
			//TODO log this. Can this happen?
		}
	}
	
	public void userLeftRoom(Node node, Room room) {
		if(isRoomShown(room)) {
			int nodeID = nodeIDs.get(node);
			frame.removeUser(nodeID, roomIDs.get(room));
			nodes.remove(nodeID);
			nodeIDs.remove(node);
		}
	}
	
	public void userLeft(Node node) {
		Room shownRoom = null;
		Room room = model.getRoomOf(node);
		if(isRoomShown(room)) {
			shownRoom = room;
		}else {
			room = model.getRoomOf(null);
			if(isRoomShown(room)) {
				shownRoom = room;
			}
		}
		
		if(shownRoom != null) {
			frame.removeUser(nodeIDs.get(node), roomIDs.get(shownRoom));
		}
	}
	
	public void roomReceivedMessage(Message message, Room room) {
		if(isRoomShown(room)) {
			sendMessageToView(message, room);
		}else {
			frame.alertRoom(roomIDs.get(room));
		}
	}	
	//From Model ------------------------------------------/

	public static int parseHour(long timestamp) {
		return Integer.valueOf(new SimpleDateFormat("HH").format(new Date(timestamp)));
	}

	public static int parseMinute(long timestamp) {
		return Integer.valueOf(new SimpleDateFormat("mm").format(new Date(timestamp)));
	}

	public static String formatHourMinute(long timestamp) {
		return new SimpleDateFormat("HH:mm").format(new Date(timestamp));
	}
}
