package com.unity.messagehub;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import com.unity.messagehub.library.RelayResponseMessage;

public class RelayResponseHandler implements Runnable {
	
	ConcurrentHashMap<Long, ClientHandler> handlerMap;
	Queue<RelayResponseMessage> q;
	
	public RelayResponseHandler(ConcurrentHashMap<Long, ClientHandler> handlerMap, 
			Queue<RelayResponseMessage> q2) 
	{
		this.handlerMap = handlerMap;
		this.q = q2;
	}
	
	public void run() {
		while (true) {
			try {
				synchronized(q) {
					if (q.isEmpty()) {
						q.wait();
					}
				}
				RelayResponseMessage msg = q.remove();
				
				for (long key: msg.getReceivers()) {
					if (handlerMap.containsKey(key)) {
						handlerMap.get(key).relay(msg);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
