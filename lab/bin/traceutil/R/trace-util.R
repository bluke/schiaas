
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


#' Return the integrate of the given df over time
#' for each entity, or for all entities according to per_entity
#' Works  with numeric events and count-if traces.
#'
#' @param df a dataframe having date and value columns
#' @param per_entity TRUE to return the integral per entity
#' @return a dataframe having entity and value column
#' @keywords traceutil
#' @export
#' @examples
#' tu_integrate(balancer.used_core)
#' [1] 402836.8
tu_integrate <- function(df, per_entity=FALSE) {
	res <- unique(df['entity'])
	res$integrate = 0
	for(i in 1:nrow(res)) {
		vals <- df[df$entity == res[i,1],]
		res[i,2] = sum(head(vals$value,-1) * (tail(vals$date,-1) - head(vals$date,-1)))
	}
	if (per_entity) return(res)
	else return(sum(res$integrate))
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
		function(x) FUN(get(paste(x,'.',obs,sep='')))
		))
	return(res)
}