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

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.project104.chat.solaris.UserEventsListener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import javax.swing.JButton;

public class PanelUsers extends JPanel {

	private JList<String> listUsers;
	
	private PanelRoom panelRoom;
	private UserEventsListener bossFrame;
	private DefaultListModel<String> model;
	
	public PanelUsers(PanelRoom panelRoom) {
		super();
		this.panelRoom = panelRoom;
		bossFrame = panelRoom.getEventsListener();
		model = new DefaultListModel<>();
		listUsers = new JList<String>(model);
		listUsers.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				//TODO detect double click and open room with this:
				if(arg0.getClickCount() == 2) {
					bossFrame.openChatFor(PanelUsers.this.panelRoom.getUserIDByIndex(listUsers.getSelectedIndex()));
				}
			}
		});
		setLayout(new BorderLayout(0, 0));
		add(listUsers);
		
		JButton btnClose = new JButton("Close room");
		btnClose.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				bossFrame.closeChat(panelRoom.getRoomID());
			}
		});
		add(btnClose, BorderLayout.SOUTH);
		
		
	}
	
	public void addUser(String name) {
		model.addElement(name);
	}

	public void removeUser(int userIndex) {
		model.removeElementAt(userIndex);
	}

	public void renameUser(int userIndex, String newName) {
		model.set(userIndex, newName);
	}

	public String getUserName(int userIndex) {
		return model.getElementAt(userIndex);
	}

	public void clearUsers() {
		model.clear();
	}

}
