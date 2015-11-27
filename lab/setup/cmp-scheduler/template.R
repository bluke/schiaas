
library(traceutil)


pdf('data.pdf')
dfs <- tu_read('.', plotting=TRUE)
dev.off()

###################################### specific

pdf('busy_hosts-integral-fast.pdf', width=700)
uc <-tu_apply(xps,'used_cores_ne_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp, las=2)
print(uc)
dev.off()
