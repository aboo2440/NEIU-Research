package edu.research.neiu;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.jaunt.NodeNotFound;
import com.jaunt.UserAgent;

public class Automation {

	// Attributes
	private UserAgent user;
	private LinkedHashMap<String, String[]> sourceServers;
	private LinkedHashMap<String, String[]> destServers;
	private boolean verbose;
	private File outputFile;
	private Calendar cal = Calendar.getInstance();
	private String destName, sourceName;
	private String[] response;
	
	// Constructor
	public Automation() {
		user = new UserAgent();
		this.loadFiles();
		this.createOutputFile();
	}
	
	// Main Method - Run Method
	public void run() {
		// Description and warranty.
		System.out.println("Traceroute Data Collection\n"
				+ "Copyright (C) 2014  Michael Abreu, Karl Amjad-Ali\n"
				+ "This program comes with ABSOLUTELY NO WARRANTY.\n"
				+ "This is free software, and you are welcome to redistribute it\n"
				+ "under certain conditions;\n");
		
		//displayMapData(destServers, "destination");
		
		// Check verbose
		Scanner in = new Scanner(System.in);
		System.out.print("Do you want verbose mode,(Y/N): ");
		String checkVerbose = in.nextLine();
		if (checkVerbose.equalsIgnoreCase("y"))
			verbose = true;
		else 
			verbose = false;
		in.close();
		
		this.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");		
		this.write("<!-- Data Collected: " + (cal.get(Calendar.MONTH)+1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "-"+ cal.get(Calendar.YEAR)
				+ "\t Time: " +cal.get(Calendar.HOUR) + ":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+" CST (GMT-5)" +" -->\n");
		this.write("<datasample xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xmlschema.xsd\">\n");
		
		Set<String> sourceKeys = sourceServers.keySet();
		Iterator<String> sourceIterator = sourceKeys.iterator();
		
		/**
		 * Main Loop
		 */
		while (sourceIterator.hasNext())
		// Condition: Iterates through all source servers.
		//
		// Pre: Source Servers are read from "source.txt"
		{
			Set<String> destKeys = destServers.keySet();
			Iterator<String> destIterator = destKeys.iterator();

			sourceName = sourceIterator.next();
			this.write("\t<traceroutes name=\""+sourceName +"\">\n");
			
			/**
			 * Inner Loop
			 */
			while (destIterator.hasNext()) 
			// Condition: Iterates through all destination servers.
			//
			// Pre: Destination Servers are read from "destination.txt"
			{
				destName = destIterator.next();
				this.write("\t\t<traceroute name=\"" + destName + "\">\n");
				// Source Tags
				this.write("\t\t\t<server type=\"source\">\n");
				this.write("\t\t\t\t<name>"+sourceName+"</name>\n");
				this.write("\t\t\t\t<location>"+sourceServers.get(sourceName)[4]+"</location>\n");
				this.write("\t\t\t\t<AS>"+sourceServers.get(sourceName)[3]+"</AS>\n");
				this.write("\t\t\t</server>\n");
				// Destination Tags
				this.write("\t\t\t<server type=\"destination\">\n");
				this.write("\t\t\t\t<name>"+destName+"</name>\n");
				this.write("\t\t\t\t<location>"+destServers.get(destName)[1]+"</location>\n");
				this.write("\t\t\t\t<AS>"+destServers.get(destName)[0]+"</AS>\n");
				this.write("\t\t\t\t<ip>"+destServers.get(destName)[2]+"</ip>\n");
				this.write("\t\t\t</server>\n");
				// Time Tags
				cal = Calendar.getInstance();
				this.write("\t\t\t<time>\n");
				this.write("\t\t\t\t<date>"+((cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DAY_OF_MONTH)+"-"+cal.get(Calendar.YEAR))+"</date>\n");
				this.write("\t\t\t\t<timestamp>"+cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+" CST (GMT-5)"+"</timestamp>\n");
				this.write("\t\t\t</time>\n");
				
				System.out.println("\nSource Location: " + sourceName + "\nDestination: " + destName);
				
				if (this.sendHttpRequest()) {
					try {
						if (sourceName.equalsIgnoreCase("privatel")) 
							/*
							 * 	privatel
							 */
						{
							response = user.doc.findFirst("<pre>").innerText().split("\n");
							
							if (verbose) {
//								System.out.println("Tailored Response");
								for (int i = 0; i < response.length; i++) {
//									System.out.println("output["+i+"]" +response[i]);
								}
//								System.out.println();
							}
							
							for (int i = 2; i < response.length; i++) {
								String[] words = response[i].split(" ");		
								
								// Filter proper data into proper tags
								this.write("\t\t\t<data hop=\"" + (i-1) + "\">\n");
								this.write("\t\t\t\t<hop>" + (i-1) + "</hop>\n");
								this.write("\t\t\t\t<domain>" + words[0] + "</domain>\n");
								this.write("\t\t\t</data>\n");
							}
						}	else 
								/*
								 * 	General Case
								 */
							{
								filterMsg("<pre>", "\n");
							}
						
						// Format raw data for tag insertion
						String rawOutput = "";
						for (int i = 0; i < response.length; i++) {
							rawOutput = rawOutput + "\t\t\t\t" + response[i] + "\n";
						}
						this.write("\t\t\t<rawdata>" + rawOutput + "\t\t\t</rawdata>\n");
						
					} catch (NodeNotFound e) {
						System.out.println("Node <pre> was not found.");
						continue;
					}
				}
				this.write("\t\t</traceroute>\n");
			}
			this.write("\t</traceroutes>\n");
		}
		this.write("</datasample>");
	} // End of Run Method.
	
	
	/** Private Methods **/
	
	private boolean sendHttpRequest() {
		try 
		/*
		 * Checks if its GET or POST request
		 * Sends the request
		 */
		{
			if (sourceServers.get(sourceName)[0].equalsIgnoreCase("get")) {
				String getMessage = constructGetMessage(sourceServers.get(sourceName)[1], sourceServers.get(sourceName)[2], destServers.get(destName)[2]);
				user.sendGET(getMessage);
			} else if (sourceServers.get(sourceName)[0].equalsIgnoreCase("post")) {
				String postMessage = constructPostMessage(sourceServers.get(sourceName)[2], destServers.get(destName)[2]);
				user.sendPOST(sourceServers.get(sourceName)[1], postMessage);
			}
			return true;
		} catch (Exception e) {
			if (verbose) {
				e.printStackTrace();
			} else {
				System.out.println("An fatal error occured in the HTTP Request.");
			}
			return false;
		}
	}
	
	/**
	 * Method used to construct the GET HTTP Request message.
	 * 
	 * @param url The source server url used in the HTTP Request. Retreived from source.txt.
	 * @param query The query String used to send the HTTP Request
	 * @param ip The ip of the destination server.
	 * @return message The String holding the GET message send in the HTTP Request.
	 */
	private String constructGetMessage(String url, String query, String ip) {
		int indexOfDest = query.indexOf('#');
		String before = query.substring(0, indexOfDest);
		String after = query.substring((indexOfDest + 1), query.length());

		String message = url + before + ip + after;

		return message;
	}

	/**
	 * Method used to construct the POST HTTP Request message.
	 * 
	 * @param query The query String used to send the HTTP Request
	 * @param ip The destination of the destination server
	 * @return message The String holding the POST message sent in the HTTP Request
	 */
	private String constructPostMessage(String query, String ip) {
		int indexOfDest = query.indexOf('#');
		String before = query.substring(0, indexOfDest);
		String after = query.substring((indexOfDest + 1), query.length());
		String message = before + ip + after;

		return message;
	}
	
	private void write(String lines) {
		try {
			FileUtils.writeStringToFile(outputFile, lines, true);
		} catch (IOException e) {
			System.err.println("There was an error writing to file output.xml");
		}
	}
	
	private void createOutputFile() {
		File checkUniqueNumber = new File("Output/checkUniqueNumber");
		Integer uniqueNum = -1;
		try
		// Get the unique number from file, increment it by 1 and resave to that file.
		{
			uniqueNum = Integer.parseInt(FileUtils.readFileToString(checkUniqueNumber));
			String newUniqueNum = Integer.toString((uniqueNum + 1));
			FileUtils.writeStringToFile(checkUniqueNumber, newUniqueNum);
		} catch (IOException e) {
			System.err.println("File \"checkUniqueNumber\" could not be written to.");
		}
		// Create the file for our output.
		cal = Calendar.getInstance();
		outputFile = new File("Output/datasample_" + Integer.toString(uniqueNum) 
				+ "_(" + (cal.get(Calendar.MONTH)+1)
				+ "-" + cal.get(Calendar.DAY_OF_MONTH) 
				+ "-" + cal.get(Calendar.YEAR) 
				+ ").xml");
	}
	
	private void loadFiles() {
		File sourceFile = new File("InputFiles/source.txt");
		sourceServers = loadData(sourceFile, "source");

		File destFile = new File("InputFiles/destination.txt");
		destServers = loadData(destFile, "destination");
	}
	
	/**
	 * Reads data from .txt files
	 * 
	 * @param filename
	 * @return LinkedHashMap<String, String[]> with key/value read from file
	 */
	private static LinkedHashMap<String, String[]> loadData(File filename, String type)
	// Helper class to load data from file into a LinkedHashMap
	{
		LinkedHashMap<String, String[]> map = new LinkedHashMap<>();

		if (type.equalsIgnoreCase("source")) {
			// Source

			// Name (Key)

			// 0 GET/POST "GET" "POST"
			// 1 Full URI
			// 2 Query String
			// 3 AS
			// 4 Location
			try {
				Iterator<String> fileIterator = FileUtils
						.lineIterator(filename);
				while (fileIterator.hasNext()) {
					String key = fileIterator.next();
					String[] values = new String[5];
					values[0] = fileIterator.next();
					values[1] = fileIterator.next();
					values[2] = fileIterator.next();
					values[3] = fileIterator.next();
					values[4] = fileIterator.next();
					map.put(key, values);
				}

			} catch (IOException e) {
				System.out.println("File was not found.");
				System.out.println("Exiting program.");
				System.exit(1);
			}
		} else if (type.equalsIgnoreCase("destination")) {
			// Destination

			// Name (Key)

			// 0 AS
			// 1 Location
			// 2 IP
			try {
				Iterator<String> fileIterator = FileUtils
						.lineIterator(filename);
				while (fileIterator.hasNext()) {
					String key = fileIterator.next();
					String[] values = new String[3];
					values[0] = fileIterator.next();
					values[1] = fileIterator.next();
					values[2] = fileIterator.next();
					map.put(key, values);
				}

			} catch (IOException e) {
				System.out.println("File was not found.");
				System.out.println("Exiting program.");
				System.exit(1);
			}
		} else if (type.equalsIgnoreCase("serverlist")) {
			// Server List

			// Name of Source Server
			// # of servers in list.
			// Server ID
			// Server Name

			try {
				Iterator<String> fileIterator = FileUtils
						.lineIterator(filename);
				while (fileIterator.hasNext()) {
					String key = fileIterator.next();
					String[] values = new String[Integer.parseInt(fileIterator
							.next())];
					for (int i = 0; i < values.length; i++) {
						values[i] = fileIterator.next();
					}
					map.put(key, values);
				}

			} catch (IOException e) {
				System.out.println("File was not found.");
				System.out.println("Exiting program.");
				System.exit(1);
			}
		} else {

		}

		return map;
	}
	
	/**
	 * Method used to filter the data from the HTTP response into the proper xml tags.
	 * 
	 * @param outputFile The file being written to.
	 * @param user The UserAgent used to retreive the HTTP response.
	 * @param response The String array holding the HTTP response broken into lines.
	 * @param tag The tag to look for the traceroute data within the HTTP response. Usually "<pre>"
	 * @param delimiter The tag used to break the HTTP response into separate lines. Usually "\n"
	 * @param verbose If true, output[] lines will be displayed as well as other useful develop information.
	 * @return output The String array holding the HTTP response broken into lines.
	 */
	private void filterMsg(String tag, String delimiter) {
		try {
			response = user.doc.findFirst(tag).innerText().split(delimiter);
			
			if (response == null)
				return;
			
			if (verbose) {
				for (int i = 0; i < response.length; i++) {
//					System.out.println("output["+i+"]" +response[i]);
				}
//				System.out.println();
			}
			
			
			
			// Determine how many lines to trim off each side.
			boolean trimFront1 = false, trimFront2 = false, trimFront3 = false, trimFront4 = false, trimBack1 = false, trimBack2 = false;
			// Check front output lines.
			if (response[0].equals(" ") || response[0].equals("") || response[0].equals("  ") 
					|| response[0].equals("\n") || response[0].contains("trace") || response[0].contains("Trace")
					|| response[0].contains("%%")) {
				trimFront1 = true;
			}
			if (trimFront1 && response.length >= 2) {
				if (response[1].equals(" ") || response[1].equals("") || response[1].equals("\n") || response[1].contains("trace") || response[1].contains("Trace")
					|| response[1].contains("%%"))
					trimFront2 = true;
			}
			if (trimFront1 && trimFront2 && response.length >=3){
				if (response[2].equals(" ") || response[2].equals("") || response[2].equals("\n") || response[2].contains("trace") 
					|| response[2].contains("Trace") || response[2].contains("%%"))
					trimFront3 = true;
			}
			if (trimFront1 && trimFront2 && trimFront3 && response.length >=4) {
					if (response[3].equals(" ") || response[3].equals("") || response[3].equals("\n")
					|| response[3].contains("trace") || response[3].contains("Trace") || response[3].contains("%%"))
						trimFront4 = true;
			}
			
			// Check back output lines.
			if (response.length > 1) {
				if (response[(response.length-1)].equals(" ") || response[(response.length-1)].equals("") || response[(response.length-1)].equals("  ")
						|| response[(response.length-1)].equals("\n") 
						|| response[(response.length-1)].contains("Trace") || response[(response.length-1)].contains("trace") || response[(response.length-1)].contains("@")
						|| response[(response.length-1)].contains("Completed"))
					trimBack1 = true;
			}
			if (response.length > 2) {
				if (trimBack1 && (response[(response.length-2)].equals(" ") || response[(response.length-2)].equals("")  || response[(response.length-1)].equals("  ")
						|| response[(response.length-2)].equals("\n") 
						|| response[(response.length-2)].contains("Trace") || response[(response.length-2)].contains("trace") || response[(response.length-2)].contains("@")
						|| response[(response.length-1)].contains("Completed")))
					trimBack2 = true;
			}
			
			// Assign amount to trim off front or back to variables.
			int frontAmt, backAmt;
			// Front Amount
			if (trimFront4)
				frontAmt = 4;
			else if (trimFront3)
				frontAmt = 3;
			else if (trimFront2)
				frontAmt = 2;
			else if (trimFront1)
				frontAmt = 1;
			else
				frontAmt = 0;
			
			// Back Amount
			if (trimBack2)
				backAmt = 2;
			else if (trimBack1)
				backAmt = 1;
			else
				backAmt = 0;
			
			
			
			if (verbose) {
				String trimMsg = "";
				if (frontAmt > 0)
					trimMsg = trimMsg + "Trim Front "+frontAmt;
				if (backAmt > 0 && frontAmt > 0)
					trimMsg = trimMsg + " | ";
				if (backAmt > 0)
					trimMsg = trimMsg + "Trim Back "+backAmt;
//				System.out.println(trimMsg);
			}
			
			for (int i = frontAmt; i < (response.length - backAmt); i++) {
				String[] words = response[i].split(" ");
				
				if (words.length > 5) {
					if (!words[5].equals("MPLS")) {
						if (words[0].equals("")) {
							if ((!words[4].equals("*") && !words[4].equals("") && !words[4].equals(" ")) && words[4].contains("("))
								words[4] = (String) words[4].subSequence(1, (words[4].length() - 1));
							
							if ((!words[3].equals("") && !words[1].equals("")) || (!words[4].equals("") && !words[1].equals(""))){
								if (words[1].equals("") || words[1].equals(" "))
									this.write("\t\t\t<data>\n");
								else {
									this.write("\t\t\t<data hop=\"" + words[1] + "\">\n");
									this.write("\t\t\t\t<hop>" + words[1] + "</hop>\n");
								}
								if (words[3].equals("*") && !words[4].equals("*")) {
									if (!words[4].equals("") || !words[4].equals(" "))
										this.write("\t\t\t\t<domain>" + words[4] + "</domain>\n");
									this.write("\t\t\t\t<ip>" + words[3] + "</ip>\n");
									System.out.println("Domain: " + words[4]);
									System.out.println("IP Address: " + words[3]);
								}
								else {
									if (!words[3].equals("") || !words[3].equals(" "))
										this.write("\t\t\t\t<domain>" + words[3] + "</domain>\n");
									if (!words[4].equals("") || !words[4].equals(" ")) {
										this.write("\t\t\t\t<ip>" + words[4] + "</ip>\n");
									
									}
								}
								this.write("\t\t\t</data>\n");
							}
						} else if (words[1].equals("")) {
							if ((!words[3].equals("*") && !words[3].equals("")) && words[3].contains("("))
								words[3] = (String) words[3].subSequence(1, (words[3].length() - 1));
							
							if (words[0].equals("") || words[0].equals(" "))
								this.write("\t\t\t<data>\n");
							else {
								this.write("\t\t\t<data hop=\"" + words[0] + "\">\n");
								this.write("\t\t\t\t<hop>" + words[0] + "</hop>\n");
							}
							if (words[2].equals("*") && !words[3].equals("*")) {
								if (!words[3].equals("") || !words[3].equals(" "))
									this.write("\t\t\t\t<domain>" + words[3] + "</domain>\n");
								this.write("\t\t\t\t<ip>" + words[2] + "</ip>\n");
								System.out.println("Domain: " + words[3]);
								System.out.println("IP Address: " + words[2]);
							}
							else {
								if (!words[2].equals("") || !words[2].equals(" "))
									this.write("\t\t\t\t<domain>" + words[2] + "</domain>\n");
								if (!words[3].equals("") || !words[3].equals(" ")) {
									this.write("\t\t\t\t<ip>" + words[3] + "</ip>\n");
									
								}
							}
							this.write("\t\t\t</data>\n");
						} else {
							// This line show never be shown on terminal.
							// If shown, revise algorithm to filter data.
							if (verbose)
								System.out.println("Relook at algorithm.");
						}
					}
				} else if (words.length > 4) {
					if (words[0].equals("")) {
						if (!words[4].equals("*"))
							if((!words[4].equals("") && !words[5].equalsIgnoreCase("ms")) && words[4].contains("("))
								words[4] = (String) words[4].subSequence(1, (words[4].length() - 1));
						
						if ((!words[3].equals("") && !words[1].equals("")) || (!words[4].equals("") && !words[1].equals(""))){
							if (words[1].equals("") || words[1].equals(" "))
								this.write("\t\t\t<data>\n");
							else {
								this.write("\t\t\t<data hop=\"" + words[1] + "\">\n");
								this.write("\t\t\t\t<hop>" + words[1] + "</hop>\n");
							}
							if (words[3].equals("*") && !words[4].equals("*")) {
								if (!words[4].equals("") || !words[4].equals(" "))
									this.write("\t\t\t\t<domain>" + words[4] + "</domain>\n");
								this.write("\t\t\t\t<ip>" + words[3] + "</ip>\n");
								System.out.println("Domain: " + words[4]);
								System.out.println("IP Address: " + words[3]);
							}
							else {
								if (!words[3].equals("") || !words[3].equals(" "))
									this.write("\t\t\t\t<domain>" + words[3] + "</domain>\n");
								if (!words[4].equals("") || !words[4].equals(" ")) {
									this.write("\t\t\t\t<ip>" + words[4] + "</ip>\n");
								
								}
							}
							this.write("\t\t\t</data>\n");
						}
					} else if (words[1].equals("")) {
						if (!words[3].equals("*"))
							if(!words[3].equals("") && !words[4].equalsIgnoreCase("ms") && words[3].contains("("))
								words[3] = (String) words[3].subSequence(1, (words[3].length() - 1));
						
						if (words[0].equals("") || words[0].equals(" "))
							this.write("\t\t\t<data>\n");
						else {
							this.write("\t\t\t<data hop=\"" + words[0] + "\">\n");
							this.write("\t\t\t\t<hop>" + words[0] + "</hop>\n");
						}
						if (words[2].equals("*") && !words[3].equals("*")) {
							if (!words[3].equals("") || !words[3].equals(" "))
								this.write("\t\t\t\t<domain>" + words[3] + "</domain>\n");
							this.write("\t\t\t\t<ip>" + words[2] + "</ip>\n");
							System.out.println("Domain: " + words[3]);
							System.out.println("IP Address: " + words[2]);
						}
						else {
							if (!words[2].equals("") || !words[2].equals(" "))
								this.write("\t\t\t\t<domain>" + words[2] + "</domain>\n");
							if (!words[3].equals("") || !words[3].equals(" ")) {
								this.write("\t\t\t\t<ip>" + words[3] + "</ip>\n");
								
							}
						}
						this.write("\t\t\t</data>\n");
					} else {
						// This line show never be shown on terminal.
						// If shown, revise algorithm to filter data.
						if (verbose)
							System.out.println("Relook at algorithm.");
					}
				}
				
				if (verbose)
					System.out.println("output["+i+"] " + response[i]);
			} // end for
		} catch (NodeNotFound e) {
			System.out.println("An error occured while finding tag \"<pre>\"");
			return;
		}
		return;
	}

	/**
	 * Helper Method used to display the data from read files. Used to check if
	 * file is being read correctly.
	 * 
	 * @param map
	 * @param nameOfMap
	 */
	@SuppressWarnings("unused")
	private static void displayMapData(LinkedHashMap<String, String[]> map,
			String nameOfMap)
	// Helper class to display the data within the map objects.
	{
		if (nameOfMap.equalsIgnoreCase("source")) {
			System.out.println("Source Servers Map Data");
			Set<String> keySet = map.keySet();
			Iterator<String> mapIterator = keySet.iterator();
			while (mapIterator.hasNext()) {
				String keyString = mapIterator.next();
				System.out.println("Key: " + keyString + "\n" + "Values:");
				System.out.println("\tGET|POST:\t" + map.get(keyString)[0]);
				System.out.println("\tFull URI:\t" + map.get(keyString)[1]);
				System.out.println("\tQuery String:\t" + map.get(keyString)[2]);
				System.out.println("\tAS Number:\t" + map.get(keyString)[3]);
				System.out.println("\tLocation:\t" + map.get(keyString)[4]);
			}
			System.out.println("");
		} else if (nameOfMap.equalsIgnoreCase("destination")) {
			System.out.println("Destination Servers Map Data");
			Set<String> keySet = map.keySet();
			Iterator<String> mapIterator = keySet.iterator();
			while (mapIterator.hasNext()) {
				String keyString = mapIterator.next();
				System.out.println("Key: " + keyString + "\n" + "Values:");
				System.out.println("\tAS Number:\t" + map.get(keyString)[0]);
				System.out.println("\tLocation:\t" + map.get(keyString)[1]);
				System.out.println("\tIPv4 Address:\t" + map.get(keyString)[2]);
			}
			System.out.println("");
		} else if (nameOfMap.equalsIgnoreCase("serverlist")) {
			System.out.println("Server List Map Data");
			Set<String> keySet = map.keySet();
			Iterator<String> mapIterator = keySet.iterator();
			while (mapIterator.hasNext()) {
				String keyString = mapIterator.next();
				System.out.println("Key: " + keyString + "\n" + "Values:");
				System.out.println("There are: "
						+ (map.get(keyString).length / 2) + " servers.");
				for (int i = 0; i < map.get(keyString).length; i = i + 2) {
					System.out.println("Server #" + ((i / 2) + 1)
							+ "\n\t Key: " + map.get(keyString)[i]);
					System.out.println("\t\tValue: "
							+ map.get(keyString)[i + 1] + "\n");
				}
			}
			System.out.println("");
		} else {
			System.out.println("Incorrect Map Set to display.");
			return;
		}

	}
}
