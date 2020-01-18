
# define a makefile variable for the java compiler

JC = javac

# define a makefile variable for compilation flags
# the -Xlint flag compiles with extra warning information

JFLAG = -Xlint

run: PacketCreator.class KMeans.class Server.class Client.class

# this target entry builds the PacketCreator class first and subsequently 
# the other classes in that order.

PacketCreator.class: PacketCreator.java
		$(JC) PacketCreator.java $(JFLAG)

KMeans.class: KMeans.java
		$(JC) KMeans.java $(JFLAG) 

Server.class: Server.java
		$(JC) Server.java $(JFLAG)

Client.class: Client.java
		$(JC) Client.java $(JFLAG) 

# Removes all .class files, so that the next make rebuilds them
clean: 
		$(RM) *.class