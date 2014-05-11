#
# Plot the number of concurrent job
#
set terminal postscript enhanced colour size 13,6.5
set size 0.8,1
#set output '| ps2pdf - diameter.pdf'
set output 'diameter.eps'
set autoscale

set xlabel "Time (s)"
set xtic auto
set xtic rotate by -45
set xtic offset 1

set ylabel "Diameter"
set ytic auto
set yrange [0:]



set xtics nomirror
set ytics nomirror

set style line 1 linewidth 3 lt 1 lc rgb "black"
set style line 2 linewidth 3 lt 2 lc rgb "black"
set style line 3 linewidth 1 lt 1 lc rgb "green"
set style line 4 linewidth 1 lt 2 lc rgb "green"
set style line 5 linewidth 1 lt 1 lc rgb "blue"
set style line 6 linewidth 1 lt 2 lc rgb "blue"
set style line 7 linewidth 1 lt 1 lc rgb "red"
set style line 8 linewidth 1 lt 2 lc rgb "red"
set style increment userstyle

set title "Concurrent jobs and VMs"
plot \
	"data/schlouder-jobs.dat" u 1:2 w steps ti "Concurrent jobs in Schlouder", \
	"data/schlouder-vms.dat" u 1:2 w steps ti "Concurrent VMs in Schlouder", \
	"data/simschlouder-none-jobs.dat" u 1:2 w steps ti "Concurrent jobs in SimSchlouder (walltime prediction only)", \
	"data/simschlouder-none-vms.dat" u 1:2 w steps ti "Concurrent VMs", \
	"data/simschlouder-psm-jobs.dat" u 1:2 w steps ti "Concurrent jobs in SimSchlouder (PSM data)", \
	"data/simschlouder-psm-vms.dat" u 1:2 w steps ti "Concurrent VMs", \
	"data/simschlouder-wto-jobs.dat" u 1:2 w steps ti "Concurrent jobs in SimSchlouder (real walltime)", \
	"data/simschlouder-wto-vms.dat" u 1:2 w steps ti "Concurrent VMs", \
	"data/simschlouder-rio-jobs.dat" u 1:2 w steps ti "Concurrent jobs in SimSchlouder (real RT, in, out)", \
	"data/simschlouder-rio-vms.dat" u 1:2 w steps ti "Concurrent VMs"

#pause -1 "Appuyez sur RETURN pour continuer"
