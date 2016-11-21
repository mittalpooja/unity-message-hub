package com.unity.messagehub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.unity.messagehub.library.MessageHubProtocol;

/**
 * Unit test for MessageHub Server.
 */
public class MessageHubTest
// extends TestCase
{
	public final static ExecutorService exec = Executors.newFixedThreadPool(10);

	@Test
	public void testGetId() {
		MessageHub server = new MessageHub();	
			try {
				exec.execute(server);
				Socket client = new Socket("localhost", MessageHub.SERVER_PORT);
				DataOutputStream out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
				out.writeByte(MessageHubProtocol.GET_ID_REQUEST);
				out.flush();
				
				DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
				assertEquals(in.readByte(),MessageHubProtocol.GET_ID_RESPONSE);
				assertEquals(in.readLong(),1);
				//server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {	
				try {
					server.stop();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
	}

	
	@Test
	public void testGetList() {
		MessageHub server = new MessageHub();	
			try {
				exec.execute(server);
				Socket client = new Socket("localhost", MessageHub.SERVER_PORT);
				Socket client2 = new Socket("localhost", MessageHub.SERVER_PORT);
				Socket client3 = new Socket("localhost", MessageHub.SERVER_PORT);
				DataOutputStream out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
				out.writeByte(MessageHubProtocol.GET_LIST_REQUEST);
				out.flush();
				
				DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
				assertEquals(in.readByte(),MessageHubProtocol.GET_LIST_RESPONSE);
				assertEquals(in.readByte(),2);
				HashSet<Long> set = new HashSet<Long>();
				set.add(in.readLong());
				set.add(in.readLong());
		
				Long id1 = new Long(1);
				Long id2 = new Long(2);
				Long id3 = new Long(3);
				assertTrue(set.contains(id2));
				assertTrue(set.contains(id3));
				
				DataOutputStream out2 = new DataOutputStream(new BufferedOutputStream(client2.getOutputStream()));
				out2.writeByte(MessageHubProtocol.GET_LIST_REQUEST);
				out2.flush();
				DataInputStream in2 = new DataInputStream(new BufferedInputStream(client2.getInputStream()));
				assertEquals(in2.readByte(),MessageHubProtocol.GET_LIST_RESPONSE);
				assertEquals(in2.readByte(),2);
				HashSet<Long> set2 = new HashSet<Long>();
				set2.add(in2.readLong());
				set2.add(in2.readLong());
				
				assertTrue(set2.contains(id1));
				assertTrue(set2.contains(id3));
				
				//server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {	
				try {
					server.stop();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
	}

	@Test
	public void testRelay() {
		MessageHub server = new MessageHub();	
		try {
			exec.execute(server);
			Socket client = new Socket("localhost", MessageHub.SERVER_PORT);
			Socket client2 = new Socket("localhost", MessageHub.SERVER_PORT);
			Socket client3 = new Socket("localhost", MessageHub.SERVER_PORT);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
			out.writeByte(MessageHubProtocol.RELAY_REQUEST);
			out.writeByte(2);
			String message = "hi there";
			byte[] msg = message.getBytes();
			out.writeInt(msg.length);
			out.writeLong(new Long(2));
			out.writeLong(new Long(3));
			out.write(msg);
			out.flush();
			
			DataInputStream in2 = new DataInputStream(new BufferedInputStream(client2.getInputStream()));
			assertEquals(in2.readByte(),MessageHubProtocol.RELAY_RESPONSE);
			assertEquals(in2.readInt(),msg.length);
			byte[] msg2 = new byte[msg.length];
			in2.read(msg2);
			for (int i=0; i<msg.length; i++) {
				assertEquals(msg2[i],msg[i]);
			}
			
			DataInputStream in3 = new DataInputStream(new BufferedInputStream(client3.getInputStream()));
			assertEquals(in3.readByte(),MessageHubProtocol.RELAY_RESPONSE);
			assertEquals(in3.readInt(),msg.length);
			byte[] msg3 = new byte[msg.length];
			in3.read(msg3);
			for (int i=0; i<msg.length; i++) {
				assertEquals(msg3[i],msg[i]);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {	
			try {
				server.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}
}
