@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package net.maiatoday.tagspotter.feature.detail.res

private fun formatJsDate(timestamp: Double, pattern: String): String = js("""
    (function(ts, pat) {
        var d = new Date(ts);
        var monthsShort = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        var monthsFull = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
        
        var hours = d.getHours();
        var min = d.getMinutes();
        var minutes = min < 10 ? '0' + min : '' + min;
        var ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12;
        var hoursStr = hours < 10 ? '0' + hours : '' + hours;
        
        var day = d.getDate();
        var dayStr = day < 10 ? '0' + day : '' + day;
        
        var res = pat;
        if (res.indexOf("MMMM") !== -1) {
            res = res.replace("MMMM", monthsFull[d.getMonth()]);
        } else if (res.indexOf("MMM") !== -1) {
            res = res.replace("MMM", monthsShort[d.getMonth()]);
        } else if (res.indexOf("MM") !== -1) {
            var m = d.getMonth() + 1;
            var mStr = m < 10 ? '0' + m : '' + m;
            res = res.replace("MM", mStr);
        }
        res = res.replace("dd", dayStr);
        res = res.replace("yyyy", "" + d.getFullYear());
        res = res.replace("hh", hoursStr);
        res = res.replace("mm", minutes);
        res = res.replace("a", ampm);
        return res;
    })(timestamp, pattern)
""")

actual object DateFormatter {
    actual fun formatDate(timestamp: Long, pattern: String): String {
        return formatJsDate(timestamp.toDouble(), pattern)
    }
}
