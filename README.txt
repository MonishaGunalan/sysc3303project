
IDE Used: Eclipse
========

Setup Instruction
=================
Please import the following Java source files to a new project in Eclipse

Java Source Files:
=================
'package sysc3303.project': 	TftpClient.java
			    	TftpServer.java
			    	TftpErrorSimulator.java

'package sysc3303.project.packets': TftpAckPacket.java
				    TftpDataPacket.java									    TftpErrorPacket.java
				    TftpPacket.java
				    TftpRequestPacket.java


Server
======
Run to start the server. Start server first.
Commands: help, stop, pwd

Error Simulator
===============
Run to start error simulator. Start error simulator before client.
choose the error mode to simulate an error before running the client
Commands: help(lists the available error modes),
	  stop(stop the error simulator (when current transfers finish))
          error(choose the error mode to simulate an error)
	



Client
======
Run after server and error simulator are started.
Commands: read <<filename>>, write <<filename>>, help, close


Diagrams:
=========
Timing Diagram - AckPacketError.jpeg
Timing Diagram - Normal mode.jpeg
Timing Diagram - PacketSizeError.jpeg
Timing Diagram - Invalid TID.jpeg
Timing Diagram - OpcodeError.jpeg
Timing Diagram - RequestPacketError.jpeg
UCM Diagrams.pdf
UMLClassDiagram.jpeg



Files for the client are stored in "client_files", files for the server are stored in "server_files"