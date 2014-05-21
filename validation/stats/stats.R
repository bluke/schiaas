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

write.table(makespan[ with(makespan, order(wpo.d)), ], file="makespan.dat")


# OMSSA
stats <- read.table("stats-omssa.dat", header=TRUE)
btu <- stats[c("source","BTU","wpo.BTU","wto.BTU")]
btu$wpo.d <- NA
btu$wpo.d <- abs( btu$BTU - btu$wpo.BTU ) / btu$BTU
pdf('btu-omssa-cdf-wpo.pdf')
plot(ecdf(btu$wpo.d), main="BTU omssa wpo")
dev.off()

btu$wto.d <- NA
btu$wto.d <- abs( btu$BTU - btu$wto.BTU ) / btu$BTU
pdf('btu-omssa-cdf-wto.pdf')
plot(ecdf(btu$wto.d), main="BTU omssa wto")
dev.off()

makespan <- stats[c("source","makespan","wpo.makespan","wto.makespan")]
makespan$wpo.d <- NA
makespan$wpo.d <- abs( makespan$makespan - makespan$wpo.makespan ) / makespan$makespan
pdf('makespan-omssa-cdf-wpo.pdf')
plot(ecdf(makespan$wpo.d), main="Makespan omssa wpo")
dev.off()

makespan$wto.d <- NA
makespan$wto.d <- abs( makespan$makespan - makespan$wto.makespan ) / makespan$makespan
pdf('makespan-omssa-cdf-wto.pdf')
plot(ecdf(makespan$wto.d), main="Makespan omssa wto")
dev.off()

# Montage
stats <- read.table("stats-montage.dat", header=TRUE)
btu <- stats[c("source","BTU","wpo.BTU","wto.BTU")]
btu$wpo.d <- NA
btu$wpo.d <- abs( btu$BTU - btu$wpo.BTU ) / btu$BTU
pdf('btu-montage-cdf-wpo.pdf')
plot(ecdf(btu$wpo.d), main="BTU Montage wpo")
dev.off()

btu$wto.d <- NA
btu$wto.d <- abs( btu$BTU - btu$wto.BTU ) / btu$BTU
pdf('btu-montage-cdf-wto.pdf')
plot(ecdf(btu$wto.d), main="BTU Montage wto")
dev.off()

makespan <- stats[c("source","makespan","wpo.makespan","wto.makespan")]
makespan$wpo.d <- NA
makespan$wpo.d <- abs( makespan$makespan - makespan$wpo.makespan ) / makespan$makespan
pdf('makespan-montage-cdf-wpo.pdf')
plot(ecdf(makespan$wpo.d), main="Makespan Montage wpo")
dev.off()

makespan$wto.d <- NA
makespan$wto.d <- abs( makespan$makespan - makespan$wto.makespan ) / makespan$makespan
pdf('makespan-montage-cdf-wto.pdf')
plot(ecdf(makespan$wto.d), main="Makespan Montage wto")
dev.off()

