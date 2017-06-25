
library(ggplot2)
library(cowplot)

plasma = c("#0D0887FF","#7E03A8FF","#CC4678FF","#F89441FF","#F0F921FF")

fullset = read.table("montecarlo-cmp.metrics.dat",header=T)
btuset = read.table("btuset.dat",header=T)
realset = subset(fullset,trace_type=="real_xp")
simset = subset(fullset,trace_type!="real_xp")
rm(fullset)

p_mks = ggplot(simset,aes(x=makespan))+geom_density(data=subset(simset,provisioning=="afap"),fill="#F0F921FF")+geom_density(data=subset(simset,provisioning=="asap"),fill="#CC4678FF")+geom_density(data=subset(realset,provisioning=="afap"),color="#0D0887FF",size=1)+geom_density(data=subset(realset,provisioning=="asap"),color="#0D0887FF",size=1)+ggtitle("OMSSA")+xlab("makespan (s)")+scale_fill_manual(values=c("#0D0887FF","#F0F921FF","#CC4678FF"),guide=guide_legend(title="Strategy"),labels=c("real","afap","asap"))

p_btu = ggplot(simset,aes(x=BTU_count+0.2,y=(..count..)*100/sum(..count..)))+geom_bar(data=subset(simset,provisioning=="afap"),fill="#CC4678FF",width=0.4)+geom_bar(data=subset(simset,provisioning=="afap"),fill="#F0F921FF",width=0.4)+geom_bar(data=subset(btuset,provisioning=="asap"),aes(x=BTU_count-0.2),fill="#0D0887FF",width=0.4)+geom_bar(data=subset(btuset,provisioning=="afap"),aes(x=BTU_count-0.2),fill="#0D0887FF",width=0.4)+scale_fill_manual(values=c("#CC4678FF","#F0F921FF","#0D0887FF"),guide=guide_legend(title = "Strategy"),labels = c("afap","asap","real"))+ggtitle("OMSSA")+xlab("BTU")+ylab("% runs")

nrow(simset)

save_plot("out.pdf",p_btu)
#save_plot("out.pdf",plot_grid(p_mks,p_btu,ncol=1),base_aspect_ratio=1)
