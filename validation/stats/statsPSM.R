stats <- read.table("stats.dat", header=TRUE)
stats <- stats[c("source","BTU","makespan","wpo.BTU","wpo.makespan","wto.BTU","wto.makespan")]

ce <- data.frame(expe="bothequal",
	ASAP.BTU=100,ASAP.makespan=100,
	ASAP.wpo.BTU=100,ASAP.wpo.makespan=100,
	ASAP.wto.BTU=100,ASAP.wto.makespan=100,
	AFAP.BTU=100,AFAP.makespan=100,
	AFAP.wpo.BTU=100,AFAP.wpo.makespan=100,
	AFAP.wto.BTU=100,AFAP.wto.makespan=100,
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

#ce <- rbind.data.frame(ce,c("double",200,50,200,50,200,50,100,100,100,100,100,100))
#ce <- rbind.data.frame(ce,c("halfSpeed",200,75,200,75,200,75,100,100,100,100,100,100))
#ce <- rbind.data.frame(ce,c("halfPrice",150,50,150,50,150,50,100,100,100,100,100,100))
#ce <- rbind.data.frame(ce,c("samePrice",100,50,100,50,100,50,100,100,100,100,100,100))
#ce <- rbind.data.frame(ce,c("lessPrice",50,50,50,50,50,50,100,100,100,100,100,100))
ce <- ce[-1,]
write.table(ce[ce[,"ASAP.BTU"] == "NaN" | ce[,"AFAP.BTU"] == "NaN" ,],
	file="missing-expes.dat", sep="\t", row.names=FALSE)

ce <- ce[!ce[,"ASAP.BTU"] == "NaN" & !ce[,"AFAP.BTU"] == "NaN" ,]
ce[,2:13] <- sapply(ce[,2:13], as.numeric)

costup <- function(afap.btu,asap.btu) {
	return (asap.btu/afap.btu)
}
speedup <- function(afap.makespan,asap.makespan) {
	return (afap.makespan/asap.makespan)
}
efficiency <- function(afap.btu,asap.btu, afap.makespan,asap.makespan) {
	return (speedup(afap.makespan,asap.makespan)/costup(afap.btu,asap.btu))
}


real <- cbind(ce[,c("expe","ASAP.BTU","AFAP.BTU","ASAP.makespan","AFAP.makespan")], 
	costup=costup(ce$AFAP.BTU,ce$ASAP.BTU), 
	speedup=speedup(ce$AFAP.makespan,ce$ASAP.makespan), 
	efficiency=efficiency(ce$AFAP.BTU,ce$ASAP.BTU,ce$AFAP.makespan,ce$ASAP.makespan)) 
real[,2:8] <- round(real[,2:8],2)
write.table(real, file="efficiency-real.dat", sep="\t", row.names=FALSE)

wpo <- cbind(ce[,c("expe","ASAP.wpo.BTU","AFAP.wpo.BTU","ASAP.wpo.makespan","AFAP.wpo.makespan")], 
	costup=costup(ce$AFAP.wpo.BTU,ce$ASAP.wpo.BTU), 
	speedup=speedup(ce$AFAP.wpo.makespan,ce$ASAP.wpo.makespan), 
	efficiency=efficiency(ce$AFAP.wpo.BTU,ce$ASAP.wpo.BTU,ce$AFAP.wpo.makespan,ce$ASAP.wpo.makespan)) 
wpo[,2:8] <- round(wpo[,2:8],2)
write.table(wpo, file="efficiency-wpo.dat", sep="\t", row.names=FALSE)


wto <- cbind(ce[,c("expe","ASAP.wto.BTU","AFAP.wto.BTU","ASAP.wto.makespan","AFAP.wto.makespan")], 
	costup=costup(ce$AFAP.wto.BTU,ce$ASAP.wto.BTU), 
	speedup=speedup(ce$AFAP.wto.makespan,ce$ASAP.wto.makespan), 
	efficiency=efficiency(ce$AFAP.wto.BTU,ce$ASAP.wto.BTU,ce$AFAP.wto.makespan,ce$ASAP.wto.makespan)) 
wto[,2:8] <- round(wto[,2:8],2)
write.table(wto, file="efficiency-wto.dat", sep="\t", row.names=FALSE)

eff <- cbind(real[,c("expe", "efficiency")], 
	efficiency.wpo=wpo$efficiency, 
	efficiency.wto=wto$efficiency, 
	error.wpo=abs((wpo$efficiency-real$efficiency)/real$efficiency),
	error.wto=abs((wto$efficiency-real$efficiency)/real$efficiency))
eff[,2:6] <- sapply(eff[,2:6], as.numeric)
eff[,2:6] <- round(eff[,2:6],2)
write.table(eff, file="efficiency-error.dat", sep="\t", row.names=FALSE)


pdf('efficiency-error-cdf.pdf')
plot(ecdf(eff$error.wpo),verticals=TRUE, do.p=FALSE, lty=1, col="red", 
	main="efficiency prediction error", xlab="accuracy", ylab="amount of cases")
lines(ecdf(eff$error.wto),verticals=TRUE, do.p=FALSE, lty=3, col="red")
legend(0.7,0.3,c("wpo","wto"),lty=c(1,3),col=c("red","red"))
dev.off()

