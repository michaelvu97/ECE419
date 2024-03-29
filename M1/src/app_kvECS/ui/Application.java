package app_kvECS.ui;

import ecs.*;
import app_kvECS.ECSClient;

import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import client.ClientSocketListener;
//import client.TextMessage;

public class Application {

	private static ECSClient client;
	private static Logger logger = Logger.getRootLogger();
	private static final String PROMPT = "ECSClient> ";
	
	private BufferedReader stdin;
	
	private boolean running = true;
	
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

		/*
		* INPUT: quit
		*/
		if(tokens[0].equals("quit")) {	
			running = false;
			disconnect();
			System.out.println(PROMPT + "Application exit!");
		}

		/*
		* INPUT: add <num of nodes> <cache strategy> <cache size>
		*/
		else if(tokens[0].equals("add")) {	
			if(tokens.length == 4) {
				try {
					int numNodes = Integer.parseInt(tokens[1]);
					String cacheStrategy = tokens[2];
					int cacheSize = Integer.parseInt(tokens[3]);
					if(!(cacheStrategy.equals("FIFO") || cacheStrategy.equals("LFU") || cacheStrategy.equals("LRU"))){
						printError("Not a valid cache strategy. Must choose FIFO, LFU, or LRU!");
					}
					else if (numNodes < 1){
						printError("The number of nodes must be greater than 0!");
					}
					else {
						add_servers(numNodes, cacheStrategy, cacheSize);	
					}
				} catch(NumberFormatException nfe) {
					printError("Please make sure that both the cache size and number of nodes are integers");
					logger.info("Unable to parse argument <cacheSize> or <numNodes>", nfe);
				} catch (UnknownHostException e) {
					printError("Unknown Host!");
					logger.info("Unknown Host!", e);
				} catch (IOException e) {
					printError("Could not establish connection!");
					logger.warn("Could not establish connection!", e);
				}
			} else {
				printError("Invalid number of parameters!");
			}
		} 

		/*
		* INPUT: remove <name of servers, separated by spaces>
		*/
		else if(tokens[0].equals("remove")) {	
			if(tokens.length >= 2) {
				try {
					List<String> serversToRemove = new ArrayList<String>();
					for (int i = 0; i < (tokens.length - 1); i++){
						serversToRemove.add(tokens[i+1]);
					}
					remove_servers(serversToRemove);
				} 
				catch(NumberFormatException nfe) {
					printError("Index must be a number!");
					logger.info("Unable to parse argument <serverName>", nfe);
				}
			} else {
				printError("Invalid number of parameters!");
			}
		} 

		/*
		* INPUT: kill <name of server>
		*/
		else if(tokens[0].equals("kill")) {	
			if(tokens.length >= 2) {
				try {
					List<String> serversToKill = new ArrayList<String>();
					for (int i = 0; i < (tokens.length - 1); i++){
						serversToKill.add(tokens[i+1]);
					}
					kill_servers(serversToKill);
				} 
				catch(NumberFormatException nfe) {
					printError("Index must be a number!");
					logger.info("Unable to parse argument <serverName>", nfe);
				}
			}  else {
				printError("Invalid number of parameters!");
			}
		}

		/*
		* INPUT: logLevel <level>
		*/
		else if (tokens[0].equals("logLevel")) {
			if (tokens.length >= 2) {
				setLevel(tokens[1]);
			} else {
				printError("Incorrect Number of Arguments");
			}
		}

		/*
		* INPUT: <unknown command>
		*/
		else {
			printError("Unknown command");
			printHelp();
		}
	}

	private void add_servers(int numServers, String cacheStrategy, int cacheSize) 
		 throws UnknownHostException, IOException {
		  	int numNodes = 0;
		  	List<ECSNode> newNodes = null;

			// call ECSClient method to add new servers.
			newNodes = client.addNodes(numServers, cacheStrategy, cacheSize);
		  	numNodes = newNodes.size();

		  	// log the number of and names for all servers added.
		  	// sorry if this is a really ugly way to do it.
		  	logger.info("Added " + numNodes + " new server(s):");

		  	for (int i = 0; i < numNodes; i++) {
		  		logger.info(newNodes.get(i).getNodeName());
		  	}
	}
	
	private void remove_servers(List<String> servers) {
		int numRmServers = 0;
		List<String> removedServers = null;

		if (servers.get(0) == "all") {

		} else {
			// call ECSClinet method to remove servers.
			removedServers = client.removeNodes(servers);
			numRmServers = removedServers.size();

			// log the number of and names for all servers removed.
		  	// sorry if this is a really ugly way to do it.
		  	logger.info("Removed " + numRmServers + " server(s):");

		  	for (int i = 0; i < numRmServers; i++) {
		  		logger.info(removedServers.get(i));
		  	}
		}		
	}


	private void kill_servers(List<String> servers) {
		int numKillServers = 0;
		List<String> killedServers = null;

		// call ECSClinet method to kill servers.
		killedServers = client.killNodes(servers);
		numKillServers = killedServers.size();

		// log the number of and names for all servers killed.
	  	// sorry if this is a really ugly way to do it.
	  	logger.info("Killed " + numKillServers + " server(s):");

	  	for (int i = 0; i < numKillServers; i++) {
	  		logger.info(killedServers.get(i));
	  	}
	}

	private void disconnect() {
		if(client != null) {
			client.shutdown();
			client = null;
		}
	}
	
	private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("ECS HELP (Usage):\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("add <number of nodes> <cache strategy> <cache size>");
		sb.append("\t adds <number of nodes> new server(s) with the specified cache size and strategy\n");
		sb.append(PROMPT).append("remove <name>");
		sb.append("\t\t\t\t\t remove node of a given name\n");
		sb.append(PROMPT).append("logLevel");
		sb.append("\t\t\t\t\t\t changes the logLevel\n");
		sb.append(PROMPT).append("\t\t\t\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
		sb.append(PROMPT).append("quit ");
		sb.append("\t\t\t\t\t\t exits the program\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::::::::::::::::::::::");
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

	private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}
	
    public static void main(String[] args) {
    	try {
			new LogSetup("logs/ecs-client.log", Level.INFO);
			
			Application app = new Application();

			client = new ECSClient(args[0], args.length > 1 ? args[1] : null); 
			client.start();
			app.run();

		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
    }
}
