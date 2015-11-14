
# return the value of data at date
tu_valueat <- function(data,date) { 
	tail(data[data$date<=date,],1)$value
}

# return the integrate of the given data
# for each entity, or for all entities according to per_entity
tu_integrate <- function(data, per_entity=FALSE) {
	res <- unique(data['entity'])
	res$integrate = 0
	for(i in 1:nrow(res)) {
		vals <- data[data$entity == res[i,1],]
		res[i,2] = sum(head(vals$value,-1) * (tail(vals$date,-1) - head(vals$date,-1)))
	}
	if (per_entity) return(res)
	else return(sum(res$integrate))
}

# apply the FUN function to the observation obs for each xp in xps
# For instance, given xps=data.frame('balancer','consolidator')
# tu_apply(xps, 'used_cores', tu_integrate) will return
#               xp                             value
# 1       balancer tu_integrate(balancer.used_cores) 
# 2 reconsolidator tu_integrate(balancer.used_cores)
tu_apply <- function(xps, obs, FUN) {
	res <- cbind(xps, 'value'=apply(
		xps['xp'],1,
		function(x) FUN(get(paste(x,'.',obs,sep='')))
		))
	return(res)
}