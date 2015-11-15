
source('reads.R')

pdf('data.pdf')
source('plots.R')
dev.off()

install.packages('../bin/traceutil_0.0.0.9000.tar.gz')
library(traceutil)

pdf('busy_hosts-integral.pdf')
uc <-tu_apply(xps,'used_cores_ne_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp)
uc
dev.off()
