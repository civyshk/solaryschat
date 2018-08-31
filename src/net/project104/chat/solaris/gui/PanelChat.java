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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;

import net.project104.chat.solaris.UserEventsListener;

public class PanelChat extends JPanel {
	private PanelRoom panelRoom;
	private UserEventsListener eventsListener;
	
	private JTextPane messages;
	private JTextField tfInput;
	private JButton bSend;
	private MouseAdapter sendMessageListener;
	private Action sendMessageEnter;
	private JScrollPane scrollPane;

	public PanelChat(PanelRoom panelRoom) {
		super();
		this.panelRoom = panelRoom;
		this.eventsListener = panelRoom.getEventsListener();
		
		setLayout(new BorderLayout(0, 0));
		
		messages = new JTextPane();
		//messages.setMargin(inset);
		messages.setEditable(false);
		((DefaultCaret) messages.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);//I do custom scroll
		
		//textArea.setCaretPosition(textArea.getDocument().getLength());
		
		scrollPane = new JScrollPane(messages);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);
		
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		tfInput = new JTextField();
		panel.add(tfInput, BorderLayout.CENTER);
		tfInput.setColumns(10);
		
		tfInput.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		
		bSend = new JButton("Send");
		panel.add(bSend, BorderLayout.EAST);
		
		bSend.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				sendMessage();
			}
		});
		
	}

	// 2 methods copied from
	// https://stackoverflow.com/questions/8789371/java-jtextpane-jscrollpane-de-activate-automatic-scrolling
	private boolean isViewAtBottom() {
	    JScrollBar sb = scrollPane.getVerticalScrollBar();
	    int min = sb.getValue() + sb.getVisibleAmount();
	    int max = sb.getMaximum();
//	    System.out.printf("min: %d - max: %d\n", min, max);
	    return min == max;
	}

	private void scrollToBottom() {
	    SwingUtilities.invokeLater( new Runnable() {
	            public void run() {
	            	scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
	            }
	        });
	}
	
	private void sendMessage() {
		int roomID = panelRoom.getRoomID();
		String message = tfInput.getText();
		if(!message.trim().isEmpty()) {
			eventsListener.sendMessage(roomID, message);
		}
		tfInput.setText("");
	}

	public void appendText(String text, boolean newLine, SimpleAttributeSet attributes) {
		StyledDocument doc = messages.getStyledDocument();

		newLine &= doc.getLength() != 0;//If document is empty, skip newLine insertion
		String prefix = newLine ? "\n " : "";
		
		// stackoverflow
		boolean scroll = isViewAtBottom() && newLine;

		try {
			doc.insertString(doc.getLength(), prefix + text, attributes);
		}catch(BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//stackoverflow
	    if (scroll) {
	        scrollToBottom();
	    }
	}
	
//	public void addMessage(String name, String time, String message, 
//			Color userColor, Color messageColor) 
//	{
//		SimpleAttributeSet attrName = new SimpleAttributeSet();
//		StyleConstants.setBold(attrName, true);
//		StyleConstants.setForeground(attrName, userColor);
//		
//		SimpleAttributeSet attrTime = new SimpleAttributeSet();
//		StyleConstants.setItalic(attrTime, true);
//		StyleConstants.setForeground(attrTime, userColor);
//		
//		SimpleAttributeSet attrMsg = new SimpleAttributeSet();
//		StyleConstants.setForeground(attrMsg, messageColor);
//
//		StyledDocument doc = messages.getStyledDocument();
//
//		// stackoverflow
//		boolean scroll = isViewAtBottom();
//		
//		try {
//			doc.insertString( doc.getLength(),"("+time+") ", attrTime);
//			doc.insertString( doc.getLength(), name+": ", attrName);
////			doc.insertString( doc.getLength(), message+"\n", attrMsg);
//			doc.insertString( doc.getLength(), message + '\n', attrMsg);
//		} catch (BadLocationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		//stackoverflow
//	    if (scroll) {
//	        scrollToBottom();
//	    }
//	}

	public void clearMessages() {
		messages.setText("");
	}
	
}
