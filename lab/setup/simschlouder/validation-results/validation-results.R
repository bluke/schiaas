#/bin/R

library(ggplot2)
library(grid)
library(gridExtra)
library(scales)
library(xtable)

simulations <- c('best','thread','boottimes','communications','predictions')
applications <- c('omssa','montage')
platforms <- c('openstack-icps','fr-inria','uk-epcc','de-hlrs')
simulations <- c('best','thread','boottimes','communications','predictions')

colmerge.metrics <- c("xp_id","platform","storage","usecase","application","provisioning")
colmerge.jobs <- c(colmerge.metrics,"name")
colmerge.nodes <- c(colmerge.metrics,"index")

vr.read.colmerge <- function(type,serie) {

	if(type == 'nodes') {
	  file = 'nodes.dat'
	  cm = colmerge.nodes
	}
	else if(type == 'jobs') {
	  file = 'jobs.dat'
	  cm = colmerge.jobs
	}
	else {
	  file = 'metrics.dat'
	  cm = colmerge.metrics
	}

	df <- read.table(file, header=TRUE, na.strings=c("NA"))
	df <- merge( 
		df[df$trace_type == 'real_xp',],
		df[df$trace_type == serie,],
		by = cm)

	return(df)
}

vr.check.communications <- function(platforms = c("openstack-icps","fr-inria",'uk-epcc',"de-hlrs")) {

	jobs <<- read.table("jobs.dat", header=TRUE, na.strings=c("NA"))

	jobs.com <- na.omit(merge( 
		jobs[jobs$trace_type == 'real_xp',
			c('input_time','input_size','output_time','output_size',colmerge.jobs)],
		jobs[jobs$trace_type == 'sim_communications',
			c('input_time','input_size','output_time','output_size',colmerge.jobs)],
		by = colmerge.jobs))

	agg.in.lat <- aggregate(
		cbind(input_size.x, input_time.x, input_time.y) ~  storage + platform, 
		data = jobs.com[jobs.com$input_time.x > 0,], min)
	agg.in.lat$ae <- ( agg.in.lat$input_time.x - agg.in.lat$input_time.y) / agg.in.lat$input_time.y

	agg.in.bw <- aggregate( 
		cbind(input_size.x, input_time.x, input_time.y) ~  storage + platform, 
		data = jobs.com[jobs.com$input_size.x > 1500,], max)
	agg.in.bw$bw.x <- agg.in.bw$input_size.x / agg.in.bw$input_time.x
	agg.in.bw$bw.y <- agg.in.bw$input_size.x / agg.in.bw$input_time.y
	agg.in.bw$ae <- ( agg.in.bw$bw.x - agg.in.bw$bw.y ) / agg.in.bw$bw.y

	agg.out.lat <- aggregate( 
		cbind(output_size.x, output_time.x, output_time.y) ~  storage + platform, 
		data = jobs.com[jobs.com$output_time.x > 0,], min)
	agg.out.lat$ae <- ( agg.out.lat$output_time.x - agg.out.lat$output_time.y) / agg.out.lat$output_time.y

	agg.out.bw <- aggregate( 
		cbind(output_size.x, output_time.x, output_time.y) ~  storage + platform, 
		data = jobs.com[jobs.com$output_size.x > 1500,], max)
	agg.out.bw$bw.x <- agg.out.bw$output_size.x / agg.out.bw$output_time.x
	agg.out.bw$bw.y <- agg.out.bw$output_size.x / agg.out.bw$output_time.y
	agg.out.bw$ae <- ( agg.out.bw$bw.x - agg.out.bw$bw.y ) / agg.out.bw$bw.y

	print("in/up")
	print(agg.in.lat)
	print("out/down")
	print(agg.out.lat)
	print("in/up")
	print(agg.in.bw[,c('storage','platform','bw.x','bw.y','ae')])
	print("out/down")
	print(agg.out.bw[,c('storage','platform','bw.x','bw.y','ae')])
}

vr.plot.communications <- function(platforms = c("openstack-icps","fr-inria",'uk-epcc',"de-hlrs"))
{
	jobs <<- read.table("jobs.dat", header=TRUE, na.strings=c("NA"))

	jobs.com <- na.omit(merge( 
		jobs[jobs$trace_type == 'real_xp' & jobs$platform %in% platforms,
			c('input_time','input_size','output_time','output_size',colmerge.jobs)],
		jobs[jobs$trace_type == 'sim_communications'  & jobs$platform %in% platforms,
			c('input_time','input_size','output_time','output_size',colmerge.jobs)],
		by = colmerge.jobs))

	plots <- list()
	for(platform in platforms) {
		for(storage in unique(jobs.com$storage)) {
			j <- jobs.com[jobs.com$platform==platform & jobs.com$storage==storage,]
			if (nrow(j) != 0) {
				p <- ggplot(j) +
					geom_smooth(aes(x=input_size.x, y=input_time.x),method = "lm", se=FALSE, color="blue", formula = y ~ x) +
        			geom_point(aes(x=input_size.x, y=input_time.x), col='blue') +
					geom_smooth(aes(x=input_size.y, y=input_time.y),method = "lm", se=FALSE, color="red", formula = y ~ x) +
        			geom_point(aes(x=input_size.y, y=input_time.y), col='red') +
	
					ggtitle(paste(platform,storage)) 
				plots[[length(plots)+1]] <- p

				p <- ggplot(j) +
					geom_smooth(aes(x=output_size.x, y=output_time.x),method = "lm", se=FALSE, color="blue", formula = y ~ x) +
        			geom_point(aes(x=output_size.x, y=output_time.x), col='blue') +
					geom_smooth(aes(x=output_size.y, y=output_time.y),method = "lm", se=FALSE, color="red", formula = y ~ x) +
        			geom_point(aes(x=output_size.y, y=output_time.y), col='red') +
	
					ggtitle(paste(platform,storage)) 
				plots[[length(plots)+1]] <- p


				#lm(j$input_size.x ~ j$input_time.x)
				#lm(j$output_size.x ~ j$output_time.x)
				#lm(j$input_size.y ~ j$input_time.y)
				#lm(j$output_size.y ~ j$output_time.y)
			}
		}
		
	}
	do.call("grid.arrange", c(plots, ncol=2))
}

vr.check.walltimes <- function() {
	
	jobs.best <- vr.read.colmerge('jobs', 'sim_best')
	
	jobs.best$walltime.ae = ( jobs.best$walltime.x - jobs.best$walltime.y ) / jobs.best$walltime.y

	for(platform in platforms) {
		j <- jobs.best[jobs.best$platform==platform & is.finite(jobs.best$walltime.ae),]

		print(platform)
		print(min(j$walltime.ae))
		print(mean(j$walltime.ae))
		print(max(j$walltime.ae))
	}
}


vr.check.management <- function(serie = 'sim_no-threads') {
	
	jobs.serie <- vr.read.colmerge('jobs', serie)
	jobs.serie <- jobs.serie[jobs.serie$management_time.x != 0,]
	jobs.serie$management_time.d <- jobs.serie$management_time.y - jobs.serie$management_time.x


	print(aggregate(
		cbind(management_time.x, management_time.d) ~ platform + storage,
		data = jobs.serie, 
		FUN = function(x) c(mean=round(mean(x),3),min=round(min(x),3),max=round(max(x),3))))

	print(aggregate(
		cbind(management_time.x, management_time.y, as.character(name), as.character(xp_id)) ~ platform + storage,
		data = jobs.serie[order(abs(jobs.serie$management_time.d)),], 
		FUN = function(x) tail(x,1)))
}


vr.metrics.all <- function(serie = 'sim_best', outfile = NA) {
	metrics <- vr.read.colmerge('metrics',serie)

	#add scheduling error
	jobs <<- vr.read.colmerge('jobs',serie)

	jobs$schederror <<- ifelse(jobs$node_index.x != jobs$node_index.y,1,0)
	jobs$jobs_count <- 1
	agg.xp.schederror <- aggregate(cbind(schederror,jobs_count) ~ xp_id + trace_type.y, data = jobs, sum)
	agg.xp.schederror$schederror.ae <- agg.xp.schederror$schederror / agg.xp.schederror$jobs_count

	agg.xp.firstschederror <- aggregate( cbind(as.character(name) ) ~ xp_id, 
		data = jobs[jobs$schederror != 0 ,c("xp_id","name")],
		function(x){return(head(x,n=1))})
	colnames(agg.xp.firstschederror)[2] <- "first_schederror"
	agg.xp <- merge(agg.xp.schederror,agg.xp.firstschederror, all.x = TRUE)

	metrics <- merge(metrics,agg.xp)

	#metrics
	metrics$makespan.ae <- abs(metrics$makespan.x - metrics$makespan.y) / metrics$makespan.x
	metrics$uptime.ae <- abs(metrics$uptime.x - metrics$uptime.y) / metrics$uptime.x
	metrics$usage.ae <- abs(metrics$usage.x - metrics$usage.y) / metrics$usage.x

	if(!is.na(outfile)) {
		write.table(metrics,outfile,sep='\t')
	}

	return(metrics)
}


vr.binwidth <<- 0.05

vr.freq <- function(d,name="m") {
	f <- data.frame(table(cut(d, seq(0,1,vr.binwidth), right=FALSE) ))
	f$prob <- round(f$Freq / sum(f$Freq),2)

	colnames(f)[2] <- paste(name,'f',sep='_')
	colnames(f)[3] <- paste(name,'p',sep='_')

	return(f)
}

vr.mmmm <- function(x,name="m") {
	m <- data.frame(name=c(min(x),mean(x),median(x),max(x)))
	rownames(m) <- c('min','mean','median','max')
	colnames(m)[1] <- name

	return(m)
}

vr.add.sign <- function(df) {
	df1 <- sapply(data.frame(df), FUN = function(x) { 
		ifelse(x>0, sprintf("$+%0.3f$",x),
			ifelse(x<0, sprintf("$%0.3f$",x),
				sprintf("$\\phantom{+}%0.3f$",x)))
		} )

	colnames(df1) <- colnames(df)
	rownames(df1) <- rownames(df)

	return(df1)
}

vr.mmmm.cmp <- function(mmmm1,mmmm2) {

	d <- mmmm1-mmmm2
	d <- vr.add.sign(d)
	
	mc <- data.frame(mmmm1,d)[,c(1,5,2,6,3,7,4,8)]

	for(ic in seq(1,ncol(mc),2)) {
		colnames(mc)[ic+1] <- paste('$\\delta_{',colnames(mc)[ic],'}$',sep='')
	}

	return(mc)
}

vr.mmmm.cmp.series <- function(metrics.1,metrics.2, outfile = NA) {
	metrics <- c('uptime.ae', 'makespan.ae','usage.ae', 'schederror.ae')
	df1 <- metrics.1[, c('xp_id','first_schederror',metrics)]
	df2 <- metrics.2[, c('xp_id','first_schederror',metrics)]

	df <- merge(df1,df2,by='xp_id')
	df$uptime.ae 	<- df$uptime.ae.y - df$uptime.ae.x
	df$makespan.ae 	<- df$makespan.ae.y - df$makespan.ae.x
	df$usage.ae 	<- df$usage.ae.y - df$usage.ae.x
	df$schederror.ae <- df$schederror.ae.y - df$schederror.ae.x

	if(!is.na(outfile)) {
		write.table(
			df[order(df$makespan.ae),],
			outfile, sep='\t')
	}

	m <- sapply(df[,metrics], function(x) c(min(x),mean(x),median(x),max(x)))

	rownames(m) <- c('min','mean','median','max')
	colnames(m) <- paste('$\\Delta_{',colnames(m),'}$',sep='')

	return(m)
}


vr.write.freqs.mmmm <- function(prefix, freqs=NA, mmmm=NA, mmmm.cmp=NA, twobytwo=NA) {

	if(!is.na(freqs)) {
		write.table(
			freqs,paste(prefix,'freqs.dat',sep='-'),
			sep='\t',row.names=FALSE)
	}
	if(!is.na(mmmm)) {
		print.xtable(
			xtable(mmmm, digits=3),
			file = paste(prefix,'mmmm.latex',sep='-'),
			floating=FALSE)
	}
	if (!is.na(mmmm.cmp)) {
		cmp <- vr.mmmm.cmp(mmmm,mmmm.cmp)
		print.xtable(
			xtable(cmp, digits=3),
			#latex.environments = "center",
			#size = "\\tiny",
			rotate.colnames = TRUE,
			file = paste(prefix,'mmmm-cmp.latex',sep='-'),
			sanitize.text.function = identity,
			floating=FALSE)
	}
	if(!is.na(twobytwo)) {
		print.xtable(
			xtable(vr.add.sign(twobytwo), digits=3),
			file = paste(prefix,'mmmm-twobytwo.latex',sep='-'),
			sanitize.text.function = identity,
			floating=FALSE)
	}

}

vr.colors <- list('schederror.ae'='red', 'makespan.ae'='blue', 'uptime.ae'='green', 'usage.ae'='orange')


vr.article.umus <- function(df,xl=1,yl=1,merged=FALSE,compare=NA) {

	plots <- list()	
	plot <- ggplot(df) + xlim(0,xl) + ylim(0,yl) + ylab('ratio of xp') + xlab(merged)
	
	if (exists('freqs')) { rm(freqs) }
	if (exists('mmmm')) { rm(mmmm) }

	for(metric in c('uptime.ae', 'makespan.ae','usage.ae', 'schederror.ae')) {
		if (merged == FALSE) {
			p <- ggplot(df,aes_string(x=metric)) + 
				geom_histogram(aes(y = (..count..)/sum(..count..)), binwidth=vr.binwidth) +
				geom_freqpoly(aes(y = (..count..)/sum(..count..)), binwidth=vr.binwidth, col=vr.colors[[metric]]) +
				xlim(0,xl) + ylim(0,yl) + ylab('ratio of xp')

			if (!is.na(compare)) {
				p <- p + geom_freqpoly(aes(y = (..count..)/sum(..count..)), 
					data = compare,
					binwidth=vr.binwidth, col='grey')
			}

			plots[[length(plots)+1]] <- p
		} else {
			plot <- plot +
				geom_freqpoly(
					aes_string(x=metric, y="(..count..)/sum(..count..)"), 
					binwidth=vr.binwidth, col=vr.colors[[metric]])
		}

		f <- vr.freq(df[[metric]],metric)
		m <- vr.mmmm(df[[metric]],metric)
		if(exists('freqs')) {
			freqs <- merge(freqs,f)
			mmmm <- cbind(mmmm,m)
		} else {
			freqs <- f
			mmmm <- m
		}

	}
	
	if (merged != FALSE) {
		plots[[length(plots)+1]] <- plot
	}
		
	return(list(plots=plots,freqs=freqs, mmmm=mmmm))
}



vr.article <- function() {

	metrics.best <- vr.metrics.all('sim_best','sim_best.metrics.dat')

	#best global
	l <- vr.article.umus(metrics.best)

	vr.write.freqs.mmmm('sim_best-4metrics',
		l[['freqs']],l[['mmmm']])
	mmmm.best <- l[['mmmm']]

	pdf('sim_best-4metrics.pdf')
	do.call("grid.arrange", c(l[['plots']], ncol=2))
	dev.off()

	#best platform/app
	plots <- list()	
	l <- vr.article.umus(metrics.best[
		metrics.best$platform == 'openstack-icps' &
		metrics.best$application == 'omssa',],
		merged = "openstack-icps / omssa")
	vr.write.freqs.mmmm('sim_best-openstack-icps-omssa-4metrics',
		l[['freqs']],l[['mmmm']])
	mmmm.openstack.omsssa <- l[['mmmm']]
	plots <- append(plots,l[['plots']])

	l <- vr.article.umus(metrics.best[
		metrics.best$platform == 'openstack-icps' &
		metrics.best$application == 'montage',],
		merged = "openstack-icps / montage")
	vr.write.freqs.mmmm('sim_best-openstack-icps-montage-4metrics',
		l[['freqs']],l[['mmmm']],mmmm.openstack.omsssa)
	plots <- append(plots,l[['plots']])

	l <- vr.article.umus(metrics.best[
		metrics.best$platform != 'openstack-icps' &
		metrics.best$application == 'omssa',],
		merged = "bonfire / omssa")
	vr.write.freqs.mmmm('sim_best-bonfire-omssa-4metrics',
		l[['freqs']],l[['mmmm']],mmmm.openstack.omsssa)
	plots <- append(plots,l[['plots']])

	l <- vr.article.umus(metrics.best[
		metrics.best$platform != 'openstack-icps' &
		metrics.best$application == 'montage',],
		merged = "bonfire / montage")
	vr.write.freqs.mmmm('sim_best-bonfire-montage-4metrics',
		l[['freqs']],l[['mmmm']],mmmm.openstack.omsssa)
	plots <- append(plots,l[['plots']])

	pdf('sim_best-4metrics-platform-app.pdf')
	do.call("grid.arrange", c(plots, ncol=2))
	dev.off()

	
	# openstack-icp / OMSSA / no schederror
	l <- vr.article.umus(metrics.best[
		metrics.best$platform == 'openstack-icps' &
		metrics.best$application == 'omssa' &
		metrics.best$schederror == 0,])
	vr.write.freqs.mmmm('sim_best-openstack-icps-omssa-noschederror-4metrics',
		l[['freqs']],l[['mmmm']],mmmm.openstack.omsssa)

	pdf('sim_best-openstack-icps-omssa-noschederror-4metrics.pdf')
	do.call("grid.arrange", c(l[['plots']], ncol=2))
	dev.off()


	# all sims
	for(sim in c('sim_no-boottimes','sim_no-threads','sim_communications','sim_predictions')) {
		
		metrics.sim <- vr.metrics.all(sim, paste(sim,'metrics.dat',sep='-'))
		mmmm.cmp <- vr.mmmm.cmp.series(metrics.best,metrics.sim, paste(sim,'4metrics-cmp.dat',sep='-'))

		l <- vr.article.umus(metrics.sim,compare=metrics.best)
		vr.write.freqs.mmmm(paste(sim,'4metrics',sep='-'),
			l[['freqs']],l[['mmmm']], mmmm.best, mmmm.cmp)

		pdf(paste(sim,'4metrics-cmp.pdf',sep='-'))
		do.call("grid.arrange", c(l[['plots']], ncol=2))
		dev.off()
	}

	pdf('communications-openstack-icps.pdf')
	vr.plot.communications(c('openstack-icps'))
	dev.off()

	pdf('communications-bonfire.pdf')
	vr.plot.communications(c('fr-inria','uk-epcc','de-hlrs'))
	dev.off()
}
