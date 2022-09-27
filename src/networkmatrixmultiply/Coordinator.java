package networkmatrixmultiply;

import java.io.*;
import java.util.ArrayList;

public class Coordinator {

	Connection conn;
	int dim;
	int portNo;
	int[][] aMatrix;
	int[][] bMatrix;
	int[][] cMatrix;
	int numNodes;
	DataInputStream[] disWorkers;
	DataOutputStream[] dosWorkers;
	int rowDivision = 1;
	int colDivision = 1;

	ArrayList<WorkerAttributes> workerAttr = new ArrayList<>();

	public Coordinator(int n, int numNodes,int portNo) {
		this.dim = n;
		this.portNo=portNo;
		aMatrix = new int[n][n];
		bMatrix = new int[n][n];
		cMatrix = new int[n][n];
		this.numNodes = numNodes;
	}

	boolean generateWorkers(int numberofWorkers, int dimension) {
		boolean success = true;
		// if odd value
		if (numberofWorkers % 2 == 1) {
			double v=Math.sqrt(numberofWorkers);
			int vs=(int) v;
			double f=v-vs;
			
			if (dimension/Math.sqrt(numberofWorkers)>1 && f==0) {
				rowDivision=(int) Math.sqrt(numberofWorkers);
				colDivision=(int) Math.sqrt(numberofWorkers);
				
				// Creates a skeleton of workers
				int colW = 0, rowW = 0;
				for (int i = 0; i < rowDivision; i++) {
					colW = 0;
					int row = (dimension / rowDivision);
					row = (i == rowDivision - 1) ? dimension - rowW : row;
					for (int j = 0; j < colDivision; j++) {
						int col = (dimension / colDivision);
						col = (j == colDivision - 1) ? dimension - colW : col;

						int a[][] = new int[row][col];
						int b[][] = new int[row][col];
						int c[][] = new int[row][col];
						WorkerAttributes wa = new WorkerAttributes(a, b, c, i, j);
						workerAttr.add(wa);
						colW = colW + col;
					}
					rowW = rowW + (dimension / rowDivision);
				}

				success = true;
			}else if (numberofWorkers <= dimension) {
				rowDivision=numberofWorkers;
				colDivision=1;
				
				// Creates a skeleton of workers
				int colW = 0, rowW = 0;
				for (int i = 0; i < rowDivision; i++) {
					colW = 0;
					int row = (dimension / rowDivision);
					row = (i == rowDivision - 1) ? dimension - rowW : row;
					for (int j = 0; j < colDivision; j++) {
						int col = (dimension / colDivision);
						col = (j == colDivision - 1) ? dimension - colW : col;

						int a[][] = new int[row][col];
						int b[][] = new int[row][col];
						int c[][] = new int[row][col];
						WorkerAttributes wa = new WorkerAttributes(a, b, c, i, j);
						workerAttr.add(wa);
						colW = colW + col;
					}
					rowW = rowW + (dimension / rowDivision);
				}

				success = true;

			} else {
				success = false;
			}
		} else if (numberofWorkers <= dimension*dimension) {
			// if the number of workers is even
			// find two common factors of number of workers...
			// ...to find how the rows and column to be divided for workers
			rowDivision = getIdealDivisible(numberofWorkers);
			colDivision = numberofWorkers / rowDivision;

			// Creates a skeleton of workers
			int colW = 0, rowW = 0;
			for (int i = 0; i < rowDivision; i++) {
				colW = 0;
				int row = (dimension / rowDivision);
				row = (i == rowDivision - 1) ? dimension - rowW : row;
				for (int j = 0; j < colDivision; j++) {
					int col = (dimension / colDivision);
					col = (j == colDivision - 1) ? dimension - colW : col;

					int a[][] = new int[row][col];
					int b[][] = new int[row][col];
					int c[][] = new int[row][col];
					WorkerAttributes wa = new WorkerAttributes(a, b, c, i, j);
					workerAttr.add(wa);
					colW = colW + col;
				}
				rowW = rowW + (dimension / rowDivision);
			}
		} else {
			success = false;
		}

		return success;
	}

	int getIdealDivisible(int value) {
		int divisibleValue = 1;
		for (int i = (value / 2)+1; i >= 2; i--) {
			if (value % i == 0 && i<=dim) {
				divisibleValue = i;
				break;
			}
		}
		return divisibleValue;
	}

	void distributeNCompute() {

		//Initial Circular shift both A and B matrices
		System.out.println("Matrix A");
		aMatrix = Matrix.createDisplayMatrix(dim);
		Matrix.displayMatrix(aMatrix);
		circularLeftShift();
		System.out.println("Shifted Matrix A");
		Matrix.displayMatrix(aMatrix);

		System.out.println("Matrix B");
		bMatrix = Matrix.createDisplayMatrix(dim);
		Matrix.displayMatrix(bMatrix);
		circularUpShift();
		System.out.println("Shifted Matrix B");
		Matrix.displayMatrix(bMatrix);

		// Give space for creating Workers.
		if (generateWorkers(numNodes, dim)) {

			try {

				// Assigning Values of Shifted A and B matrices to their
				// corresponding worker attributes
				int ii = 0, jj = 0;
				int lastI = 0, lastJ = 0;
				for (int i = 0; i < workerAttr.size(); i++) {
					WorkerAttributes wa = workerAttr.get(i);
					int aa[][] = wa.getA();
					int rowCount = aa.length;
					int colCount = aa[0].length;
					int bb[][] = wa.getB();
					for (int r = 0; r < rowCount; r++) {
						ii = (lastI + r) % dim;
						for (int c = 0; c < colCount; c++) {
							jj = (lastJ + c) % dim;

							aa[r][c] = aMatrix[ii][jj];
							bb[r][c] = bMatrix[ii][jj];
						}

					}

					if (jj == dim - 1) {
						lastI = lastI + rowCount;
					}

					lastJ = lastJ + colCount;

					wa.setA(aa);
					wa.setB(bb);
				}

				// Creating and distributing workers
				// Creating Workers
				conn = new Connection(portNo);
				// Connecting with workers and retrieving their details
				// Sending data to them
				for (int k = 0; k < workerAttr.size(); k++) {
					DataIO dio = conn.acceptConnect();
					DataInputStream dis = dio.getDis();
					DataOutputStream dos = dio.getDos();
					WorkerAttributes wa = workerAttr.get(k);
					wa.setIpAddress(dis.readUTF()); // 1)get worker ip
					wa.setPortNo(dis.readInt()); // 2)get worker port #
					wa.setInputStream(dis);
					wa.setOutputStream(dos); // the stream to worker

					// Communicate with worker
					int aa[][] = wa.getA();
					int bb[][] = wa.getB();
					dos.writeInt(dim);// 3)Dimension of the n X n matrices
					dos.writeInt(aa.length);// 4)Sends the row count
					dos.writeInt(aa[0].length);// 5)Sends the column count
					for (int i = 0; i < aa.length; i++) {
						for (int j = 0; j < aa[0].length; j++) {
							dos.writeInt(aa[i][j]);// //6)Sends a value of
													// Matrix A of the block
							dos.writeInt(bb[i][j]);// //7)Sends a value of
													// Matrix B of the block
						}
					}
				}

				// Defining Left and Up relationship of each
				// workers.
				for (int i = 0; i < workerAttr.size(); i++) {
					WorkerAttributes wa = workerAttr.get(i);
					int rowIndex = wa.getRowIndex();
					int colIndex = wa.getColIndex();
					int leftR = rowIndex;
					int leftC = (colIndex - 1 == -1) ? colDivision - 1
							: colIndex - 1;
					int upR = (rowIndex - 1 == -1) ? rowDivision - 1
							: rowIndex - 1;
					int upC = colIndex;
					
					for (int j = 0; j < workerAttr.size(); j++) {
						WorkerAttributes wor = workerAttr.get(j);
						int rowI = wor.getRowIndex();
						int colI = wor.getColIndex();
						// Left Worker
						if (rowI == leftR && colI == leftC) {
							wa.setLeftWorker(wor);
						}
						// Up Worker
						if (rowI == upR && colI == upC) {
							wa.setUpWorker(wor);
						}
					}
				}

				// Computes
				ArrayList<int[]> tempList = new ArrayList<>();
				for (int i = 1; i < dim; i++) {
					// Reading all worker Block A
					tempList.clear();

					for (int k = 0; k < workerAttr.size(); k++) {
						WorkerAttributes wa = workerAttr.get(k);
						DataInputStream dio = wa.getInputStream();
						int rowCount = wa.getA().length;
						int colValues[] = new int[rowCount];
						// Reading first column of Block A
						// System.out.println("Worker "+k);
						for (int j = 0; j < rowCount; j++) {
							colValues[j] = dio.readInt();
							// System.out.println("Receiving A "+colValues[j]);
						}
						tempList.add(colValues);
					}

					// Writing all worker Block A to its Left Worker
					for (int k = 0; k < workerAttr.size(); k++) {
						WorkerAttributes wa = workerAttr.get(k);
						DataOutputStream dos = wa.getLeftWorker()
								.getOutputStream();
						int rowCount = wa.getA().length;
						int colValues[] = tempList.get(k);
						// Sending first column of Block A
						// System.out.println("Worker "+k);
						for (int j = 0; j < rowCount; j++) {
							dos.writeInt(colValues[j]);
							// System.out.println("Send A "+colValues[j]);
						}
					}

					// Reading all worker Block B
					tempList.clear();
					for (int k = 0; k < workerAttr.size(); k++) {
						WorkerAttributes wa = workerAttr.get(k);
						DataInputStream dio = wa.getInputStream();
						int colCount = wa.getA()[0].length;
						int rowValues[] = new int[colCount];
						// Reading first row of Block B
						// System.out.println("Worker "+k);
						for (int j = 0; j < colCount; j++) {
							rowValues[j] = dio.readInt();
							// System.out.println("Receiving B "+rowValues[j]);
						}
						tempList.add(rowValues);
					}

					// Writing all worker Block B to its Up Worker
					for (int k = 0; k < workerAttr.size(); k++) {
						WorkerAttributes wa = workerAttr.get(k);
						DataOutputStream dos = wa.getUpWorker()
								.getOutputStream();
						int colCount = wa.getA()[0].length;
						int rowValues[] = tempList.get(k);
						// Sending first row of Block B
						// System.out.println("Worker "+k);
						for (int j = 0; j < colCount; j++) {
							dos.writeInt(rowValues[j]);
							// System.out.println("Sending B "+rowValues[j]);
						}
					}
				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		}

	}

	void circularLeftShift() {
		int shifted[][] = new int[dim][dim];
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				int k = j - 1 - i;
				k = k < 0 ? dim + k : k;
				shifted[i][k] = aMatrix[i][j];
			}
		}
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				aMatrix[i][j] = shifted[i][j];
			}
		}
	}

	void circularUpShift() {
		int shifted[][] = new int[dim][dim];
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				int k = j - 1 - i;
				k = k < 0 ? dim + k : k;
				shifted[k][i] = bMatrix[j][i];
			}
		}
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				bMatrix[i][j] = shifted[i][j];
			}
		}
	}

	void getProductMatrix() throws IOException {
		// Receive the result matrix from each workers

		int ii = 0, jj = 0;
		int lastI = 0, lastJ = 0;
		for (int k = 0; k < workerAttr.size(); k++) {
			WorkerAttributes wa = workerAttr.get(k);
			int rowCount = wa.getA().length;
			int colCount = wa.getA()[0].length;
			DataInputStream dis = wa.getInputStream();
			for (int r = 0; r < rowCount; r++) {
				ii = (lastI + r) % dim;
				for (int c = 0; c < colCount; c++) {
					jj = (lastJ + c) % dim;
					cMatrix[ii][jj] = dis.readInt();
				}
				System.out.println();
			}

			if (jj == dim - 1) {
				lastI = lastI + rowCount;
			}

			lastJ = lastJ + colCount;
		}
		Boolean success = Matrix.compareMatrix(
				cMatrix,
				Matrix.multiplyMatrix(Matrix.createDisplayMatrix(dim),
						Matrix.createDisplayMatrix(dim)));
		System.out.println("Product Matrix");
		Matrix.displayMatrix(cMatrix);
	}

	public static void main(String[] args) {

		if (args.length != 3) {
			System.out
					.println("usage: java Coordinator maxtrix-dim number-nodes coordinator-port-num");
		}
		
		int mDimension=Integer.parseInt(args[0]);
		int mNoOfNodes=Integer.parseInt(args[1]);
		int mPortNo=Integer.parseInt(args[2]);
		
		Coordinator coor = new Coordinator(mDimension, mNoOfNodes,mPortNo);
		coor.distributeNCompute();
		try {
			coor.getProductMatrix();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Coordinator Done.");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}
}
