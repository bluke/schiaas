
library(traceutil)


pdf('data.pdf')
dfs <- tu_read('.', plotting=TRUE, plotting_state=FALSE)
dev.off()

###################################### specific

pdf('busy_hosts-integral-fast.pdf')
uc <-tu_apply(xps,'used_cores_ne_0',tu_integrate)
par(mar=c(15,5,1,1))
barplot(uc$value, names.arg=uc$xp, las=2)
print(uc)
dev.off()
