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

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Model assumes each IP has 1 and only 1 Node
 * There is one public room
 * There can be one private room for each node
 * There can't be more rooms
 */
public class Model { 
	public static final String VERSION ="20180326";
	
	private Presenter presenter;
	private NetManager net;
	//BidiMap
	private Map<Node, Room> roomsByNode;
	private Map<Room, Node> nodesByRoom;
	private Map<InetAddress, Node> nodesByAddress;
	
	public Model() {
		roomsByNode = new HashMap<>();
		nodesByRoom = new HashMap<>();
		nodesByAddress = new HashMap<>();
	}	
	
	private Room addRoom(Node node) {
		Room room;
		if(node != null) {
			room = new Room(false);
			room.setName(node.getDisplayName());
			room.addParticipant(node);
		}else {
			room = new Room(true);
			room.setName("Public");
		}
		room.addParticipant(net.getSelfNode());
		roomsByNode.put(node, room);
		nodesByRoom.put(room, node);
		presenter.roomAvailable(room);
		return room;
	}
	
	private void sendMessage(String text, Node destination) {
		Message msg = net.sendMessage(text, destination);
		Room room = roomsByNode.get(destination);
		room.addMessage(msg);
		presenter.roomReceivedMessage(msg, room);
	}
	
	//From Presenter --------------------------------------\
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
	
	public void init(String userName) {
		net = new NetManager(this, userName);
		addRoom(null);
		presenter.showBroadcasts(net.getBroadcasts());
	}
	
	public void connect() {
		net.startServer();
		net.join();
	}
	
	public void disconnect() {
		net.stopServer();
		net.leave();
		//TODO remove room[s]
	}
	
	public void changeSelfName(String name) {
		net.getSelfNode().setName(name);
		nodesByAddress.values()
			.stream()
			.filter(Node::isJoined)
			.filter(n -> n != net.getSelfNode())
			.map(Node::getAddress)
			.forEach(net::sendHello);
	}
	
	public Collection<Room> getRooms() {
		return roomsByNode.values();
	}
	
	public List<Node> getUsers(Room room) {
		return room.getUsers();
	}
	
	public List<Message> getMessages(Room room) {
		return room.getMessages();
	}
	
	public void sendMessage(String message, Room room) {
		if(room.isPublic()) {
			sendMessage(message, (Node) null);//this will broadcast the message
		}else {
			room.getUsers()
				.stream()
				.filter(n -> n != net.getSelfNode())
				.forEach(n -> sendMessage(message, n));
		}
	}
	
	public Room getRoomOf(Node node) {
		return roomsByNode.get(node);
	}
	
	public Room getPublicRoom() {
		return getRoomOf(null);
	}
	
	public Room createRoomFor(Node node) {
		Room room = roomsByNode.get(node);
		if(room == null) {
			room = addRoom(node);
		}else {
			//TODO log this warning. 
		}
		return room;
	}
	
	public Room createPublicRoom() {
		Room room = createRoomFor(null);
		net.join();
		return room;
	}
	
	public void deleteRoom(Room room) {
		Node other = nodesByRoom.remove(room);
		Room removedRoom = roomsByNode.remove(other);
		if(removedRoom != room) {
			//TODO log this warning
		}
	}
	
	public boolean isSelfNode(Node node) {
		return node == net.getSelfNode();
	}
	
	public void setBroadcastIP(String broadcast) {
		net.setBroadcastIP(broadcast);
	}
	//From Presenter --------------------------------------/
	
	//From NetManager -------------------------------------\
	public void receivedMessage(InetAddress address, String content, int autoDelete, boolean isPublic) {
		Node origin = getNode(address);
		if(origin == null) {
			nodeJoined(address, null);
			origin = getNode(address);
		}
		
		if(content.trim().isEmpty()) {
			return;
		}
		
		Message msg = new Message(content, origin, System.currentTimeMillis());
		Room room;
		if(isPublic) {
			room = roomsByNode.get(null);
			if(room == null) {
				addRoom(null);
				room = getPublicRoom();
				room.addParticipant(origin);
				net.join();
			}
		}else {
			room = roomsByNode.get(origin);
			if(room == null) {
				addRoom(origin);
				room = roomsByNode.get(origin);
				room.addParticipant(origin);
			}
		}
		room.addMessage(msg);
		presenter.roomReceivedMessage(msg, room);
		
		//TODO autodelete
	}

	public Node getNode(InetAddress address) {
		return nodesByAddress.get(address);
	}
	
	public void nodeJoined(InetAddress address, String name) {
		Node node = getNode(address);
		if(node == null) {
			node = new Node(address, name);
			node.join();
			nodesByAddress.put(address, node);
			boolean added = addNodeToPublicRoom(node);
			if(added) {
				presenter.userEnteredRoom(node, getPublicRoom());
			}
		}else {
			node.setName(name);
		}
		net.sendHello(address);
	}
	
	/**
	 * @param node Node to add to the public room
	 * @return true if there is a public room and this node was added to it;
	 * false if there is no public room or if the node was already present
	 */
	private boolean addNodeToPublicRoom(Node node) {
		Room publicRoom = getPublicRoom();
		if(publicRoom != null) {
			return publicRoom.addParticipant(node);
		}
		return false;
	}
	
	public void nodeSaidHello(InetAddress address, String name) {
		Node node = nodesByAddress.get(address);
		if(node == null) {
			node = new Node(address, name);
			node.join();
			nodesByAddress.put(address, node);
		}else {
			String oldName = node.getUniqueName();
			node.setName(name);
			Room room  = getRoomOf(node);
			if(room != null) {
				room.setName(node.getDisplayName());
				presenter.roomChangedName(room);;
			}
			presenter.userChangedName(node, oldName);
		}
		
		Room publicRoom = getPublicRoom();
		if(publicRoom != null) {
			boolean added = addNodeToPublicRoom(node);
			if(added) {
				presenter.userEnteredRoom(node, getPublicRoom());	
			}
		}
	}
	
	public void nodeLeft(InetAddress address, String content) {
		Node node = getNode(address);
		if(node != null) {
			node.leave();
			presenter.userLeft(node);
			nodesByAddress.remove(node.getAddress());
			roomsByNode.get(null).removeParticipant(node);
		}
	}
	//From NetManager -------------------------------------/

	public boolean isConnected() {
		return net.isConnected();
	}
}
