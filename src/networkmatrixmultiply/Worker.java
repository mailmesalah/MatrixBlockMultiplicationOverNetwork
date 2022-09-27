package networkmatrixmultiply;

import java.io.*;
import java.net.InetAddress;

import matrix.*;

public class Worker {

	int localPort;
	Connection conn;
	int dimension;
	int height;
	int width;
	int[][] a;
	int[][] b;
	int[][] c;
	DataInputStream disCoor;
	DataOutputStream dosCoor;

	volatile boolean isASend = false, isAReceive = false, isBSend = false,
			isBReceive = false;
	
	public Worker(int localPort) {
		this.localPort = localPort;
	}

	void initialize(String coorIP, int coorPort) {
		try {
			conn = new Connection(localPort);
			DataIO dio = conn.connectIO(coorIP, coorPort);
			dosCoor = dio.getDos();
			dosCoor.writeUTF(InetAddress.getLocalHost().getHostAddress());// 1)
																			// Send
																			// IP
																			// Address
			dosCoor.writeInt(localPort);// 2) Send Port No
			disCoor = dio.getDis();
			dimension = disCoor.readInt();// 3) Receive row count of the worker
			height = disCoor.readInt();// 4) Receive row count of the worker
			width = disCoor.readInt();// 5) Receive column count of the worker

			// Initialize Blocks
			a = new int[height][width];
			b = new int[height][width];
			c = new int[height][width];

			// Reading the worker content of both A and B blocks
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					a[i][j] = disCoor.readInt();// 6)Receives a value of Matrix
												// A
					b[i][j] = disCoor.readInt();// 7)Receives a value of Matrix
												// B
					c[i][j] = a[i][j] * b[i][j];// Calculating the first Value
												// of C
				}
			}

			System.out.println("Block A");
			Matrix.displayMatrix(a);
			System.out.println("Block B");
			Matrix.displayMatrix(b);

			// Initially we had one shift so only dimension - 1 number of shift
			// left.
			for (int i = 1; i < dimension; i++) {		
							
				// Shift Left
				int outLValue[] = new int[height];
				if(width==1){
					for (int k2 = 0; k2 < width; k2++) {
						outLValue[k2] = a[k2][0];
					}
				}
				for (int k1 = 0; k1 < height; k1++) {
					for (int k2 = 0; k2 < width - 1; k2++) {
						if (k2 == 0) {// First column values of matrix a is taken in
										// outValue
							outLValue[k1] = a[k1][0];
							a[k1][k2] = a[k1][k2 + 1];
						} else {
							a[k1][k2] = a[k1][k2 + 1];
						}
					}
				}
				
				  // Shift Up 
				int outUValue[] = new int[width];
				if(height==1){
					for (int k2 = 0; k2 < width; k2++) {
						outUValue[k2] = b[0][k2];
					}
				}
				for (int k1 = 0; k1< height - 1; k1++) { 
					for (int k2 = 0; k2 < width; k2++) {				
						if (k1== 0) {// First column values of matrix a is taken in // outValue
							outUValue[k2] = b[0][k2]; b[k1][k2] = b[k1 + 1][k2]; 
						} else {
							b[k1][k2] = b[k1 + 1][k2]; 
						} 
					} 
				}
				 
				// Send A Block's first column to Left Worker
				//System.out.println("One Block");
				for (int j = 0; j < height; j++) {
					dosCoor.writeInt(outLValue[j]);
					//dosCoor.writeInt(j);
					//System.out.println(" Sending A "+ outLValue[j]);
				}
				
				// Receive column data for A Block from Right Worker
				//System.out.println("One Block");
				for (int j = 0; j < height; j++) {
					a[j][width - 1] = disCoor.readInt();
					//System.out.println(" Reading A "+ a[j][width - 1]);
					//System.out.println(" Reading A "+ disCoor.readInt()+" check");
				}
				
				// Send B Block's first row to Up Worker
				//System.out.println("One Block");
				for (int j = 0; j < width; j++) {
					dosCoor.writeInt(outUValue[j]);
					//dosCoor.writeInt(j);
					//System.out.println(" Sending B "+ outUValue[j]+" j"+j);
				}
				
				// Receive column data for B Block from Down Worker
				//System.out.println("One Block");
				for (int j = 0; j < width; j++) {				
					b[height - 1][j] = disCoor.readInt();			
					//System.out.println(" Reading B "+ b[height - 1][j]);
				}
				
				// Compute C
				for (int k1 = 0; k1 < height; k1++) {
					for (int k2 = 0; k2 < width; k2++) {
						c[k1][k2] = c[k1][k2] + (a[k1][k2] * b[k1][k2]);
					}
				}
				
				System.out.println("Shifted A Block");
				Matrix.displayMatrix(a);
				System.out.println();
				
				System.out.println("Shifted B Block");
				Matrix.displayMatrix(b);
				System.out.println();			
			}

			sendResultToCoordinator();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}		
	}
			
	public void sendResultToCoordinator() throws IOException {
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				dosCoor.writeInt(c[i][j]);
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out
					.println("usage: java Worker worker-port-num coordinator-ip coordinator-port-num");
		}
		int portNumber = Integer.parseInt(args[0]);
		String coorIP = args[1];
		int coorPortN = Integer.parseInt(args[2]);
		Worker worker = new Worker(portNumber);
		worker.initialize(coorIP, coorPortN);
			
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		
		System.out.println("Worker Done.");
	}
}
