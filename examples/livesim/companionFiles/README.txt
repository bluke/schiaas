LiveSim is a simulation mapped on realtime

LiveSim opens a TCP socket and waits for infstructions. Eachtime an instruction is run. The time elapse since the first connection is mesured, and the instruction is ran at that date in the simulation.


===
To test it :

$ java -cp simgrid.jar:../schiaas.jar:livesim.jar livesim.LiveSim platformTest.xml cloudTest.xml 1907
$ netcat localhost 1907

Use the 'terminate' command to kill both the livesim and the netcat. 


===
Available command :

describeInstanceTypes
describeInstanceType <type>
describeImages
describeImage <img>
describeAvailability <type>
runInstance <img> <type>
runInstances <img> <type> <num>
describeInstances
describeInstance <inst>
suspendInstance <inst>
resumeInstance <inst>
terminateInstance <inst>
terminate
help

Good luck;
