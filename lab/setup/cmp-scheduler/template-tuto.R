
library(traceutil)
dfs <- tu_read('.',FALSE)


##################################### all

for (x in dfs) {
	png(paste(x,'png', sep='.'))
	tu_plot(get(x), x)
	dev.off()
}

png('xp_balancer_slowfast.instances_count.png')
plot(xp_balancer_slowfast.instances_count$date,xp_balancer_slowfast.instances_count$value, type='s', main='xp_balancer_slowfast.instances_count', xlab="date", ylab="value")
dev.off()

###################################### specific

png('busy_hosts-integral-slow.png', width=700)
par(mar=c(12,5,1,1)) 
subxps <- data.frame(xp=c('xp_balancer_slow','xp_consolidator_slow','xp_reconsolidator0_slow','xp_reconsolidator10_slow','xp_reconsolidator100_slow'))
uc <-tu_apply(subxps,'used_cores_ne_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp, las=2)
print(uc)
dev.off()

png('busy_hosts-integral-fast.png', width=700)
par(mar=c(12,5,1,1)) 
subxps <- data.frame(xp=c('xp_balancer_fast','xp_consolidator_fast','xp_reconsolidator0_fast','xp_reconsolidator10_fast','xp_reconsolidator100_fast'))
uc <-tu_apply(subxps,'used_cores_ne_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp, las=2)
print(uc)
dev.off()
