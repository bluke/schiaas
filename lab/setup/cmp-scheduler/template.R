library(traceutil)

pdf('data.pdf')
tu_read('.',TRUE)
dev.off()

pdf('busy_hosts-integral.pdf')
uc <-tu_apply(xps,'used_cores_eq_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp)
uc
dev.off()
