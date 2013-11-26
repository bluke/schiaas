#
# Plot the number of concurrent job
#
set terminal postscript enhanced colour size 13,6.5
set size 1.02,1
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

set style line 1 linewidth 3 lt 1 lc rgb "red"
set style line 2 linewidth 3 lt 2 lc rgb "red"
set style line 3 linewidth 1 lt 1 lc rgb "green"
set style line 4 linewidth 1 lt 2 lc rgb "green"
set style line 5 linewidth 1 lt 1 lc rgb "blue"
set style line 6 linewidth 1 lt 2 lc rgb "blue"
set style increment userstyle

set title "Concurrent jobs and VMs"
plot \
	"schlouder-jobs.dat" u 1:2 w steps ti "Concurrent jobs in Schlouder", \
	"schlouder-vms.dat" u 1:2 w steps ti "Concurrent VMs (when booted)", \
	"simschlouder-realduration-jobs.dat" u 1:2 w steps ti "Concurrent jobs in SimSchlouder (real duration)", \
	"simschlouder-realduration-vms.dat" u 1:2 w steps ti "Concurrent VMs (when booted)", \
	"simschlouder-predictiononly-jobs.dat" u 1:2 w steps ti "Concurrent jobs in SimSchlouder (prediction only)", \
	"simschlouder-predictiononly-vms.dat" u 1:2 w steps ti "Concurrent VMs (when booted)"

#pause -1 "Appuyez sur RETURN pour continuer"
