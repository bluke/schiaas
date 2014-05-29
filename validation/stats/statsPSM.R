stats <- read.table("stats.dat", header=TRUE)
stats <- stats[c("source","BTU","makespan","wpo.BTU","wpo.makespan","wto.BTU","wto.makespan")]

ce <- data.frame(expe="foo",
	ASAP.BTU=1,ASAP.makespan=1,
	ASAP.wpo.BTU=1,ASAP.wpo.makespan=1,
	ASAP.wto.BTU=1,ASAP.wto.makespan=1,
	AFAP.BTU=1,AFAP.makespan=1,
	AFAP.wpo.BTU=1,AFAP.wpo.makespan=1,
	AFAP.wto.BTU=1,AFAP.wto.makespan=1,
	stringsAsFactors=FALSE)


for(e in c("hrt","hrs","brt","brs","pleiades-1x1","pleiades-2x2","pleiades-3x3","pleiades-4x4")) {
	for(c in c("openstack","bonfire-uk","bonfire-de","bonfire-fr")) {
		expe <- paste(c,".*",e,".*",sep="")
		expeAsap <- paste(expe,"Asap",sep="")
		expeAfap <- paste(expe,"Afap",sep="")
		ce <- rbind.data.frame(ce,c(expe,
			colMeans(stats[grep(expeAsap,stats$source),2:7]),
			colMeans(stats[grep(expeAfap,stats$source),2:7])))
	}
}
ce <- ce[-1,]
ce <- ce[!ce[,"ASAP.BTU"] == "NaN" & !ce[,"AFAP.BTU"] == "NaN" ,]
ce[,2:13] <- sapply(ce[,2:13], as.numeric)


real <- cbind(ce[,c("expe","ASAP.BTU","AFAP.BTU","ASAP.makespan","AFAP.makespan")], 
	dPrice=ce$ASAP.BTU/ce$AFAP.BTU-1, 
	dSpeed=ce$AFAP.makespan/ce$ASAP.makespan, 
	interest=(ce$ASAP.BTU/ce$AFAP.BTU-1)/(ce$AFAP.makespan/ce$ASAP.makespan)) 
real[,2:8] <- round(real[,2:8],2)
write.table(real, file="interest-real.dat", sep="\t")

wpo <- cbind(ce[,c("expe","ASAP.wpo.BTU","AFAP.wpo.BTU","ASAP.wpo.makespan","AFAP.wpo.makespan")], 
	dPrice=ce$ASAP.wpo.BTU/ce$AFAP.wpo.BTU-1, 
	dSpeed=ce$AFAP.wpo.makespan/ce$ASAP.wpo.makespan,
	interest=(ce$ASAP.wpo.BTU/ce$AFAP.wpo.BTU-1)/(ce$AFAP.wpo.makespan/ce$ASAP.wpo.makespan)) 
wpo[,2:8] <- round(wpo[,2:8],2)
write.table(wpo, file="interest-wpo.dat", sep="\t")

wto <- cbind(ce[,c("expe","ASAP.wto.BTU","AFAP.wto.BTU","ASAP.wto.makespan","AFAP.wto.makespan")], 
	dPrice=ce$ASAP.wto.BTU/ce$AFAP.wto.BTU-1, 
	dSpeed=ce$AFAP.wto.makespan/ce$ASAP.wto.makespan, 
	interest=(ce$ASAP.wto.BTU/ce$AFAP.wto.BTU-1)/(ce$AFAP.wto.makespan/ce$ASAP.wto.makespan))
wto[,2:8] <- round(wto[,2:8],2)
write.table(wto, file="wto-real.dat", sep="\t")

interest <- cbind(real[,c("expe", "interest")], 
	wpo$interest, abs(wpo$interest-real$interest)/real$interest,
	wto$interest, abs(wto$interest-real$interest)/real$interest)
interest[,2:6] <- sapply(interest[,2:6], as.numeric)
interest[,2:6] <- round(interest[,2:6],2)
write.table(interest, file="interest.dat", sep="\t")


pdf('interest-cdf.pdf')
plot(ecdf(interest[,4]),verticals=TRUE, do.p=FALSE, lty=1, col="red", 
	main="Score accuracy", xlab="accuracy", ylab="amount of cases")
lines(ecdf(interest[,6]),verticals=TRUE, do.p=FALSE, lty=3, col="red")
legend(0.7,0.3,c("wpo","wto",lty=c(1,3),col=c("red","red")))
dev.off()

