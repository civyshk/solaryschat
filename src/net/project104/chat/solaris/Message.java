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

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
	private Node origin;
	protected String content;
	protected long timestamp;
	protected String timeStr;
	private int timeHour, timeMinute;

	public Message(String content, Node from, long timestamp) {
		this.content = content;
		this.origin = from;
		this.timestamp = timestamp;
		this.timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
		this.timeHour = Presenter.parseHour(timestamp);
		this.timeMinute = Presenter.parseMinute(timestamp);
	}
	
	@Override
	public String toString() {
		return String.format("(%s) %s: %s", timeStr, origin.getDisplayName(), content); 
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public String getTimeStr() {
		return timeStr;
	}
	
	public String getMessage() {
		return content;
	}

	public Node getOrigin() {
		return origin;
	}
	
	public int getHour() {
		return timeHour;
	}

	public int getMinute() {
		return timeMinute;
	}
}
