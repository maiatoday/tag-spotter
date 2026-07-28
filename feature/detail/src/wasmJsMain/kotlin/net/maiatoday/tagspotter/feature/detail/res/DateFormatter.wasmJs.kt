@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package net.maiatoday.tagspotter.feature.detail.res

private fun formatJsDate(timestamp: Double, pattern: String): String = js("""
    (function(ts, pat) {
        var d = new Date(ts);
        if (isNaN(d.getTime())) return "";
        var monthsShort = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        var monthsFull = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
        
        var hours = d.getHours();
        var min = d.getMinutes();
        var minutes = min < 10 ? '0' + min : '' + min;
        var ampm = hours >= 12 ? 'PM' : 'AM';
        var h12 = hours % 12;
        h12 = h12 ? h12 : 12;
        var hoursStr = h12 < 10 ? '0' + h12 : '' + h12;
        
        var day = d.getDate();
        var dayStr = day < 10 ? '0' + day : '' + day;
        var monthNum = d.getMonth() + 1;
        var monthStr = monthNum < 10 ? '0' + monthNum : '' + monthNum;

        return pat.replace(/MMMM|MMM|MM|dd|yyyy|hh|mm|\ba\b|a/g, function(match) {
            switch (match) {
                case 'MMMM': return monthsFull[d.getMonth()];
                case 'MMM': return monthsShort[d.getMonth()];
                case 'MM': return monthStr;
                case 'dd': return dayStr;
                case 'yyyy': return '' + d.getFullYear();
                case 'hh': return hoursStr;
                case 'mm': return minutes;
                case 'a': return ampm;
                default: return match;
            }
        });
    })(timestamp, pattern)
""")

actual object DateFormatter {
    actual fun formatDate(timestamp: Long, pattern: String): String {
        return formatJsDate(timestamp.toDouble(), pattern)
    }
}
