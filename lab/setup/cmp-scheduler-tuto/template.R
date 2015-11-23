
library(traceutil)
dfs <- tu_read('.',FALSE)


##################################### all

for (x in dfs) {
	png(paste(x,'png', sep='.'))
	tu_plot(get(x), x)
	dev.off()
}

png('balancer_slowfast.instances_count.png')
plot(balancer_slowfast.instances_count$date,balancer_slowfast.instances_count$value, type='s', main=title, xlab="date", ylab="value")
dev.off()

###################################### specific

png('busy_hosts-integral-slow.png', width=700)
subxps <- data.frame(xp=xps[grep('fast', xps$xp, invert=TRUE),])
uc <-tu_apply(subxps,'used_cores_ne_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp, las=2)
print(uc)
dev.off()

png('busy_hosts-integral-fast.png', width=700)
subxps <- data.frame(xp=xps[grep('_fast', xps$xp),])
uc <-tu_apply(subxps,'used_cores_ne_0',tu_integrate)
barplot(uc$value, names.arg=uc$xp, las=2)
print(uc)
dev.off()
