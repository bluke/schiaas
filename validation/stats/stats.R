# R script to compute validation stats

stats <- read.table("stats.dat", header=TRUE)

btu <- stats[c("source","BTU","wpo.BTU","wto.BTU")]
btu$wpo.d <- NA
btu$wpo.d <- abs( btu$BTU - btu$wpo.BTU ) / btu$BTU
btu$wto.d <- NA
btu$wto.d <- abs( btu$BTU - btu$wto.BTU ) / btu$BTU
write.table(btu[ with(btu, order(wpo.d)), ], file="btu.dat", row.names=FALSE)


makespan <- stats[c("source","makespan","wpo.makespan","wto.makespan")]
makespan$wpo.d <- NA
makespan$wpo.d <- abs( makespan$makespan - makespan$wpo.makespan ) / makespan$makespan
makespan$wto.d <- NA
makespan$wto.d <- abs( makespan$makespan - makespan$wto.makespan ) / makespan$makespan
write.table(makespan[ with(makespan, order(wpo.d)), ], file="makespan.dat", row.names=FALSE)



pdf('global-cdf.pdf')
plot(ecdf(btu$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="red", 
	main="Global error", xlab="error", ylab="amount of cases")
lines(ecdf(btu$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="red")
lines(ecdf(makespan$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="blue")
lines(ecdf(makespan$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="blue")
legend(0.7,0.3,c("btu wpo","btu wto","makespan wpo","makespan wto"),lty=c(1,3,1,3),col=c("red","red","blue","blue"))
dev.off()

pdf('expes-btu-cdf.pdf')
plot(ecdf(btu[grep("pleiade",btu$source),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="red",
	main="BTU error per xp", xlab="error", ylab="amount of cases")
lines(ecdf(btu[grep("pleiade",btu$source),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="red")
lines(ecdf(btu[grep("pleiade",btu$source,invert=TRUE),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="blue")
lines(ecdf(btu[grep("pleiade",btu$source,invert=TRUE),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="blue")
legend(0.7,0.3,c("montage btu wpo","montage btu wto","omssa btu wpo","omssa btu wto"),lty=c(1,3,1,3),col=c("red","red","blue","blue"))
dev.off()

pdf('cloud-btu-cdf.pdf')
plot(ecdf(btu[grep("openstack",btu$source),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="red",
	main="BTU error per cloud", xlab="error", ylab="amount of cases")
lines(ecdf(btu[grep("openstack",btu$source),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="red")
lines(ecdf(btu[grep("bonfire",btu$source),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="blue")
lines(ecdf(btu[grep("bonfire",btu$source),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="blue")
legend(0.7,0.3,c("icps btu wpo","icps btu wto","bonfire btu wpo","bonfire btu wto"),lty=c(1,3,1,3),col=c("red","red","blue","blue"))
dev.off()

pdf('expes-makespan-cdf.pdf')
plot(ecdf(makespan[grep("pleiade",makespan$source),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="red",
	main="Makespan error per xp", xlab="error", ylab="amount of cases")
lines(ecdf(makespan[grep("pleiade",makespan$source),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="red")
lines(ecdf(makespan[grep("pleiade",makespan$source,invert=TRUE),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="blue")
lines(ecdf(makespan[grep("pleiade",makespan$source,invert=TRUE),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="blue")
legend(0.7,0.3,c("montage makespan wpo","montage makespan wto","omssa makespan wpo","omssa makespan wto"),lty=c(1,3,1,3),col=c("red","red","blue","blue"))
dev.off()

pdf('cloud-makespan-cdf.pdf')
plot(ecdf(makespan[grep("openstack",makespan$source),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="red",
	main="Makespan error per cloud", xlab="error", ylab="amount of cases")
lines(ecdf(makespan[grep("openstack",makespan$source),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="red")
lines(ecdf(makespan[grep("bonfire",makespan$source),]$wpo.d),verticals=TRUE, do.p=FALSE, lty=1, col="blue")
lines(ecdf(makespan[grep("bonfire",makespan$source),]$wto.d),verticals=TRUE, do.p=FALSE, lty=3, col="blue")
legend(0.7,0.3,c("icps makespan wpo","icps makespan wto","bonfire makespan wpo","bonfire makespan wto"),lty=c(1,3,1,3),col=c("red","red","blue","blue"))
dev.off()



min(btu$wpo.d)
max(btu$wpo.d)
mean(btu$wpo.d)

min(btu$wto.d)
max(btu$wto.d)
mean(btu$wto.d)


min(makespan$wpo.d)
max(makespan$wpo.d)
mean(makespan$wpo.d)

min(makespan$wto.d)
max(makespan$wto.d)
mean(makespan$wto.d)
