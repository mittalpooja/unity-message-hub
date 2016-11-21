package com.unity.messagehub;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import com.unity.messagehub.library.RelayResponseMessage;

public class MessageHub implements Runnable
{
	public final static int SERVER_PORT = 8082;
	
	AtomicLong count = new AtomicLong();
	private ConcurrentHashMap<Socket, Long> clients = null;
	private ConcurrentHashMap<Long, ClientHandler> handlerMap;
	public Queue<RelayResponseMessage> q = null;
	public final static Executor exec = Executors.newFixedThreadPool(256);
	private static ServerSocket server = null;

	public MessageHub() {
		this.clients = new ConcurrentHashMap<Socket, Long>();
		this.q = new LinkedList<RelayResponseMessage>();
		this.handlerMap = new ConcurrentHashMap<Long, ClientHandler>();
	}
	
	public void run() {
		try {
			server = new ServerSocket(SERVER_PORT);
			RelayResponseHandler relayTask = new RelayResponseHandler(handlerMap, q);
			exec.execute(relayTask);
			
			while (true) {
				Socket sock = server.accept();
				count.incrementAndGet();
				clients.put(sock, count.get());
				
				ClientHandler handler = new ClientHandler(sock, count.get(), clients, q);
				handlerMap.put(count.get(), handler);
				exec.execute(handler);
			}
		} catch (Exception e) {
		} finally {
			//server.close();
		}
	}
	
	public void stop() throws IOException {
		if (server!=null) server.close();
	}
	
	public static void main( String[] args ) throws Exception
	{
		MessageHub server = new MessageHub();	
		server.run();
	}
}
