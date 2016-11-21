package com.unity.messagehub;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import com.unity.messagehub.library.Message;
import com.unity.messagehub.library.MessageHubProtocol;
import com.unity.messagehub.library.RelayResponseMessage;

import com.unity.messagehub.library.GetIdResponseMessage;
import com.unity.messagehub.library.GetListResponseMessage;

public class ClientHandler implements Runnable {
	Socket sock;
	long id;
	ConcurrentHashMap<Socket, Long> clients;
	DataOutputStream out = null;
	DataInputStream in = null;
	Queue<RelayResponseMessage> q = null;
	
	public ClientHandler(Socket sock, long id, ConcurrentHashMap<Socket, Long> clients,
			Queue<RelayResponseMessage> q) throws IOException {
		this.sock = sock;
		this.id = id;
		this.clients = clients;
		this.out = new DataOutputStream(new BufferedOutputStream(this.sock.getOutputStream()));
		this.in = new DataInputStream(new BufferedInputStream(this.sock.getInputStream()));
		this.q = q;
	}
	
	public void relay(Message msg) {
		try {
			msg.send(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		//System.out.println("hello");
		// RECEIVE MESSAGE -- at the server end from the client
		try {
			byte msg_type;			
			Message resp = null;
			
			while ((msg_type = in.readByte())!=-1) {
				//System.out.println("msg_type:"+msg_type);
				
				switch(msg_type) {
					case MessageHubProtocol.GET_ID_REQUEST:
						resp = new GetIdResponseMessage(clients.get(sock));
						resp.send(out);
						break;
					case MessageHubProtocol.GET_LIST_REQUEST:
						List<Long> list = new ArrayList<Long>();						
						for (Map.Entry<Socket,Long> entry: clients.entrySet()) {
							if (!entry.getKey().equals(sock)) {
								list.add(entry.getValue());
							}
						}
						resp = new GetListResponseMessage(list);
						resp.send(out);
						break;
					case MessageHubProtocol.RELAY_REQUEST:
						byte numReceivers = in.readByte();
						int messageSize = in.readInt();
						
						long[] receivers = new long[numReceivers];
						for (int i=0; i<numReceivers; i++) {
							receivers[i]=in.readLong();
						}
						byte[] message = new byte[messageSize];
						in.read(message);
						
						RelayResponseMessage reply = new RelayResponseMessage(messageSize, message, receivers);
						synchronized(q) {
							q.add(reply);
							q.notifyAll();
						}
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				clients.remove(sock);
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}