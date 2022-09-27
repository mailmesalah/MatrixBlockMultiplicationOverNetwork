package networkmatrixmultiply;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class WorkerAttributes {
	int a[][];
	int b[][];
	int c[][];
	int rowIndex;
	int colIndex;
	
	String ipAddress;
	int portNo;
	DataInputStream inputStream;
	DataOutputStream outputStream;
	//Right,Down,Left and Up Workers Attributes
	WorkerAttributes leftWorker;
	WorkerAttributes upWorker;	
	
	public WorkerAttributes(int[][] a, int[][] b, int[][] c, int rowIndex,
			int colIndex) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
		this.rowIndex = rowIndex;
		this.colIndex = colIndex;
	}
		
	public WorkerAttributes getLeftWorker() {
		return leftWorker;
	}

	public void setLeftWorker(WorkerAttributes leftWorker) {
		this.leftWorker = leftWorker;
	}

	public WorkerAttributes getUpWorker() {
		return upWorker;
	}

	public void setUpWorker(WorkerAttributes upWorker) {
		this.upWorker = upWorker;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPortNo() {
		return portNo;
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public DataInputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(DataInputStream inputStream) {
		this.inputStream = inputStream;
	}

	public DataOutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(DataOutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public int[][] getA() {
		return a;
	}
	public void setA(int[][] a) {
		this.a = a;
	}
	public int[][] getB() {
		return b;
	}
	public void setB(int[][] b) {
		this.b = b;
	}
	public int[][] getC() {
		return c;
	}
	public void setC(int[][] c) {
		this.c = c;
	}
	public int getRowIndex() {
		return rowIndex;
	}
	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}
	public int getColIndex() {
		return colIndex;
	}
	public void setColIndex(int colIndex) {
		this.colIndex = colIndex;
	}
	
}
