# TFTP_server. 
This Project is a Java File-Transfer server, that uses our own file transfer protocol.  
**TFTP** = Trivial File Transfer Protocol.  
It uses our own packet structure, as described in the instructions.pdf file.  
The Server's Architecture is 'Thread-Per-Client', we got a main 'listening' thread, that awaits new connections;  
And for each client connected, a personal 'working' thread is created.  
The API of the server is built in pure Java (no libraries).  
also included is a client.jar client for use with the server.  
  
Enjoy.
	