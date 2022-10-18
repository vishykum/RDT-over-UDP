/**
* Name: Vishal Venkatakumar
* Login name: vvenkata
*/


/**
 * @author mohamed
 *
 */

package rdt;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestServer {

	public TestServer() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		 if (args.length != 3) {
	         System.out.println("Required arguments: dst_hostname dst_port local_port");
	         return;
	      }
		 String hostname = args[0];
	     int dst_port = Integer.parseInt(args[1]);
	     int local_port = Integer.parseInt(args[2]);
	     	      
	     RDT rdt = new RDT(hostname, dst_port, local_port, 3, 3);
	     RDT.setLossRate(0.5);
	     byte[] buf = new byte[500];  	     

		 switch(RDT.protocol) {
			case RDT.GBN:
				System.out.println("Using Go Back-N protocol...\n\n");
				break;
			case RDT.SR:
				System.out.println("Using Selective Repeat protocol...\n\n");
				break;
			default:
				System.out.println("Error: Using Unkown protocol...\n\n");
				break;
 		 }		 	


	     System.out.println("Server is waiting to receive ... " );
	
	     
	    while (true) {
	    	int size = rdt.receive(buf, RDT.MSS);

			RDTSegment seg = new RDTSegment();

			seg.seqNum = Utility.byteToInt(buf, RDTSegment.SEQ_NUM_OFFSET);
			seg.ackNum = Utility.byteToInt(buf, RDTSegment.ACK_NUM_OFFSET);
			seg.flags  = Utility.byteToInt(buf, RDTSegment.FLAGS_OFFSET);
			seg.checksum = Utility.byteToInt(buf, RDTSegment.CHECKSUM_OFFSET);
			seg.rcvWin = Utility.byteToInt(buf, RDTSegment.RCV_WIN_OFFSET);
			seg.length = Utility.byteToInt(buf, RDTSegment.LENGTH_OFFSET);

			for (int i=0; i< seg.length; i++)
				seg.data[i] = buf[i + RDTSegment.HDR_SIZE]; 

			// System.out.println("\nPacket received: seq_num: " + seg.seqNum + ", length: " + seg.length);
			if (seg.length > 0) {
				System.out.print("Printing data: ");
				for(int i = 0; i < seg.length; i++) {
					System.out.print(seg.data[i] + " ");
				}
				System.out.println("\n");
			}
			 
	    } 
		
    } 
}

