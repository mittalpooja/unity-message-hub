package com.unity.messagehub;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import com.unity.messagehub.library.GetIdResponseMessage;
import com.unity.messagehub.library.GetListResponseMessage;
import com.unity.messagehub.library.Message;
import com.unity.messagehub.library.MessageHubProtocol;
import com.unity.messagehub.library.RelayRequestMessage;
import com.unity.messagehub.library.RelayResponseMessage;

public class ClientHandler implements Runnable {
	Socket sock; // socket opened for this client
	long id; // id assigned to this client by the server
	
	private ConcurrentHashMap<Long, ClientHandler> handlerMap; //Keeps track of the clients connected to this server 
	 														   // for the getList command
	
	Queue<RelayResponseMessage> messageQueue = null; // Queue for the relay messages received by this client handler
	DataOutputStream out = null;
	DataInputStream in = null;
	
	public ClientHandler(Socket sock, long id, ConcurrentHashMap<Long, ClientHandler> handlerMap,
			Queue<RelayResponseMessage> q) throws IOException {
		this.sock = sock;
		this.id = id;
		this.handlerMap = handlerMap;
		this.out = new DataOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
		this.in = new DataInputStream(new BufferedInputStream(this.sock.getInputStream()));
		this.messageQueue = q;
	}
	
	public void relay(Message msg) {
		try {
			msg.send(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		// RECEIVE MESSAGE -- at the server end from the client
		try {
			byte msg_type;			
			Message resp = null;
			
			while ((msg_type = in.readByte())!=-1) {
				
				switch(msg_type) {
					case MessageHubProtocol.GET_ID_REQUEST:
						resp = new GetIdResponseMessage(this.id);
						resp.send(out);
						break;
					case MessageHubProtocol.GET_LIST_REQUEST:
						List<Long> list = new ArrayList<Long>();						
						for (Long key: handlerMap.keySet()) {
							if (!key.equals(this.id)) {
								list.add(key);
							}
						}
						resp = new GetListResponseMessage(list);
						resp.send(out);
						break;
					case MessageHubProtocol.RELAY_REQUEST:
						RelayRequestMessage request = new RelayRequestMessage(in);
						RelayResponseMessage reply = new RelayResponseMessage(request.getMessageSize(), 
								request.getMessage(), request.getReceivers());
						synchronized(messageQueue) {
							messageQueue.add(reply);
							messageQueue.notifyAll();
						}
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (sock!=null) handlerMap.remove(id);
				if (sock!=null) sock.close();
				if (in!=null) in.close();
				if (out!=null) out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}