
#' Bad bad function, used only to debug things
#' @export
tu_reload <- function(dir='./bin') {
	devtools::document(paste(dir,'traceutil', sep='/'))
	devtools::install(paste(dir,'traceutil', sep='/'))
	library(traceutil)
}

#' Reads all the dat file in a given directory.
#' Create dataframe for each.
#' Plot according to plotting.
#' And create xps with the list of xp.
#'
#' @param dir the directory containing the dat files
#' @param plotting plot the data if TRUE
#' @return the list of dat files
#' @keywords traceutil
#' @export
#' @examples
#' tu_read('./data', TRUE)
#' will read all dit files inf the data directory and plot everything
tu_read <- function(dir = '.', plotting=FALSE) { 
	library(ggplot2)
	call_dir <- getwd()
	setwd(dir)
	datfile <- list.files('.',pattern='*.dat$')
	for (f in datfile) {
		print(f)
		df <- assign(sub('.dat$','',f), read.table(f,sep="", header=TRUE), envir = .GlobalEnv)

		if ( plotting ) tu_plot(df, f)
	}

	xps <<- data.frame(xp=unique(sub('\\..*$','',datfile)))

	setwd(call_dir)
	return(datfile)
}


#' Plot the given dataframe according to its type 
#' 
#' @param df a dataframe ['entity', 'date', 'value'] 
#' @return a plot 
#' @keywords traceutil
#' @export
#' @examples
#' tu_plot(balancer.vm__state)
tu_plot <- function(df, title=NA) {
	if ( colnames(df)[2] == "key" ) return();

	if (is.numeric(df$value)) {
		if (length(unique(df$entity)) == 1)	{
			type <- 's'
		} else {
			type <- 'p'
		}
		plot(df$date,df$value, type=type, main=title, xlab="date", ylab="value")
	} else {
		tu_plot_state(df, title)
	}
}


#' Plot the state of entities as colored rectangles.
#' 
#' @param df a dataframe ['entity', 'date', 'value'] where value is a state
#' @return a plot 
#' @keywords traceutil
#' @export
#' @examples
#' tu_plot_state(balancer.vm__state)
tu_plot_state <- function(df, title=NA) {
	colors <- data.frame(color=c("red", "green", "blue", "black", "orange", "purple", "coral", "seagreen", "gold" ))

	intervals <- tu_intervals(df)

	states <- data.frame(value=unique(intervals$value))
	states <- cbind(states,(head(colors,nrow(states))))
	
	entities <- data.frame(entity=unique(intervals$entity))
	entities$index <- seq_len(nrow(entities))

	fdf <- merge(merge(intervals,states),entities)

	print(ggplot(fdf) + expand_limits(x=-100) +
	geom_rect(aes(xmin=start_date,xmax=end_date,ymin=index,ymax=index+0.7,fill=color)) +
	geom_text(data=entities, aes(x=0, y=index+0.2, hjust=1, vjust=0, label=entity), size=3) +
	scale_fill_discrete(name="State", breaks=states$color, labels=states$value) +
	ggtitle(title) + xlab("date") + ylab("entity"))
}




#' Returns the value of a df at a given date.
#' Works  with events and count-if traces.
#'
#' @param df a dataframe having date and value columns
#' @param date a date in the simulation, in second
#' @return a dataframe having entity and value column
#' @keywords traceutil
#' @export
#' @examples
#' tu_valueat(balancer.used_core, 300)
#' 1  root:cloud:myCloud:compute:compute_host:node-2.me:used_cores     5
#' 2  root:cloud:myCloud:compute:compute_host:node-1.me:used_cores     7
#' 3  root:cloud:myCloud:compute:compute_host:node-3.me:used_cores     8

tu_valueat <- function(df,date) { 
	res <- unique(df['entity'])
	res$value = 0
	for(i in 1:nrow(res)) {
		res[i,2] = tail(df[df$entity == res[i,1] & df$date<=date,],1)$value
	}
	return(res)
}


#' Compute the intervals of a given df.
#' 
#' @param df a dataframe ['entity', 'date', 'value']
#' @return a dataframe ['entity', 'start_date', 'value', 'end_date', 'duration']
#' @keywords traceutil
#' @export
#' @examples
#' head(tu_intervals(balancer.used_cores))
#'                                                            entity start_date
#' 1   root:cloud:myCloud:compute:compute_host:node-20.me:used_cores     0.0000
#' 38  root:cloud:myCloud:compute:compute_host:node-20.me:used_cores    27.0000
#' 63  root:cloud:myCloud:compute:compute_host:node-20.me:used_cores    72.0000
#' 87  root:cloud:myCloud:compute:compute_host:node-20.me:used_cores   123.0000
#' 111 root:cloud:myCloud:compute:compute_host:node-20.me:used_cores   204.0000
#' 130 root:cloud:myCloud:compute:compute_host:node-20.me:used_cores   348.0078
#'     value end_date duration
#' 1       4  27.0000  27.0000
#' 38      5  72.0000  45.0000
#' 63      6 123.0000  51.0000
#' 87      7 204.0000  81.0000
#' 111     8 348.0078 144.0078
#' 130     7 378.0078  30.0000

tu_intervals <- function(df) {
	entities <- unique(df['entity'])
	res <- NULL 
	for(i in 1:nrow(entities)) {
		edf <- df[df$entity == entities[i,1],]
		eres <- head(edf,-1)
		colnames(eres)[2] <- "start_date"
		eres$end_date <- tail(edf,-1)$date
		eres$duration <- eres$end_date - eres$start_date

		res <- rbind(res, eres)
	}
	return(res)
}




#' Return the integrate of the given df over time
#' for each entity, or for all entities according to per_entity
#' Works  with numeric events and count-if traces.
#'
#' @param df a dataframe ['entity', 'date', 'value']
#' @param per_entity TRUE to return the integral per entity
#' @return a dataframe ['entity', 'integral'] of just integral according to per_entity
#' @keywords traceutil
#' @export
#' @examples
#' tu_integrate(balancer.used_core)
#' [1] 402836.8
tu_integrate <- function(df, per_entity=FALSE) {
	res <- unique(df['entity'])
	res$integral = 0
	for(i in 1:nrow(res)) {
		vals <- df[df$entity == res[i,1],]
		res[i,2] = sum(head(vals$value,-1) * (tail(vals$date,-1) - head(vals$date,-1)))
	}
	if (per_entity) return(res)
	else return(sum(res$integral))
}

#' Apply the FUN function to the observation obs for each xp in xps
#'
#' @param df a dataframe having date and value columns
#' @param per_entity TRUE to return the integral per entity
#' @return a dataframe having xp and value column
#' @keywords traceutil
#' @export
#' @examples
#' Given xps=data.frame(xp=c('balancer','consolidator')
#' tu_apply(xps, 'used_cores', tu_integrate) will return
#'                  xp    value
#' 1          balancer 402836.8
#' 2      consolidator 402836.8
#' Note that xps can be loaded with source('data/reads.R')
tu_apply <- function(xps, obs, FUN) {
	res <- cbind(xps, 'value'=apply(
		xps['xp'],1,
		function(x) FUN(get(paste(x,obs,sep='.')))
		))
	return(res)
}

