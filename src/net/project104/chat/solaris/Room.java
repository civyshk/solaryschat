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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * @author besokare
 *
 */
public class Room {
	private TreeSet<Node> participants;
	private TreeSet<Message> messages;
	private String name;
	private boolean isPublic;

	public Room(boolean isPublic) {
		this.isPublic = isPublic;
		
		participants = new TreeSet<>((p1, p2) -> {
			byte[] a1 = p1.getAddress().getAddress();
			byte[] a2 = p2.getAddress().getAddress();
			if(a1.length < a2.length) {
				return -1;
			}else if(a1.length > a2.length) {
				return 1;
			}
			
			for(int i = 0; i < a1.length; i++) {
				if(a1[i] < a2[i]) {
					return -1;
				}else if(a1[i] > a2[i]) {
					return 1;
				}
			}
			
			return 0;
		});
		
		messages = new TreeSet<>((m1, m2) -> { 
			long t1 = m1.getTimestamp();
			long t2 = m2.getTimestamp();
			if(t1 < t2) {
				return -1;
			}else if(t1 > t2) {
				return 1;
			}
			return 0;
		});
		name = "Unknown";
	}

	public void addMessage(Message msg) {
		messages.add(msg);
	}
	
	/**
	 * @param node The node to add to this room
	 * @return true if the node was actually added to the room;
	 * false if the node was already present in the room
	 */
	public boolean addParticipant(Node node) {
		return participants.add(node);
	}
	
	public void removeParticipant(Node node) {
		participants.remove(node);
	}
	
	public boolean hasParticipant(Node node) {
		return participants.contains(node);
	}

	public List<Node> getUsers() {
		return new ArrayList<Node>(participants);
	}

	public List<Message> getMessages() {
		return new ArrayList<Message>(messages);
	}
	
	public void setName(String name) {
		if(name != null && !name.trim().isEmpty()) {
			this.name = name;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isPublic() {
		return isPublic;
	}

	/**
	 * @param message
	 * @return True if there is a message at last position with the same author
	 */
	public boolean previousMessageHasSameAuthor(Message message) {
		Message previous = messages.lower(message);
		if(previous == null) {
			return false;
		}
		return messages.lower(message).getOrigin() == message.getOrigin();
			//TODO fix possible bug. lower() doesn't match a message with same timestamp,
			//so 2 messages at the same timestamp can't get a previous one
			//refactor treeset
	}

	/**
	 * @param message
	 * @return True if there is a message at last position with the same hour&minute
	 */
	public boolean previousMessageHasSameMinute(Message message) {
		Message previous = messages.lower(message);
		if(previous == null) {
			return false;
		}
		return previous.getHour() == message.getHour() && previous.getMinute() == message.getMinute();
	}
}
