A few tips to ensure smooth execution of the application.
This application was made in a Linux (Ubuntu 18.04) environment on Sublime 3.0 text editor.
It is therefore recommended that the application be executed in a Linux (or at least UNIX) based environment.

1) Extract all the files into one Folder.
2) Open this particular directory in the terminal.
3) To call the Makefile into action, execute the following commands.

	- $make clean
	- $make run
	(The "make clean" command is just to ensure that any previous instance of compiled classes is removed)

4) To run the application, execute the following commands in two separate terminals (Ideally, run the Server program first followed by Client).

	- $java Server
	- $java Client

5) To change the input file (data01.txt or data02.txt), navigate to line 383 of Client.java and make the following changes
	
	- inputFile = new FileReader("data0x.text") //where x is your input file number. Default number is 1. 

 |-| |-|
    X 
  -----
