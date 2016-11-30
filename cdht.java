//REINALDO CAPIZZI
//z5046440
//COMP9331
import java.io.*;
import java.net.*;
import java.util.*;
public class cdht {

	static int peerNum, peerAddress, success1, success1port, success2, success2port, predecessor, predecessor2;
	static boolean rcv = true;
	static int quitCount = 0;
	static int success1ping = 0;
	static int success1response = 0;
	static int success2ping = 0;
	static int success2response = 0;

	ServerSocket fileReqSocket;
	
	void setPorts(){
		peerAddress = 50000 + peerNum;
		success1port = 50000 + success1;
		success2port = 50000 + success2;
	}
	//This initialises the UDP Server, to receive incoming UDP packets
	public static class UDPReceiveThread implements Runnable {
		private Thread thread;
		private String threadName;

		UDPReceiveThread (String threadName) {
			this.threadName = threadName;
		}

		public void run() {
			//rcv is set to false if the quit command has been entered to ensure the server quits
			//I have used certain letters are codes to determine the type of request received, noted in my report (e.g. "P" means ping request)
			if (rcv){
			try {
				byte[] rcvmsg = new byte[1024];
				DatagramPacket responsepkt = new DatagramPacket(rcvmsg, rcvmsg.length);
				DatagramSocket pingsocket = new DatagramSocket(peerAddress);
				while(true){
					pingsocket.receive(responsepkt);
					String rcvmsg2 = new String(responsepkt.getData());
					
					if (String.valueOf(rcvmsg2.charAt(0)).equals("R")){
						String submsg2 = rcvmsg2.replaceAll("\\D+","");
						int peernumber = Integer.parseInt(submsg2);
						System.out.println("A ping response was received from Peer " + peernumber);

					}
					if (String.valueOf(rcvmsg2.charAt(0)).equals("P")){
						String submsg2 = rcvmsg2.replaceAll("\\D+","");
						int peernumber = Integer.parseInt(submsg2);
						System.out.println("A ping request was received from Peer " + peernumber + ". Response sent");
						String response = "R" + peerNum + "E";
		            	InetAddress senderIP = responsepkt.getAddress();
		            	int senderport = peernumber + 50000;
		            	byte[] responsedata = response.getBytes();
		            	DatagramPacket sndpkt = new DatagramPacket(responsedata, responsedata.length, senderIP, senderport);
		            	pingsocket.send(sndpkt);
					}
//					byte[] buffer = ("N" + 255 + "EEE" + peerNum + "EEE").getBytes();
					if (String.valueOf(rcvmsg2.charAt(0)).equals("N")){
						String submsg2 = rcvmsg2.replaceAll("\\D+","");
						int pred = Integer.parseInt(submsg2);
						predecessor = pred;
					}
					if (String.valueOf(rcvmsg2.charAt(0)).equals("M")){
						String submsg2 = rcvmsg2.replaceAll("\\D+","");
						int pred2 = Integer.parseInt(submsg2);
						predecessor2 = pred2;
						}
				}
			} catch (Exception e) {
			}    
		}
		}

		public void start () {
			if (thread == null) {
				thread = new Thread (this, threadName);
				thread.start ();
			}
		}
	}
	//This task is the ping requester, set to request every 10 seconds in the pingstart method below it
	Timer myTimer = new Timer();
	TimerTask task = new TimerTask() {
		public void run(){
			if (rcv){
			try{
				byte[] buffer = ("P" + peerNum + "EEE").getBytes();
				InetAddress hostaddress = InetAddress.getByName("localhost");
				DatagramPacket pingsuccessor1 = new DatagramPacket(
						buffer, buffer.length, hostaddress, success1port);
				DatagramPacket pingsuccessor2 = new DatagramPacket(
						buffer, buffer.length, hostaddress, success2port);
				
				DatagramSocket pingsocket = new DatagramSocket();
				System.out.println();
				System.out.println("Sending ping request to successors: Peer "+success1+" and Peer "+success2+"...");
				pingsocket.send(pingsuccessor1);
				pingsocket.send(pingsuccessor2);
				pingsocket.close();

			}catch (Exception e){
				System.out.println("Error sending");
			}


		}
		}
	};
	public void pingstart(){
		myTimer.scheduleAtFixedRate(task, 0, 10000);
	}
	//This method is run once on initialising, to set every peer's predecessors correctly.
	void setpredecessors(){
		try{
		byte[] buffer1 = ("N" + success1 + "EEE").getBytes();
		byte[] buffer2 = ("M" + peerNum + "EEE").getBytes();
		InetAddress hostaddress = InetAddress.getByName("localhost");
		DatagramPacket setPredSuccess1 = new DatagramPacket(
				buffer1, buffer1.length, hostaddress, success2port);
		DatagramPacket setPredSuccess2 = new DatagramPacket(
				buffer2, buffer2.length, hostaddress, success2port);
		DatagramSocket predSend = new DatagramSocket(peerAddress + 100);
		predSend.send(setPredSuccess1);
		predSend.send(setPredSuccess2);
		predSend.close();	
		
		
		
		
	}catch (Exception e){
		System.out.println("set predecessor failed");
	}
	}

	//This is the TCP server thread, to receive incoming TCP requests
public static class TCPReceiveThread implements Runnable {
    private Thread t;
    private String threadName;
   
    TCPReceiveThread (String threadName) { 
        this.threadName = threadName;
    }

    public void run() {
		try{
		ServerSocket server = new ServerSocket(peerAddress);
		
		while(true){
			//Again I have used letter codes to determine the type of request
			Socket connection = server.accept();
		
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String rcvsentence = new String(in.readLine());
			String[] requestsplit = rcvsentence.split(" ");
			String request = String.valueOf(rcvsentence.charAt(0));	

			//Quit departure response. Only the 'quitting' peer will receive a W. This indicates that a successor has received the departure notification.
			//When two Ws are received, the peer will exit.
			if (request.equals("W")){
				if (quitCount == 0){
					System.out.println("Received first quit response");
				}
				if (quitCount == 1){
					System.out.println("Received second quit response");
				}
				quitCount += 1;
				
				if (quitCount == 2){
					System.out.println("Quit response messages received. Peer exiting in 3 seconds.");
					rcv = false;
					Thread.sleep(3000);
					System.exit(0);
				}

			}
			//File request
			if (request.equals("F")){
				int originPeerNum = Integer.parseInt(requestsplit[1]);
				int filename = Integer.parseInt(requestsplit[2]);
				String filenamestring = String.valueOf(filename);
				String formattedname = ("0000" + filenamestring).substring(filenamestring.length());
				System.out.println
				("File request for file " + formattedname + " received from Peer " + requestsplit[1]);
				int filehash = Integer.parseInt(requestsplit[3]);
                boolean havefile = false;
        		
        		if (filehash == peerNum){
        			havefile = true;
        		}
        		if (filehash >= peerNum && filehash < success1){
        			havefile = true;
        		}
        		//if current peer is the last peer
        		if (peerNum > success1){
        			if (filehash >= peerNum && filehash <= 256){
        				havefile = true;
        			}
        			if (filehash >= 0 && filehash < success1){
        				havefile = true;
        			}
        		}
        		
        		
				if (havefile){
					System.out.println("File " + formattedname + " is here.");
					Socket filesocket = new Socket("localhost", 50000 + originPeerNum);
					OutputStream filereqstream = filesocket.getOutputStream();
                	PrintWriter fileresponseout = new PrintWriter(filereqstream, true);
                	String filerequest = new String("A" + " " + peerNum + " " + requestsplit[2]);
                	fileresponseout.println(filerequest);
                	filesocket.close();
					System.out.println("A response message, destined for Peer " + originPeerNum + " has been sent.");
				}else {
						System.out.println("File " + formattedname + " is not stored here.");
						System.out.println("File request message has been forwarded to my successor");
						Socket filesocket = new Socket("localhost", success1port);
						OutputStream filereqstream = filesocket.getOutputStream();
		            	PrintWriter fileresponseout = new PrintWriter(filereqstream, true);
		            	fileresponseout.println(rcvsentence);
		            	filesocket.close();
					}

			//Notification of peer who has file
			}if (request.equals("A")){
				int filename = Integer.parseInt(requestsplit[2]);
				String filenamestring = String.format("%04d", filename);
				System.out.println
				("Received a response message from Peer " + requestsplit[1] + " which has the file " + filenamestring);
			}

			//Departure notification
			if (request.equals("Q")){
				int departingPeer = Integer.parseInt(requestsplit[1]);
				System.out.println("Peer " + departingPeer + " will depart the network.");
				
				Socket filesocket = new Socket("localhost", 50000 + departingPeer);
				OutputStream quitResponseStream = filesocket.getOutputStream();
            	PrintWriter quitResponseOut = new PrintWriter(quitResponseStream, true);
            	String response = new String("W");
            	quitResponseOut.println(response);
            	filesocket.close();
				
				//The following determine the type of quit command they received, so they know how to read the message.
				//If Q1, they were the departing peer's first predecessor.
				//If Q2, they were the departing peer's second predecessor.
				//They then change their successors, and set their new successors' predecessors.
				if (requestsplit[0].equals("Q1")){
					success1 = Integer.parseInt(requestsplit[2]);
					success2 = Integer.parseInt(requestsplit[3]);
					success1port = 50000 + success1;
					success2port = 50000 + success2;
					System.out.println("My first successor is now Peer " + success1 + ".");
					System.out.println("My second successor is now Peer " + success2 + ".");
					
					try{
						byte[] buffer1 = ("N" + success1 + "EEE").getBytes();
						byte[] buffer2 = ("M" + peerNum + "EEE").getBytes();
						InetAddress hostaddress = InetAddress.getByName("localhost");
						DatagramPacket setPredSuccess1 = new DatagramPacket(
								buffer1, buffer1.length, hostaddress, success2port);
						DatagramPacket setPredSuccess2 = new DatagramPacket(
								buffer2, buffer2.length, hostaddress, success2port);
						DatagramSocket predSend = new DatagramSocket(peerAddress + 100);
						predSend.send(setPredSuccess1);
						predSend.send(setPredSuccess2);
						predSend.close();	
						
					}catch (Exception e){
						System.out.println("set predecessor failed");
					}
					
				}if (requestsplit[0].equals("Q2")){
					success2 = Integer.parseInt(requestsplit[2]);
					success2port = 50000 + success2;
					System.out.println("My first successor is now Peer " + success1 + ".");
					System.out.println("My second successor is now Peer " + success2 + ".");
					try{
						byte[] buffer1 = ("N" + success1 + "EEE").getBytes();
						byte[] buffer2 = ("M" + peerNum + "EEE").getBytes();
						InetAddress hostaddress = InetAddress.getByName("localhost");
						DatagramPacket setPredSuccess1 = new DatagramPacket(
								buffer1, buffer1.length, hostaddress, success2port);
						DatagramPacket setPredSuccess2 = new DatagramPacket(
								buffer2, buffer2.length, hostaddress, success2port);
						DatagramSocket predSend = new DatagramSocket(peerAddress + 100);
						predSend.send(setPredSuccess1);
						predSend.send(setPredSuccess2);
						predSend.close();	
						

					}catch (Exception e){
						System.out.println("set predecessor failed");
					}
				
			}
		}

		}

	}catch (Exception e){
		e.printStackTrace();
	}	
    	
    	
    	
    	
    }

    public void start () {
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
   }
}

	//This thread creates a TCP socket and monitors the user input
	public static class CommandThread implements Runnable {
        private Thread thread;
        private String threadName;
       
        CommandThread (String threadName) { 
            this.threadName = threadName;
        }

        public void run() {
            try {
                while (true) {
                	//Ensuring the input commands are valid
                    BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                    if (userInput.ready()) {
                        String command = new String(userInput.readLine());
                        String[] wordlist = command.split(" ");
                        if ((!wordlist[0].equals("request") && !wordlist[0].equals("quit"))){
                        	System.out.println("Invalid command");
                        	continue;
                        }


                        //Monitoring for an input request, and sending the file request if the current peer doesn't have it
                        if (wordlist[0].equals("request")) {
                        	if (wordlist[1].length() != 4){
                        		System.out.println("Incorrect filename input");
                        		continue;
                        	}
                        	if (!wordlist[1].matches("[0-9]+")){
                        		System.out.println("Incorrect filename input");
                        		continue;
                        	}
                        	String file = wordlist[1];
                            int filename = Integer.parseInt(file);
                            String filenamestring = String.format("%04d", filename);
                            int filehash = (filename + 1) % 256;
                            boolean havefile = false;
                    		//for all peers except the last peer
                    		if (filehash == peerNum){
                    			havefile = true;
                    		}
                    		if (filehash >= peerNum && filehash < success1){
                    			havefile = true;
                    		}
                    		//if current peer is the last peer
                    		if (peerNum > success1){
                    			if (filehash >= peerNum && filehash <= 256){
                    				havefile = true;
                    			}
                    			if (filehash >= 0 && filehash < success1){
                    				havefile = true;
                    			}
                    		}
                    		
                            if (havefile){
            					System.out.println("File " + filenamestring +" is here.");
                            }
                            else{
                            System.out.println("File request for file " + filenamestring + " has been sent to my successor.");

                            Socket requestSocket = new Socket("localhost", success1port);
                            OutputStream srvrout = requestSocket.getOutputStream();
                        	PrintWriter sendout = new PrintWriter(srvrout, true);
                        	String filerequest = new String("F" + " " + peerNum + " " + filename + " " + filehash);
                        	sendout.println(filerequest);
                        	requestSocket.close();
                            
                            }
            
                        }
                        //Takes a quit command, which then sends the departure notification to successors.
                        if ((wordlist[0]).equals("quit")){
                        	Socket pred1socket = new Socket("localhost", 50000 + predecessor);
                        	Socket pred2socket = new Socket("localhost", 50000 + predecessor2);
                        	OutputStream pred1stream = pred1socket.getOutputStream();
        					OutputStream pred2stream = pred2socket.getOutputStream();
                        	PrintWriter pred1out = new PrintWriter(pred1stream, true);
                        	PrintWriter pred2out = new PrintWriter(pred2stream, true);
                        	
                        	String pred1notice = "Q1" + " " + peerNum + " " + success1 + " " + success2; 
                        	String pred2notice = "Q2" + " " + peerNum + " " + success1;
                        	pred1out.println(pred1notice);
                        	pred1socket.close();
                        	pred2out.println(pred2notice);
                        	pred2socket.close();
                        	rcv=false;

//                        }
//                        if ((wordlist[0]).equals("pred")){
//                        	System.out.println("Pred 1: " + predecessor);
//                        	System.out.println("Pred 2: " + predecessor2);
//                        }
                           
                    }
                                    

                    } 
                }
            } catch (Exception e) {

            }
        }

        public void start () {
            if (thread == null) {
                thread = new Thread (this, threadName);
                thread.start ();
            }
       }
    }

    //This method is to delay startup, so that every peer has a chance to initialise the UDP/TCP server sockets before sending out pings etc.
	void delay(){
		try{
		Thread.sleep(3000);
	}catch (Exception e){
		System.out.println();
	}
	}

	public static void main(String[] args) throws IOException{

		if (args.length != 3){
			System.out.println("Incorrect argument input");
			return;
		}
		cdht peer = new cdht();
		peer.peerNum = Integer.valueOf(args[0]);
		peer.success1 = Integer.valueOf(args[1]);