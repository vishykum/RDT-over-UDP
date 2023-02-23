
/**
* Name: Vishal Venkatakumar
* Login name: vvenkata
*/

/**
 * 
 * @author mohamed
 *
 */
package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

class TimeoutHandler extends TimerTask {
	RDTBuffer sndBuf;
	RDTSegment seg; 
	DatagramSocket socket;
	InetAddress ip;
	int port;
	
	TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, 
			InetAddress ip_addr, int p) {
		sndBuf = sndBuf_;
		seg = s;
		socket = sock;
		ip = ip_addr;
		port = p;
	}
	
	public void run() {
		
		System.out.println(System.currentTimeMillis()+ ":Timeout for seg: " + seg.seqNum);
		System.out.flush();
		
		// complete 
		switch(RDT.protocol){
			case RDT.GBN:
				// System.out.println("Timer expired for seg: " + seg.seqNum);
				for(int i = 0; i < sndBuf.next; i++){
					if (i >= sndBuf.size) break;

					RDTSegment iter = sndBuf.buf[i];

					if (iter.seqNum >= seg.seqNum && !iter.ackReceived) {

						// send using udp_send()

						iter.timeoutHandler = new TimeoutHandler(sndBuf, iter, socket, ip, port);
						RDT.timer.schedule(iter.timeoutHandler, RDT.RTO);

						Utility.udp_send(iter, socket, ip, port);
					}
				}

				break;
			case RDT.SR:
				
				//Retransmit lost segment

				seg.timeoutHandler = new TimeoutHandler(sndBuf, seg, socket, ip, port);
				RDT.timer.schedule(seg.timeoutHandler, RDT.RTO);

				Utility.udp_send(seg, socket, ip, port);

				break;
			default:
				System.out.println("Error in TimeoutHandler:run(): unknown protocol");
		}
		
	}
} // end TimeoutHandler class

