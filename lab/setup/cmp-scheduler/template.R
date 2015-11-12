
source('reads.R')

pdf('data.png')

source('plots.R')

dev.off()

#valueat <- function(v,d) { return(tail(d[d$date<=v,],1)$value) }

#va_migrations <- function(v) {
#	return(valueat(v, reconsolidator100.VirtualMachines__state_eq_migrating)[1])
#}

#integrate(va_migrations, 0, 10000)