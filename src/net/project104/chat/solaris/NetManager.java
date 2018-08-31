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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Protocol messages, case sensitive:
 * JOIN  NAME={name} //Used both for announcing as a client and to rename itself
 * LEAVE CONTENT={bye message}
 * MSG   PUBLIC={TRUE|FALSE} AUTODELETE={seconds} CONTENT={message from user}
 *
 * This class manages the sending and receiving of messages 
 * between clients in the same network, using connectionless
 * UDP packets
 * 
 * JOIN -> (NAME=name)
 *      <- HELLO (NAME=name)
 *      
 * MSG <-> (PUBLIC=TRUE|FALSE AUTODELETE=ms CONTENT=content)
 * LEAVE <-> (CONTENT=content)
 * 
 * JOIN <- 
 *      -> HELLO 
 *           
 * @author civyshk
 * @version 20180315
 */
public class NetManager {
	public static final int DEFAULT_PORT = 41315;
	private String broadcast = "192.168.1.255";
//	private String broadcast = "10.2.3.255";
	
	private Model model;
	private Node selfNode;
	private Server server;
	private DatagramSocket socket;
	
	public NetManager(Model model, String userName) {
		this.model = model;
		this.server = null;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO let caller know
			e.printStackTrace();
		}

		if(userName != null) {
			selfNode = new Node(getLocalAddress(), userName);
		}else {
			selfNode = new Node(getLocalAddress());
		}
	}
	
	/**
	 * Sends a broadcast message to let every listening node
	 * know that we are online
	 */
	public void join() {
		selfNode.join();
		InetAddress address = getBroadcastAddress();
		sendJoin(address);
	}
	
	private InetAddress getBroadcastAddress() {
		try {
			return InetAddress.getByName(broadcast);
		} catch (UnknownHostException e) {
			// TODO let caller know
			e.printStackTrace();
			return null;
		}
	}
	
	public void leave() {
		selfNode.leave();
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public void startServer() {
		stopServer();		
		try {
			server = new Server(DEFAULT_PORT);
			server.start();
		}catch(SocketException e) {
			//TODO let caller know this
			e.printStackTrace();
		}
	}
	
	public void stopServer() {
		if(server != null) {
			server.stopServer();
		}
	}
	
	private void received(String command, InetAddress from) {
		if(getSelfNode().getAddress().equals(from)) {
			return;
		}
		
		String[] tokens = command.split(" ", 2);
		if(tokens.length != 2) {
			//TODO log this
			return;
		}
		switch(tokens[0]) {
			case "MSG":   receivedMsg  (tokens[1], from); break;
			case "JOIN":  receivedJoin (tokens[1], from); break;
			case "LEAVE": receivedLeave(tokens[1], from); break;
			case "HELLO": receivedHello(tokens[1], from); break;
			default: /*TODO log this malformed message*/; break;				
		}
	}
	
	private void receivedMsg(String command, InetAddress address) {
		String[] tokens = command.split(" ", 3);
		if(tokens.length != 3){
			//TODO log this malformed message
			return;
		}
		boolean isPublic = false;
		int autoDelete = -1;
		String content = "";
		for(String token : tokens) {
			String[] keyValuePair = token.split("=", 2);
			if(keyValuePair.length != 2) {
				continue;
			}			
			switch(keyValuePair[0]) {
				case "PUBLIC":     isPublic = "TRUE".equals(keyValuePair[1]); break;
				case "AUTODELETE": autoDelete = Integer.valueOf(keyValuePair[1]);
				case "CONTENT":    content = keyValuePair[1].trim();
				default: break;
			}
		}
		
		model.receivedMessage(address, content, autoDelete, isPublic);
	}

	private void receivedJoin(String command, InetAddress address) {
		String name = "";
		String[] keyValuePair = command.split("=", 2);
		if(keyValuePair.length == 2) {
			if(keyValuePair[0].equals("NAME")) {
				name = keyValuePair[1].trim();
			}
		}
		model.nodeJoined(address, name);
	}

	private void receivedLeave(String command, InetAddress address) {		
		String content = "";
		String[] keyValuePair = command.split("=", 2);
		if(keyValuePair.length == 2) {
			if(keyValuePair[0].equals("CONTENT")) {
				content = keyValuePair[1].trim();
			}
		}
		model.nodeLeft(address, content);
	}

	private void receivedHello(String command, InetAddress address) {
		String name = "";
		String[] keyValuePair = command.split("=", 2);
		if(keyValuePair.length == 2) {
			if(keyValuePair[0].equals("NAME")) {
				name = keyValuePair[1].trim();
			}
		}
		model.nodeSaidHello(address, name);
	}
	
	/**
	 * Create and send a Message
	 * @param text The message
	 * @param destination Node to send the private message or null if the message is public
	 * @return The created and sent message
	 */
	public Message sendMessage(String text, Node destination) {
		if(destination != null) {
			return sendMessage(text, destination.getAddress(), false);
		}else {			
			return sendMessage(text, getBroadcastAddress(), true);
		}		
	}
	
	private Message sendMessage(String text, InetAddress address, boolean isPublic) {
		String command = String.format("MSG PUBLIC=%s AUTODELETE=%d CONTENT=%s",
				isPublic?"TRUE":"FALSE", -1, text);
        try {
        	byte[] buffer = command.getBytes("UTF-8");
        	DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, DEFAULT_PORT);
			socket.send(packet);
			return new Message(text, selfNode, System.currentTimeMillis());
		} catch (IOException e) {
			// TODO let caller know this
			e.printStackTrace();
			return null;
		}
	}
	
	public void sendJoin(InetAddress address) {
		String command = String.format("JOIN NAME=%s", selfNode.getName() != null ? selfNode.getName() : selfNode.getDisplayName());
		sendPacket(command, address);
	}

	public void sendLeave(InetAddress address, String content) {
		String command = String.format("LEAVE CONTENT=%s", content);
	    sendPacket(command, address);
	}
	
	public void sendHello(InetAddress address) {
		String command = String.format("HELLO NAME=%s", selfNode.getName() != null ? selfNode.getName() : selfNode.getDisplayName());
		sendPacket(command, address);
	}
	
	private void sendPacket(String command, InetAddress address) {
		byte[] buffer = command.getBytes();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, DEFAULT_PORT);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO let caller know this
			e.printStackTrace();
		}
	}
	
	/**
	 * Launches a thread which accepts incoming messages and redirects 
	 * them to father NetManager class
	 * @author civyshk
	 * @created 20180320
	 */
	private class Server extends Thread{
		private DatagramSocket serverSocket;
		private boolean running;
		private byte[] buffer;
		
		public Server(int port) throws SocketException {
			try {
				serverSocket = new DatagramSocket(port);
				serverSocket.setSoTimeout(10000);
				buffer = new byte[2000];
			} catch (SocketException e) {
				throw e;
			}
		}
		
		public void stopServer() {
			running = false;
			
			//Try to unblock serverSocket to quit faster
			try {
				InetAddress address = InetAddress.getLocalHost();
				byte[] stop = "STOP".getBytes();
				DatagramPacket packet = new DatagramPacket(stop, stop.length, address, serverSocket.getLocalPort());
				serverSocket.send(packet);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			running = true;
			
			while(running) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	            try {
					serverSocket.receive(packet);
	            } catch(SocketTimeoutException e) {
	            	continue;
				} catch (IOException e) {
					//TODO avisar a alguien
					e.printStackTrace();
					running = false;
				}
	            
	            InetAddress address = packet.getAddress();
	            String received;
	            try {
	            	received = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
		        }catch(Exception e) {
		        	received = new String(packet.getData(), 0, packet.getLength());
		        }

	            NetManager.this.received(received, address);
	                         
	            if (address.isLoopbackAddress() && received.equals("STOP")) {
	                running = false;
	                continue;
	            
	            }
	        }
	        serverSocket.close();
		}		
	}

	public Node getSelfNode() {
		return selfNode;
	}

	/* copied from https://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java */
	public static InetAddress getLocalAddress() {
		try(final DatagramSocket socket = new DatagramSocket()){
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			return socket.getLocalAddress();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public boolean isConnected() {
		return server != null;
	}

	public void setBroadcastIP(String broadcast) {
		this.broadcast = broadcast;	
		this.join();
	}
	
	
}
