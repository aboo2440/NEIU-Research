NEIU-Research
=============

Northeastern Illinois University Internet Topology Research was a research project done in the summer of 2014. The members of the research group were Dr. Graciela Perera, Bonnie Tongs, Charla Earls, Michael Abreu, and Karl Amjad-Ali. Our research was aimed at updating the findings of previous research done in 2008 by Gill, et al. [1].

Files
=============

Summary of files in project:
  - The <b>data</b> folder contains all data files collected during our research. These files are in xml format.
  - The <b>Data Automation Program</b> is the tool we used to collect traceroute data from public traceroute servers. This tool was written in Java. No GUI was made for our tool because we were pressed for time. The tool can be reconfigured easily using the two text files:
    - <b>Data Automation Program / Collection / config / </b> source.txt
    - <b>Data Automation Program / Collection / config / </b> destination.txt

Configuration format of source.txt and destination.txt

<b>All lines are required and can't be skipped. A missing line may cause an error in the program.</b><br/>

<b>source.txt</b><br/>
Line 1:(Name of source server)<br/>
Line 2:(Method of http request, i.e. GET | POST)<br/>
Line 3:(URI of public server)(If using GET, remove the GET query string from this line and add it below)<br/>
Line 4:(GET|POST query string, substitute the location in the string where you input an IP address with #)<br/>
Line 5:(AS number of source server)<br/>
Line 6:(Location, i.e. COUNTRY - STATE - CITY)<br/>

Example:<br/>
Method GET<br/>
Line 1:ThunderWorx (PrimeTel NOC)<br/>
Line 2:GET<br/>
Line 3:http://noc.thunderworx.net/cgi-bin/public/traceroute.pl<br/>
Line 4:?target=#<br/>
Line 5:AS12033<br/>
Line 6:Cyprus<br/>

Method POST<br/>
Line 1:Colocation American Corporation<br/>
Line 2:POST<br/>
Line 3:http://www.colocationamerica.com/data-center-connectivity/speed-test.htm<br/>
Line 4:remoteAddress=#<br/>
Line 5:AS21769<br/>
Line 6:USA - Nevada - Las Vegas<br/>

<b>destination.txt</b><br/>
Line 1:(Name of destination server)<br/>
Line 2:(AS number of destination server)<br/>
Line 3:(Location, i.e. COUNTRY - STATE - CITY)<br/>
Line 4:(IP address of destination server)<br/>

Example:<br/>
Line 1:Google.com<br/>
Line 2:AS15169<br/>
Line 3:USA - California<br/>
Line 4:173.194.46.70<br/>

Installation
=============

Download the files. Open a command prompt that has Java installed to its PATH variable. Navigate into 'Data Automation Program' and run the command: <b>java -jar "Data Automation Program.jar"</b>

References
=============
[1] Gill, Phillipa, et al. "The flattening internet topology: Natural evolution, unsightly barnacles or contrived collapse?." Passive and Active Network Measurement. Springer Berlin Heidelberg, 2008. 1-10.
