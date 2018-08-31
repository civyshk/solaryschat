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

import java.awt.Color;
import java.net.InetAddress;

import net.project104.chat.solaris.gui.Colors;

/**
 * Node
 * @author civyshk
 * @version 20180312
 */
public class Node {
	private InetAddress address;
	private String name;
	private long lastHeard;
	private boolean joined;
	private Color color;
		
	public Node(InetAddress address) {
		this(address, "");
	}

	public Node(InetAddress address, String name) {
		this.address = address;
		this.name = name;
	}

	public InetAddress getAddress() {
		return address;
	}
	
	public void setName(String name) {
		if(isValidName(name)) {
			this.name = name.trim();
		}else {
			//Log this invalid name
		}
	}

	static public boolean isValidName(String name) {
		return (name != null && !name.trim().isEmpty() && name.length() < 30);
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		if(name == null || name.isEmpty()) {
			return address.toString();
		}
		return name;
	}
	
	public String getUniqueName() {
		String result = "[" + address.toString() + "]";
		if(name != null && !name.isEmpty()) {
			result += " " + name;
		}
		return result;
	}
	
	public void updateHeard() {
		lastHeard = System.currentTimeMillis();
	}
	
	public void leave() {
		joined = false;
		Colors.unuseColor(color);
		updateHeard();
	}
	
	public void join() {
		joined = true;
		//if called twice, colors from Colors can be wasted
		color = Colors.getUniqueColor();
		updateHeard();
	}

	public boolean isJoined() {
		return joined;
	}
	
	public Color getColor() {
		if(color == null) {
			throw new IllegalStateException("Color attribute is not set. Has .join() been called?"); 
			//TODO move Color initialization out of join
		}
		return color;
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s)", getDisplayName(), getAddress().toString());
	}
}
