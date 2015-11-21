
library(traceutil)
dfs <- tu_read('.',FALSE)


##################################### all

for (x in dfs) {
	png(paste(x,'png', sep='.'))
	tu_plot(get(x), x)
	dev.off()
}

###################################### specifi

for (inj in c('slow','fast','slowfast')) {
	png(paste('busy_hosts-integral',inj,'png',sep='.'), width=700)
	subxps <- data.frame(xp=xps[grep(paste('_',inj,'$',sep=''), xps$xp),])
	uc <-tu_apply(subxps,'used_cores_ne_0',tu_integrate)
	barplot(uc$value, names.arg=uc$xp, las=2)
	print(uc)
	dev.off()
}
