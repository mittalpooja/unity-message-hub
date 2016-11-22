package com.unity.messagehub;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import com.unity.messagehub.library.RelayResponseMessage;

public class RelayResponseHandler implements Runnable {
	
	ConcurrentHashMap<Long, ClientHandler> handlerMap;
	Queue<RelayResponseMessage> messageQueue;
	
	public RelayResponseHandler(ConcurrentHashMap<Long, ClientHandler> handlerMap, 
			Queue<RelayResponseMessage> q) 
	{
		this.handlerMap = handlerMap;
		this.messageQueue = q;
	}
	
	public void run() {
		while (true) {
			try {
				synchronized(messageQueue) {
					if (messageQueue.isEmpty()) {
						messageQueue.wait();
					}
				}
				RelayResponseMessage msg = messageQueue.remove();
				
				for (long key: msg.getReceivers()) {
					if (handlerMap.containsKey(key)) {
						handlerMap.get(key).relay(msg);
					}
					// Please NOTE: Relay message is lost if the receiver is not yet connected 
					//       to the server
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
