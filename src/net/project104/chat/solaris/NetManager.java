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
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

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

	private ArrayList<InetAddress> broadcasts = new ArrayList<InetAddress>();
	private InetAddress broadcast = null;
	
	private Model model;
	private Node selfNode;
	private Server server;
	private DatagramSocket socket;
	
	private final String mySigning;
	
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
		
		mySigning = String.valueOf(Math.random());
	}
	
	public InetAddress getBroadcastAddress() {
		if(broadcast == null) {
			return getBestBroadcastAddress();
		}else {
			return broadcast;
		}
	}
	
	public List<InetAddress> getBroadcasts(){
		if(broadcasts.isEmpty()) {
			generateBroadcasts();			
		}
		return broadcasts;
	}
	
	/**
	 * Try to return the best looking broadcast address, according
	 * to similarity to the ip address of the local node
	 * @return The best broadcast address it finds
	 */
	private InetAddress getBestBroadcastAddress() {
		if(broadcasts.isEmpty()) {
			generateBroadcasts();
		}
		
		if(broadcasts.isEmpty()) {
			return broadcasts.get(0);
		}
		
		return null;
	}
	
	/* 
	 * Taken from 
	 * https://stackoverflow.com/questions/4887675/detecting-all-available-networks-broadcast-addresses-in-java
	 */
	private void generateBroadcasts() {
		broadcasts.clear();
	    Enumeration<NetworkInterface> ifaces;
	    try {
	        ifaces = NetworkInterface.getNetworkInterfaces();
	
	        while(ifaces.hasMoreElements()) {
	            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
	
	            if(iface == null) continue;
	
	            if(!iface.isLoopback() && iface.isUp()) {
	                //System.out.println("Found non-loopback, up interface:" + iface);
	
	                Iterator<InterfaceAddress> it = iface.getInterfaceAddresses().iterator();
	                while (it.hasNext()) {
	                    InterfaceAddress address = it.next();
	                    //System.out.println("Found address: " + address);
	                    if(address == null) continue;
	                    InetAddress broadcast = address.getBroadcast();
	                    if(broadcast != null) {
	                        broadcasts.add(broadcast);
	                    }
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        System.err.println("Error while getting network interfaces");
	        ex.printStackTrace();
	    }

		if(!broadcasts.isEmpty()) {		    
			InetAddress bestAddress = broadcasts.get(0);
			int bestScore = 0;
			byte[] localBytes = selfNode.getAddress().getAddress();
			
			for(InetAddress address : broadcasts) {
				byte[] bytes = address.getAddress();
				if(bytes.length == localBytes.length) {
					int score = 0;
					for(int i = 0; i < bytes.length; ++i) {
						score += (bytes[i] == localBytes[i]) ? 1 : 0;
					}
					if(score > bestScore){
						bestAddress = address;
						bestScore = score;
					}
				}
			}
			
			broadcasts.remove(bestAddress);
			broadcasts.add(0, bestAddress);
		}
	}
	
	/**
	 * Sends a broadcast message to let every listening node
	 * know that we are online
	 */
	public void join() {
		selfNode.join();
		InetAddress address = getBroadcastAddress();
		if(address != null) {
			sendJoin(address);
		}else {
			System.err.println("Error, can't obtain a broadcast address");
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
		
		String[] tokens = command.split(" ", 3);
		if(tokens.length != 3) {
			System.err.println("Received command with erroneous length: " + tokens.length);
			return;
		}else if(tokens[0].equals(mySigning)) {
			//message from this same node. Happens with some broadcast addresses
			System.err.println(String.format("Received %s command from this same node. Skip", tokens[1]));
			return;
		}		
		
		switch(tokens[1]) {
			case "MSG":   receivedMsg  (tokens[2], from); break;
			case "JOIN":  receivedJoin (tokens[2], from); break;
			case "LEAVE": receivedLeave(tokens[2], from); break;
			case "HELLO": receivedHello(tokens[2], from); break;
			default: System.err.println("Received bad command: " + tokens[1]);; break;				
		}
	}
	
	private void receivedMsg(String command, InetAddress address) {
		String[] tokens = command.split(" ", 3);
		if(tokens.length != 3){
			System.err.println(String.format("Received bad message with %d fields", tokens.length));
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
	
	private String getSignedCommand(String command) {
		return mySigning + " " + command;
	}
	
	private Message sendMessage(String text, InetAddress address, boolean isPublic) {
		String command = getSignedCommand(String.format("MSG PUBLIC=%s AUTODELETE=%d CONTENT=%s",
				isPublic?"TRUE":"FALSE", -1, text));
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
		byte[] buffer = getSignedCommand(command).getBytes();
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

	/* 
	 * copied from 
	 * https://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java 
	 */
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
		try {
			this.broadcast = InetAddress.getByName(broadcast);
			this.join();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	
}
