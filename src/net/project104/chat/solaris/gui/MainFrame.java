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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import net.project104.chat.solaris.Presenter;
import net.project104.chat.solaris.UserEventsListener;
import net.project104.chat.solaris.View;

public class MainFrame extends JFrame implements View, UserEventsListener{
		
	private JPanel contentPane;
	private JTextField tfName;

	private JButton bConnect;
	private JButton bDisconnect;
	private JTabbedPane tpRooms;
	
	private Presenter presenter;
	private ArrayList<Integer> roomIDs;
	private Random rand;
	private Component verticalGlue;
	private JLabel lblName;
	private JTextField tfIP;
	private JLabel lblIP;
	
	private Map<Integer, SimpleAttributeSet[]> styles;
	private SimpleAttributeSet timeAttributes;
	
	/**
	 * Create the frame.
	 */
	public MainFrame() {		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 300);
//		setTitle("Solaris Chat (" + Model.VERSION + ")");
		setTitle("Solarys Chat");
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JPanel pActions = new JPanel();
		pActions.setLayout(new BoxLayout(pActions, BoxLayout.PAGE_AXIS));
		contentPane.add(pActions, BorderLayout.LINE_START);
		
		lblName = new JLabel("Name:");
		lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblName.setHorizontalAlignment(SwingConstants.RIGHT);
		pActions.add(lblName);
		
		tfName = new JTextField();
		tfName.setToolTipText("Your name");
		tfName.setColumns(10);
		tfName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setClientName(tfName.getText());
			}
		});
		pActions.add(tfName);
		
		lblIP = new JLabel("Broadcast IP:");
		lblIP.setAlignmentX(Component.CENTER_ALIGNMENT);
		pActions.add(lblIP);
		
		tfIP = new JTextField();
		tfIP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setBroadcastIP(tfIP.getText());
			}
		});
		
		//TODO get this from NetManager
		tfIP.setText("192.168.1.255");
		tfIP.setColumns(10);
		pActions.add(tfIP);
		
		verticalGlue = Box.createVerticalGlue();
		verticalGlue.setPreferredSize(new Dimension(1, Short.MAX_VALUE));
		verticalGlue.setMaximumSize(new Dimension(1, Short.MAX_VALUE));
		pActions.add(verticalGlue);
		
		bConnect = new JButton("Connect");
		bConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
		bConnect.setEnabled(false);
		bConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				connect();
			}
		});
		
		pActions.add(bConnect);
		
		bDisconnect = new JButton("Disconnect");
		bDisconnect.setAlignmentX(Component.CENTER_ALIGNMENT);
		bDisconnect.setEnabled(false);
		bDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				disconnect();
			}
		});
		pActions.add(bDisconnect);
		
		tpRooms = new JTabbedPane(JTabbedPane.TOP);
		tpRooms.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				selectChat(roomIDs.get(tpRooms.getSelectedIndex()));
			}
		});
		contentPane.add(tpRooms, BorderLayout.CENTER);

		roomIDs = new ArrayList<>();
		styles = new HashMap<>();
		timeAttributes = new SimpleAttributeSet();
		StyleConstants.setItalic(timeAttributes, true);
		rand = new Random();
	}

	/**
	 * The presenter must be set before the JFrame is shown to the user
	 */
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	private int generateRoomID() {
		int roomID;
		do{
			roomID = rand.nextInt();
		}while(roomIDs.contains(roomID));
		return roomID;
	}
	
	private PanelRoom createRoomPane(int roomID, String roomName) {
		return new PanelRoom(this, roomID, roomName);
	}

	private int getRoomIndexByID(int roomID) {
		return roomIDs.indexOf(roomID);
	}
	
	private PanelRoom getCurrentPanelRoom() {
		return (PanelRoom) tpRooms.getSelectedComponent();
	}
	
	private PanelRoom getPanelRoom(int roomID) {
		return (PanelRoom) tpRooms.getComponentAt(getRoomIndexByID(roomID));
	}
	
	private void selectTab(int roomID) {
		int roomIndex = getRoomIndexByID(roomID);
		PanelRoom panelRoom = getPanelRoom(roomID);
		panelRoom.resetUnreadCount();
		tpRooms.setTitleAt(roomIndex, panelRoom.getTitle());		
		tpRooms.setSelectedIndex(roomIndex);
	}
	
	private void addStyle(int nodeID) {
		SimpleAttributeSet[] attributes = styles.get(nodeID);
		if(attributes == null) {
			Color color = Colors.getUniqueColor();
			
			attributes = new SimpleAttributeSet[2];
			attributes[0] = new SimpleAttributeSet();
			StyleConstants.setItalic(attributes[0], true);
			StyleConstants.setForeground(attributes[0], color);
			StyleConstants.setBold(attributes[0], true);

			attributes[1] = new SimpleAttributeSet();
			StyleConstants.setForeground(attributes[1], color);
			
			styles.put(nodeID, attributes);
		}else {
			//TODO log a warning
		}
	}
	
	private SimpleAttributeSet getTimeAttributes() {
		return timeAttributes;
	}
	
	private SimpleAttributeSet getUserNameAttributes(int nodeID) {
		return styles.get(nodeID)[0];
	}
	
	private SimpleAttributeSet getUserMessageAttributes(int nodeID) {
		return styles.get(nodeID)[1];
	}
	
	//Interface UserEventsListener-------------------------\
	@Override
	public void connect() {
		presenter.connect();
	}
	
	@Override
	public void disconnect() {
		presenter.disconnect();
	}
	
	@Override
	public void setClientName(String name) {
		presenter.setClientName(name);
	}
	
	@Override
	public void selectChat(int roomID) {
		presenter.selectRoom(roomID);
	}
	
	@Override
	public void openChatFor(int userID) {
		int roomID = presenter.createRoomFor(userID);
		presenter.selectRoom(roomID);
//		this.selectTab(roomID);
	}
	
	@Override
	public void closeChat(int roomID) {
		presenter.closeRoom(roomID);
	}
	
	@Override
	public void sendMessage(int roomID, String message) {
		presenter.sendMessage(message, roomID);
	}
	
	@Override
	public void setBroadcastIP(String broadcast) {
		presenter.setBroadcastIP(broadcast);
	}
	//Interface UserEventsListener-------------------------/

	
	//Interface View---------------------------------------\
	@Override
	public void setConnected() {
		bConnect.setEnabled(false);
		tfIP.setEnabled(true);
//		bDisconnect.setEnabled(true);
		bDisconnect.setEnabled(false);
	}

	@Override
	public void setDisconnected() {
		bConnect.setEnabled(true);
		tfIP.setEnabled(true);
		bDisconnect.setEnabled(false);
	}
	
	@Override
	public int getNewRoomID() {
		return generateRoomID();
	}

	@Override
	public void addRoom(int roomID, String name) {
		PanelRoom roomPane = createRoomPane(roomID, name) ;
		roomIDs.add(roomID);
		tpRooms.add(roomPane.getTitle(), roomPane);
	}

	@Override
	public void showRoom(int roomID) {
		this.selectTab(roomID);
	}
	
	@Override
	public void removeRoom(int roomID) {
		int roomIndexToRemove = getRoomIndexByID(roomID);
		roomIDs.remove(Integer.valueOf(roomID));//force usage of remove(Object), not remove(int)
		tpRooms.remove(roomIndexToRemove);
		//TODO CHECK is desired tab selected now, according to history
	}

	@Override
	public void renameRoom(int roomID, String newName) {
		int roomIndex = getRoomIndexByID(roomID);
		PanelRoom panelRoom = getPanelRoom(roomID);
		panelRoom.setName(newName);
		tpRooms.setTitleAt(roomIndex, panelRoom.getTitle());
	}

	@Override
	public void alertRoom(int roomID) {
		int roomIndex = getRoomIndexByID(roomID);
		PanelRoom panelRoom = getPanelRoom(roomID);
		panelRoom.incrementUnreadCount();
		tpRooms.setTitleAt(roomIndex, panelRoom.getTitle());
	}

	@Override
	public int addUser(String name, int roomID) {
		PanelRoom panelRoom = getPanelRoom(roomID);
		int nodeID = panelRoom.addUser(name);
		
		//TODO lo de SimpleAttributeSet attrTime = new SimpleAttributeSet();
		addStyle(nodeID);
		
		return nodeID;
	}

	@Override
	public void removeUser(int userID, int roomID) {
		PanelRoom panelRoom = getPanelRoom(roomID);
		panelRoom.removeUser(userID);
	}
	
	@Override
	public void renameUser(int userID, String newName) {
		PanelRoom panelRoom = getCurrentPanelRoom();
		panelRoom.renameUser(userID, newName);
		//TODO rename room of this user as well, if it exists
	}

	@Override
	public void clearUsers(int roomID) {
		PanelRoom panelRoom = getPanelRoom(roomID);
		panelRoom.clearUsers();
	}
	
	@Override
	public void clearMessages(int roomID) {
		PanelRoom panelRoom = getPanelRoom(roomID);
		panelRoom.clearMessages();		
	}

	@Override
	public void appendTime(int roomID, String time) {
		getPanelRoom(roomID).appendText(time, true, getTimeAttributes());
	}
	
	@Override
	public void appendUserName(int roomID, int userID, String name) {
		getPanelRoom(roomID).appendText(name, true, getUserNameAttributes(userID));
	}
	
	@Override
	public void appendMessage(int roomID, int userID, String text, boolean newLine) {
		getPanelRoom(roomID).appendText(text, newLine, getUserMessageAttributes(userID));
	}
	//Interface View---------------------------------------/

	@Override
	public void appendSystemMessage(String message, String time, int roomID) {
		// TODO Auto-generated method stub
		
	}
	
}
