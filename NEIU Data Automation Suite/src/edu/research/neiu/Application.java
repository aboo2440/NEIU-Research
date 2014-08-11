package edu.research.neiu;

import java.util.Scanner;

import edu.research.neiu.collection.DataCollector;
import edu.research.neiu.organization.DataOrganization;

public class Application {

	public static void main(String[] args) {
		
		DataCollector collector = new DataCollector();
		DataOrganization organizer = new DataOrganization();
		
		System.out.println("Traceroute Data Automation Suite of Tools."
				+ "\nCopyright (C) 2014  Michael Abreu, Karl Amjad-Ali"
				+ "\nThis program comes with ABSOLUTELY NO WARRANTY."
				+ "\nThis is free software, and you are welcome to redistribute it"
				+ "under certain conditions;");
		
		Scanner in = new Scanner(System.in);
		String response = "";
		
		while (true) {
		
			System.out.print("\nMain Menu"
				+ "\nAvailable Tools:"
				+ "\n\tData Collector"
				+ "\n\tData Organizer"
				+ "\n\nEnter the tool name you would like to use: "
				+ "\nCommand > ");
			if ( in.hasNext() )
				response = in.nextLine();
			
			if (response.equalsIgnoreCase("exit")){
				in.close();
				System.out.println("Thanks for using our program!");
				break;
			} else if (response.equalsIgnoreCase("data collector")) {
				collector.run(in);
			} else if (response.equalsIgnoreCase("data organizer")) {
				organizer.run(in);
			} else
				System.out.println("Sorry invalid option. Try again.");
		}
		
	}
	
}
