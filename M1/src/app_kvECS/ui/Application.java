package app_kvECS.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import app_kvClient.KVClient;
import client.ClientSocketListener;
//import client.TextMessage;

public class Application implements ClientSocketListener {

	private static Logger logger = Logger.getRootLogger();
	private static final String PROMPT = "KVClient> ";
	private BufferedReader stdin;
	private KVClient client = null;
	private boolean running = true;
	
	private String serverAddress;
	private int serverPort;
	
	public void run() {
		while(running) {
			stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(PROMPT);
			
			try {
				String cmdLine = stdin.readLine();
				this.handleCommand(cmdLine);
			} catch (IOException e) {
				running = false;
				printError("CLI does not respond - Application terminated ");
			}
		}
	}
	
	private void handleCommand(String cmdLine) {
		String[] tokens = cmdLine.split("\\s+");

		// quit
		if(tokens[0].equals("quit")) {	
			running = false;
			disconnect();
			System.out.println(PROMPT + "Application exit!");
		} 
		// unknown command
		else {
			printError("Unknown command");
			printHelp();
		}
	}

	private void connect(String address, int port) 
			throws UnknownHostException, IOException {
		client = new KVClient();
		try {
			client.newConnection(address, port);
			client.addListener(this);
		} catch (IOException e) {
			printError("Could not establish connection!");
			logger.warn("Could not establish connection!", e);
		}
	}
	
	private void disconnect() {
		if(client != null) {
			client.disconnect();
			client = null;
		}
	}
	
	private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("CLIENT HELP (Usage):\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("connect <host> <port>");
		sb.append("\t\t establishes a connection to a server\n");
		sb.append(PROMPT).append("get <key>");
		sb.append("\t\t\t retrieves a tuple from the server based on <key>\n");
		sb.append(PROMPT).append("put <key> <value>");
		sb.append("\t\t inserts a new KV tuple into the server. If <value> == \"null\", the tuple is deleted from the server.\n");
		sb.append(PROMPT).append("disconnect");
		sb.append("\t\t\t disconnects from the server \n");
		sb.append(PROMPT).append("logLevel");
		sb.append("\t\t\t changes the logLevel \n");
		sb.append(PROMPT).append("\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
		sb.append(PROMPT).append("quit ");
		sb.append("\t\t\t\t exits the program");
		System.out.println(sb.toString());
	}
	
	private void printPossibleLogLevels() {
		System.out.println(PROMPT 
				+ "Possible log levels are:");
		System.out.println(PROMPT 
				+ "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
	}

	private String setLevel(String levelString) {
		
		if(levelString.equals(Level.ALL.toString())) {
			logger.setLevel(Level.ALL);
			return Level.ALL.toString();
		} else if(levelString.equals(Level.DEBUG.toString())) {
			logger.setLevel(Level.DEBUG);
			return Level.DEBUG.toString();
		} else if(levelString.equals(Level.INFO.toString())) {
			logger.setLevel(Level.INFO);
			return Level.INFO.toString();
		} else if(levelString.equals(Level.WARN.toString())) {
			logger.setLevel(Level.WARN);
			return Level.WARN.toString();
		} else if(levelString.equals(Level.ERROR.toString())) {
			logger.setLevel(Level.ERROR);
			return Level.ERROR.toString();
		} else if(levelString.equals(Level.FATAL.toString())) {
			logger.setLevel(Level.FATAL);
			return Level.FATAL.toString();
		} else if(levelString.equals(Level.OFF.toString())) {
			logger.setLevel(Level.OFF);
			return Level.OFF.toString();
		} else {
			return LogSetup.UNKNOWN_LEVEL;
		}
	}
	
	@Override
	public void handleStatus(SocketStatus status) {
		if(status == SocketStatus.CONNECTED) {

		} else if (status == SocketStatus.DISCONNECTED) {
			System.out.print(PROMPT);
			System.out.println("Connection terminated: " 
					+ serverAddress + " / " + serverPort);
			
		} else if (status == SocketStatus.CONNECTION_LOST) {
			System.out.println("Connection lost: " 
					+ serverAddress + " / " + serverPort);
			System.out.print(PROMPT);
		}
		
	}

	private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}
	
    /**
     * Main entry point for the echo server application. 
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
    	try {
    		// TODO log level?
			new LogSetup("logs/client.log", Level.ALL);
			Application app = new Application();
			app.run();
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
    }

}
