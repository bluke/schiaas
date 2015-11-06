

cmpscheduler <- function(series){
	series <<- series

	pdf('scheduler-busy-hosts.pdf')
	instances_count <- read.table(paste(series[1],'-instances_count.dat',sep=""), header=FALSE)
	plot(instances_count$V2,instances_count$V3, xlab='time', ylab='count',t='S')
	
	par(new=TRUE)
	mydata <<- read.table(paste(series[1],'-used_cores-neq-0.dat',sep=""), header=FALSE)
	plot(mydata$V1,mydata$V2,t='S',ylim=c(0,max(mydata$V2)))

	for (serie in tail(series,-1)) {
		mydata <<- read.table(paste(serie,'-used_cores-neq-0.dat',sep=""), header=FALSE)
		lines(mydata$V1,mydata$V2,t='S')
	}
	dev.off()

	pdf('scheduler-used-cores.pdf')
	mydata <<- read.table(paste(series[1],'-used_cores.dat',sep=""), header=FALSE)
	plot(mydata$V2,mydata$V3)
	for (serie in tail(series,-1)) {
		mydata <<- read.table(paste(serie,'-used_cores.dat',sep=""), header=FALSE)
		points(mydata$V2,mydata$V3)
	}
	dev.off()
}

# series
cmpscheduler(c(%series%))

