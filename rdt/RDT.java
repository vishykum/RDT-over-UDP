
/**
 * @author mohamed
 *
 */
package rdt;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RDT {

	public static final int MSS = 100; // Max segement size in bytes (CHANGE THIS BACK TO 100)
	public static final int RTO = 1000; // Retransmission Timeout in msec
	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;  
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat
	public static final int protocol = GBN; //Change to GBN
	
	public static double lossRate = 0.0;
	public static Random random = new Random(); 
	public static Timer timer = new Timer();	
	
	private DatagramSocket socket; 
	private InetAddress dst_ip;
	private int dst_port;
	private int local_port; 
	
	private RDTBuffer sndBuf;
	private RDTBuffer rcvBuf;

	private int currentSeq;
	
	private ReceiverThread rcvThread;  
	private ReceiverThread rcvThread1;  
	
	
	RDT (String dst_hostname_, int dst_port_, int local_port_) 
	{
		local_port = local_port_;
		dst_port = dst_port_; 
		try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(MAX_BUF_SIZE);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else 
			rcvBuf = new RDTBuffer(MAX_BUF_SIZE);

		currentSeq = -1;
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}
	
	RDT (String dst_hostname_, int dst_port_, int local_port_, int sndBufSize, int rcvBufSize)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		 try {
			 socket = new DatagramSocket(local_port);
			 dst_ip = InetAddress.getByName(dst_hostname_);
		 } catch (IOException e) {
			 System.out.println("RDT constructor: " + e);
		 }
		sndBuf = new RDTBuffer(sndBufSize);
		if (protocol == GBN)
			rcvBuf = new RDTBuffer(1);
		else 
			rcvBuf = new RDTBuffer(rcvBufSize);

		currentSeq = -1;
		
		rcvThread = new ReceiverThread(rcvBuf, sndBuf, socket, dst_ip, dst_port);
		rcvThread.start();
	}
	
	public static void setLossRate(double rate) {lossRate = rate;}
	
	// called by app
	// returns total number of sent bytes  
	public int send(byte[] data, int size) {
		
		//****** complete
		

		// System.out.println("Segments: " + segments);
		int mss = MSS;

		if (size > mss) {
			// divide data into segments
			int segments = 0;
			for(int i = 0; i < size; i++) {
				if (i%mss == 0) {
					segments++;
				}
			}

			System.out.println("Segments: " + segments);

			if (segments > sndBuf.size) {
				System.out.println("More packets than send window");
				segments = sndBuf.size;
				System.out.println("Only sending first " + segments + " packets...");
			}

			for(int i = 0; i < segments; i++) {
				byte[] sndData = new byte[mss];
				for(int j = 0; j < mss; j++) {
					int add = mss*i;
					sndData[j] = data[j + add];
				}

				send(sndData, mss);
			}

		}

		else {
			// put each segment into sndBuf
			RDTSegment snd = new RDTSegment();
			snd.setData(data, data.length);
			snd.seqNum = sndBuf.next;

			// send using udp_send()
			// RDTSegment snd = sndBuf.buf[sndBuf.current];

			snd.timeoutHandler = new TimeoutHandler(sndBuf, snd, socket, dst_ip, dst_port);
			timer.schedule(snd.timeoutHandler, RTO);

			sndBuf.putNext(snd);

			// System.out.println("base: " + sndBuf.base);

			Utility.udp_send(snd, socket, dst_ip, dst_port);

			// schedule timeout for segment(s) 
		}


			
		return size;
	}
	
	
	// called by app
	// receive one segment at a time
	// returns number of bytes copied in buf
	public int receive (byte[] buf, int size)
	{
		//*****  complete

		// System.out.println("In receive()");


		// if (protocol == GBN) {
			RDTSegment seg = rcvBuf.getNext();
			seg.makePayload(buf);
		// }


		return size;   // fix
	}
	
	// called by app
	public void close() {
		// OPTIONAL: close the connection gracefully
		// you can use TCP-style connection termination process

		sndBuf.dump();

		// for(int i = 0; i < )
	}
	
}  // end RDT class 


class RDTBuffer {
	public RDTSegment[] buf;
	public int size;	
	public int base;
	public int next;
	public boolean isEmpty;
	public int current;
	public Semaphore semMutex; // for mutual execlusion
	public Semaphore semFull; // #of full slots
	public Semaphore semEmpty; // #of Empty slots
	
	RDTBuffer (int bufSize) {
		buf = new RDTSegment[bufSize];
		for (int i=0; i<bufSize; i++)
			buf[i] = null;
		size = bufSize;
		isEmpty = true;
		base = next = current = 0;
		semMutex = new Semaphore(1, true);
		semFull =  new Semaphore(0, true);
		semEmpty = new Semaphore(bufSize, true);
	}

	
	
	// Put a segment in the next available slot in the buffer
	public void putNext(RDTSegment seg) {		
		try {
			semEmpty.acquire(); // wait for an empty slot 
			semMutex.acquire(); // wait for mutex 
				isEmpty = false;
				buf[next%size] = seg;
				next++;  
			semMutex.release();
			semFull.release(); // increase #of full slots
		} catch(InterruptedException e) {
			System.out.println("Buffer put(): " + e);
		}
	}
	
	// return the next in-order segment
	public RDTSegment getNext() {
		
		// **** Complete

		RDTSegment seg = new RDTSegment();

		try {
			semFull.acquire(); // wait for an full slot 
			semMutex.acquire(); // wait for mutex 
			// System.out.println("Get next: " + seg.seqNum);
			seg = buf[base];
			base++;
			base = (base % size);
			semMutex.release();
			semEmpty.release(); // increase #of empty slots
		} catch(InterruptedException e) {
			System.out.println("Buffer put(): " + e);
		}

		return seg;  // fix 
	}
	
	// Put a segment in the *right* slot based on seg.seqNum
	// used by receiver in Selective Repeat
	public void putSeqNum (RDTSegment seg) {
		// ***** compelte
		// seg.seqNum = next;

	}
	
	// for debugging
	public void dump() {
		System.out.println("Dumping the receiver buffer ...\n");
		// Complete, if you want to 
		System.out.print("Printing list: ");
		for(int i = 0; i < next; i++) {
			if (i >= size) break;

			RDTSegment seg = buf[i];
			System.out.print(seg.seqNum + " ");
			
		}
		System.out.println("\n");
		
	}
} // end RDTBuffer class



class ReceiverThread extends Thread {
	RDTBuffer rcvBuf, sndBuf;
	DatagramSocket socket;
	InetAddress dst_ip;
	int dst_port;
	
	ReceiverThread (RDTBuffer rcv_buf, RDTBuffer snd_buf, DatagramSocket s, 
			InetAddress dst_ip_, int dst_port_) {
		rcvBuf = rcv_buf;
		sndBuf = snd_buf;
		socket = s;
		dst_ip = dst_ip_;
		dst_port = dst_port_;
	}	
	public void run() {
		
		// *** complete 
		// Essentially:  while(cond==true){  // may loop for ever if you will not implement RDT::close()  
		//                socket.receive(pkt)
		//                seg = make a segment from the pkt
		//                verify checksum of seg
		//	              if seg contains ACK, process it potentailly removing segments from sndBuf
		//                if seg contains data, put the data in rcvBuf and do any necessary 
		//                             stuff (e.g, send ACK)
		//


		while(true) {
			int size = RDT.MSS + RDTSegment.HDR_SIZE;
			byte[] buf = new byte[size];

			DatagramPacket recvPack = new DatagramPacket(buf, size);
			
			try {
				socket.receive(recvPack);
			} catch (Exception e) {
				System.out.println("receive: " + e);
			}

			byte[] buffer = recvPack.getData();
			for(int i = 0; i < size; i++) {
				buf[i] = buffer[i];
			}

			RDTSegment seg = new RDTSegment();

			makeSegment(seg, buf);

			if (seg.containsAck()) {
				//Do stuff
				System.out.println("ACK " + seg.ackNum + " received!");

					if (sndBuf.buf[sndBuf.base].seqNum == seg.ackNum) {
						RDTSegment cancel = sndBuf.getNext();	
						cancel.timeoutHandler.cancel();
						cancel.ackReceived = true;
					}


					System.out.print("Printing list: ");
					for(int i = 0; i < sndBuf.next; i++) {
					if (i >= sndBuf.size) break;

					RDTSegment segs = sndBuf.buf[i];
					System.out.print(segs.seqNum + " ");
					
					}
					System.out.println("\n");

			}

			else {

				if (RDT.protocol == RDT.GBN) {
					if (!rcvBuf.isEmpty) {
						RDTSegment check = rcvBuf.buf[rcvBuf.base];
						
						if (check.seqNum+1 != seg.seqNum) {
							//Send ACK
							RDTSegment ack = new RDTSegment();
							ack.seqNum = 0;
							ack.ackNum = seg.seqNum;

							ack.flags = 1;

							// send using udp_send()
							Utility.udp_send(ack, socket, dst_ip, dst_port);
							continue;
						}
					}

					else if (rcvBuf.isEmpty && (seg.seqNum != 0)) {
						continue;
					}
				}
				

				rcvBuf.putNext(seg);

				//Send ACK
				RDTSegment ack = new RDTSegment();
				ack.seqNum = 0;
				ack.ackNum = seg.seqNum;

				ack.flags = 1;

				// send using udp_send()
				Utility.udp_send(ack, socket, dst_ip, dst_port);
			}
		}
	}
	
	
//	 create a segment from received bytes 
	void makeSegment(RDTSegment seg, byte[] payload) {
	
		seg.seqNum = Utility.byteToInt(payload, RDTSegment.SEQ_NUM_OFFSET);
		seg.ackNum = Utility.byteToInt(payload, RDTSegment.ACK_NUM_OFFSET);
		seg.flags  = Utility.byteToInt(payload, RDTSegment.FLAGS_OFFSET);
		seg.checksum = Utility.byteToInt(payload, RDTSegment.CHECKSUM_OFFSET);
		seg.rcvWin = Utility.byteToInt(payload, RDTSegment.RCV_WIN_OFFSET);
		seg.length = Utility.byteToInt(payload, RDTSegment.LENGTH_OFFSET);
		//Note: Unlike C/C++, Java does not support explicit use of pointers! 
		// we have to make another copy of the data
		// This is not effecient in protocol implementation
		for (int i=0; i< seg.length; i++)
			seg.data[i] = payload[i + RDTSegment.HDR_SIZE]; 
	}
	
} // end ReceiverThread class

