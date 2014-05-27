# R script to compute validation stats

stats <- read.table("stats.dat", header=TRUE)

btu <- stats[c("source","BTU","wpo.BTU","wto.BTU")]

btu$wpo.d <- NA
btu$wpo.d <- abs( btu$BTU - btu$wpo.BTU ) / btu$BTU
pdf('btu-cdf-wpo.pdf')
plot(ecdf(btu$wpo.d))
dev.off()

btu$wto.d <- NA
btu$wto.d <- abs( btu$BTU - btu$wto.BTU ) / btu$BTU

pdf('btu-cdf-wto.pdf')
plot(ecdf(btu$wto.d))
dev.off()

pdf('btu-cdf-wpo-openstack.pdf')
plot(ecdf(btu[grep("openstack",btu$source),]$wto.d))
dev.off()

pdf('btu-cdf-wpo-bonfire.pdf')
plot(ecdf(btu[grep("bonfire",btu$source),]$wto.d))
dev.off()

pdf('btu-cdf-wpo-montage.pdf')
plot(ecdf(btu[grep("pleiade",btu$source),]$wto.d))
dev.off()

pdf('btu-cdf-wpo-omssa.pdf')
plot(ecdf(btu[grep("pleiade",btu$source,invert=TRUE),]$wto.d))
dev.off()


write.table(btu[ with(btu, order(wpo.d)), ], file="btu.dat")


makespan <- stats[c("source","makespan","wpo.makespan","wto.makespan")]

makespan$wpo.d <- NA
makespan$wpo.d <- abs( makespan$makespan - makespan$wpo.makespan ) / makespan$makespan
pdf('makespan-cdf-wpo.pdf')
plot(ecdf(makespan$wpo.d))
dev.off()

makespan$wto.d <- NA
makespan$wto.d <- abs( makespan$makespan - makespan$wto.makespan ) / makespan$makespan

pdf('makespan-cdf-wto.pdf')
plot(ecdf(makespan$wto.d))
dev.off()

pdf('makespan-cdf-wpo-openstack.pdf')
plot(ecdf(makespan[grep("openstack",makespan$source),]$wto.d))
dev.off()

pdf('makespan-cdf-wpo-bonfire.pdf')
plot(ecdf(makespan[grep("bonfire",makespan$source),]$wto.d))
dev.off()

pdf('makespan-cdf-wpo-montage.pdf')
plot(ecdf(makespan[grep("pleiade",makespan$source),]$wto.d))
dev.off()

pdf('makespan-cdf-wpo-omssa.pdf')
plot(ecdf(makespan[grep("pleiade",makespan$source,invert=TRUE),]$wto.d))
dev.off()


write.table(makespan[ with(makespan, order(wpo.d)), ], file="makespan.dat")


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
