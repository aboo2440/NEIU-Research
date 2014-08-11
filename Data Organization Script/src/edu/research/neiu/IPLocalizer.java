package edu.research.neiu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@SuppressWarnings("deprecation")
public class IPLocalizer {
	
	// Attributes
	private File databaseFile;
	private LinkedHashMap<String, String[]> database;
	private URL u;
	private InputStream in;
	private XMLInputFactory factory;
	private XMLStreamReader parser;
//	private String currentIP;	// Used to keep track of IP while writting xml.
	
	private static int attempts;
	private final File attemptsFile = new File("Output/attempts.txt");
	
	// XML Tools
	private DocumentBuilderFactory documentBuilderFactory;
	private DocumentBuilder documentBuilder;
	private Document doc;

	// IPv4 Regex Pattern
	private final String ipPattern = "(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]?[0-9])";
	
	// Run Method
	public void run() {
		try {
			databaseFile = new File("Output/database.xml");
			u = databaseFile.toURI().toURL();
			in = u.openStream();
			factory = XMLInputFactory.newInstance();
			parser = factory.createXMLStreamReader(in);
			database = loadDatabase();
			
			Scanner userIn = new Scanner(System.in);
			System.out.println("Welcome to IP Localizer!"
					+"\nEnter the text \"load\" to prompt file loader."
					+"\nEnter the text \"display\" to display entire database."
					+"\nEnter the text \"query [IP Address]\" to search the database for an IP address."
					+"\nEnter the text \"attempts\" to display number of attempts. Max limit is 10,000 daily."
					+"\nEnter the text \"help\" to display this message again."
					+"\nEnter the text \"exit\" to exit program."
					+"\nEnter any IP Address to search for its' AS Number, Location, and Coordinates.");
			while (true) {
				System.out.print("IP Localizer> ");
				String response = userIn.nextLine();
				if (response.equalsIgnoreCase("exit")){
					System.out.println("Goodbye.");
					break;
				} else if (response.equalsIgnoreCase("help")) {
					System.out.println("Enter the text \"load\" to prompt file loader."
							+"\nEnter the text \"display\" to display entire database."
							+"\nEnter the text \"query [IP Address]\" to search the database for an IP address."
							+"\nEnter the text \"attempts\" to display number of attempts. Max limit is 10,000 daily."
							+"\nEnter the text \"help\" to display this message again."
							+"\nEnter the text \"exit\" to exit program."
							+"\nEnter any IP Address to search for its' AS Number, Location, and Coordinates.");
				} else if (response.equalsIgnoreCase("display")) {
					displayDatabase();
				} else if (response.equalsIgnoreCase("load")) {
					System.out.print("Enter the name of the file to load: ");
					String file = userIn.nextLine();
					File loadFile = new File("Input/"+file);
					loadFile(loadFile);
				} else if (response.equalsIgnoreCase("attempts")) {
					System.out.println("Attempts: " + getAttempts());
				} else if (response.substring(0, 5).equalsIgnoreCase("query")) {
					System.out.println("Querying: " + response.substring(6, response.length()));
					String[] queryResponse = query(response.substring(6, response.length()));
					if (queryResponse == null) {
						System.out.println("That IP Address was not found.");
					} else {
						System.out.println("\tAS Number:\t" + queryResponse[0]);
						System.out.println("\tLocation:\t" + queryResponse[1]);
						System.out.println("\tCoordinates:\t" + queryResponse[2]);
					}
				}
				else {
					if (response.matches(ipPattern)){
						if (search(response)){
							// True
							System.out.println("That IP address is in the database.");
						} else {
							// False - IP isn't in database
							String[] information = getInfo(response);
							System.out.println("AS Number: " + information[0]);
							System.out.println("Location: " + information[1]);
							System.out.println("Coordinates: " + information[2]);
							
							
							
							System.out.println("That IP address has been added to the database.");
						}
					} else {
						System.out.println("Not a valid IPv4 Addres. Try again.");
					}
				}
			} // End of While Loop
			// Close everything.
			userIn.close();
			parser.close();
			in.close();
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 	Helper Methods
	 */
	
	// Loading and Creating Files
	
	private LinkedHashMap<String, String[]> loadDatabase() {
		LinkedHashMap<String, String[]> map = new LinkedHashMap<>();
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(databaseFile);
			
			doc.getDocumentElement().normalize();
			
			NodeList setList = doc.getElementsByTagName("set"); // List of sets
			
			for (int i = 0; i < setList.getLength(); i++) {
				Element set = (Element) setList.item(i); // Get current set
				
				NodeList ipList = set.getElementsByTagName("ip");
				NodeList asnList = set.getElementsByTagName("asn");
				NodeList locationList = set.getElementsByTagName("location");
				NodeList coordinatesList = set.getElementsByTagName("coordinates");
				
				Element ip = (Element) ipList.item(0);
				Element asn = (Element) asnList.item(0);
				Element location = (Element) locationList.item(0);
				Element coordinates = (Element) coordinatesList.item(0);
				
				String key = ip.getTextContent(); // Store ip
				String[] values = new String[3];
				values[0] = asn.getTextContent(); // Store asn
				values[1] = location.getTextContent(); // Store location
				values[2] = coordinates.getTextContent(); // Store coordinates
						
				map.put(key, values);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	
	
	private void displayDatabase() {
		Set<String> keySet = database.keySet();
		Iterator<String> mapIterator = keySet.iterator();
		System.out.println("Database:");
		while (mapIterator.hasNext()) {
			String keyString = mapIterator.next();
			System.out.println("\tIP Address: " + keyString);
			System.out.println("\t\tAS Number:\t" + database.get(keyString)[0]);
			System.out.println("\t\tLocation:\t" + database.get(keyString)[1]);
			System.out.println("\t\tCoordinates:\t" + database.get(keyString)[2]);
		}
	}
	
	// Input File
	
	private void loadFile(File filename) {
		try {
			
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			if (checkFileType(filename)) {
				
				doc = documentBuilder.parse(filename);
				doc.getDocumentElement().normalize();
				NodeList sourceList = doc.getElementsByTagName("traceroutes");
			 
				for (int i = 0; i < sourceList.getLength(); i++) {
					 
					Node srcServer = sourceList.item(i); // Iterator on <traceroutes>
					System.out.println("\nSource Server: " + srcServer.getAttributes().item(0).getNodeValue());
					NodeList tracerouteList = srcServer.getChildNodes(); // <traceroute> list
					
					for (int k = 0; k < tracerouteList.getLength(); k++) {
						
						Node curDest = tracerouteList.item(k); // Iterator on <traceroute>
						
						if (curDest.getNodeType() == Node.ELEMENT_NODE) {
							 
							int j = 0;
							
							while (curDest.hasChildNodes()) {

								System.out.println("Destination Server: " + curDest.getAttributes().item(0).getNodeValue());
								Element destElement = (Element) curDest;
								
								if (srcServer.getAttributes().item(0).getNodeValue().equalsIgnoreCase("privatel"))
									System.out.print("");
								
								NodeList dataList = destElement.getElementsByTagName("data");
								Node dataNode = dataList.item(j);
								j++;
								
								if (dataNode == null)
									break;

								while (dataNode.hasChildNodes()) {
									
									Element data = (Element) dataNode;
									NodeList hops = data.getElementsByTagName("hop");
									Node hop = hops.item(0);
									NodeList ips = data.getElementsByTagName("ip");
									Node ip = ips.item(0);

									if (srcServer.getAttributes().item(0).getNodeValue().equalsIgnoreCase("ITFN"))
										System.out.print("");
									
									if (hop == null | ip == null)
										break;
									
									if (!ip.getTextContent().equals("*") && !ip.getTextContent().equals("")) {

										int result = addToDatabase(ip.getTextContent());
										
										if (result == 1) // IP was not in database.
											System.out.println("Hop "+ hop.getTextContent()
													+"\tIP Address: "+ ip.getTextContent());
										
										// Search for it
										String[] values = query(ip.getTextContent());
									
										// Write tags to file.
										Element asn = doc.createElement("asn");
										Element location = doc.createElement("location");
										Element coordinates = doc.createElement("coordinates");
										
										asn.appendChild(doc.createTextNode(values[0]));
										location.appendChild(doc.createTextNode(values[1]));
										coordinates.appendChild(doc.createTextNode(values[2]));
										
										data.appendChild(asn);
										data.appendChild(location);
										data.appendChild(coordinates);
									}
									
									if (dataNode.getNextSibling().getNextSibling().getNodeName().equalsIgnoreCase("rawdata"))
										break;
									
									dataNode = dataNode.getNextSibling().getNextSibling();
								}
								
								curDest = curDest.getNextSibling(); // Iterator to next <traceroute>
							}
						} // End check
					} // End Loop
				}
				
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource domSource = new DOMSource(doc);
				String newFilename = filename.toString().substring(6, filename.toString().length());
				StreamResult result = new StreamResult(new File("Output/updated_"+newFilename));
				transformer.transform(domSource, result);
				System.out.println("File successfully loaded.");
			} else {
				System.out.println("The file was not loaded successfully. Try again."
						+ "\nFile Path:"+filename);
				return;
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	private void writeToDatabase(String newIP, String newASN, String newLocation, String newCoordinates) {
		try {
			
			String filepath = "Output/database.xml";
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filepath);
			
			
			Element root = (Element) doc.getFirstChild();
			
			// Create Set Tag
			Element set = doc.createElement("set");
			
			// Create IP Tag
			Element ip = doc.createElement("ip");
			ip.appendChild(doc.createTextNode(newIP));
			
			// Create ASN Tag
			Element asn = doc.createElement("asn");
			asn.appendChild(doc.createTextNode(newASN));
			
			// Create Location Tag
			Element location = doc.createElement("location");
			location.appendChild(doc.createTextNode(newLocation));
			
			// Create Coordinates Tag
			Element coordinates = doc.createElement("coordinates");
			coordinates.appendChild(doc.createTextNode(newCoordinates));
			
			
			// Append all to Set
			set.appendChild(ip);
			set.appendChild(asn);
			set.appendChild(location);
			set.appendChild(coordinates);
			root.appendChild(set);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("Output/database.xml"));
			transformer.transform(domSource, result);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Boolean checkFileType(File filename) {
		String xmlPattern = ".*.xml";
		String file = filename.getPath();
		
		if (file.matches(xmlPattern))
			return true;
		else
			return false;
	}
	
	private int addToDatabase(String ip) {
		boolean result = search(ip);
		if (result) {
			return 0; // 0 - Already in database 
		}
		else if (!result) {
			
			// Get the as, location, coordinates from http://ipinfo.io/
			String[] info = getInfo(ip);
			String asn = info[0];
			String location = info[1];
			String coordinates = info[2];
			
			// Add to loaded database
			String[] values = new String[3];
			values[0] = asn;
			values[1] = location;
			values[2] = coordinates;
			database.put(ip, values);
			
			// Write to database file
			writeToDatabase(ip, values[0], values[1], values[2]);
			
			return 1; // 1 - Added to database
		}
		
		return -1; // Error
	}
	
	private boolean search(String ip) {
		if (database.get(ip) == null)
			return false; // False - IP was not found in database.
		else 
			return true; // True - IP was found in database.
	}
	
	private String[] query(String ip) {
		if (search(ip)) {
			
			String[] list = new String[3];
			list[0] = database.get(ip)[0]; // AS
			list[1] = database.get(ip)[1]; // Location
			list[2] = database.get(ip)[2]; // Coordinates
			
			return list;
		}
		else
			return null;
	}
	
	private static int getAttempts() {
		return attempts;
	}
	
	@SuppressWarnings("unused")
	private void loadAttempts() {
		try {
			Iterator<String> fileIterator = FileUtils.lineIterator(attemptsFile);
			
			// File Structure:
			// 1:	Attempts Integer
			// 2:	Instant Time
			 int oldAttempts = Integer.parseInt(fileIterator.next());
			 Instant timeOld = Instant.parse(fileIterator.next());
			
			Instant timeNow = Instant.now(); 
			if(timeNow.minusSeconds(86400).isBefore(timeOld))
			{
				if(oldAttempts < 10000)
				{
					attempts = oldAttempts;
				}
				
				if(oldAttempts >= 10000)
				{
					attempts = 0;
					System.out.println("You have reached the 10,000 rate limit allowed within a 24 hour period");	 
				}
			}
			else
			{
				attempts = 0;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private void writeAttempts(Instant time) {
		// assume the Instant time variable
		File attemptsFile = new File("Input/attemptsFile.xml");
		String attemptsValue = Integer.toString(attempts);
		String currentInstant = time.toString();
		try
		{
		FileUtils.write(attemptsFile, attemptsValue, true);
		FileUtils.write(attemptsFile, currentInstant, true);
		}
		
		catch(IOException e){
			e.printStackTrace();
			}
		}
	
	@SuppressWarnings({ "resource" })
	private static String[] getInfo(String ip) {
		String[] location = new String[3];
		location[0] = "";
		location[1] = "";
		location[2] = "";
		
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet("http://ipinfo.io/" +(ip + "/json?token=9074c87c013094"));
		
		
		try {
			HttpResponse response = client.execute(get);
			attempts++;
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			client.getConnectionManager().closeExpiredConnections();
			
			String line = "";
			String[] locations = new String[10];
			int k = 0;
			while ((line = rd.readLine()) != null) {
				locations[k] = line;
				k++;
			}
			if (locations[0].contains("Rate limit"))
				return null;
			else {
				if (!locations[4].contains("bogon")) {
					// Get Location
					if (!locations[5].contains("null") && locations[5] != null && (locations[5].length() > 14)) {
						location[1] = locations[5].substring(14, (locations[5].length() - 2));
						if (!locations[4].contains("null")) {
							location[1] = location[1] + " - " + locations[4].substring(13, (locations[4].length() - 2));
							if (!locations[3].contains("null"))
								location[1] = location[1] + " - " + locations[3].substring(11, (locations[3].length() - 2));
						}
					}
					// Get coordinates.
					if (locations[6].length() > 13)
					location[2] = locations[6].substring(10, (locations[6].length() - 2));
					
					// Get asn
					if (locations[7].length() > 13)
						location[0] = locations[7].substring(10, (locations[7].length() - 2));
				}
			}
			
			
			return location;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}
