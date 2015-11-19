
source('reads.R')

pdf('data.pdf')
source('plots.R')
dev.off()

library(traceutil)

pdf('busy_hosts-integral.pdf')
uc <-tu_apply(xps,'used_cores_eq_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp)
uc
dev.off()
