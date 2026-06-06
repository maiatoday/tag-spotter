package net.maiatoday.tagspotter.utils

import net.maiatoday.tagspotter.data.SpotDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KmlExporter {
    fun generateKml(spots: List<SpotDetails>): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        sb.append("  <Document>\n")
        sb.append("    <name>Tag Spotter Export</name>\n")
        sb.append("    <description>Exported spots from Tag Spotter app</description>\n")

        for (spotDetails in spots) {
            val spot = spotDetails.spot
            val name = spot.description.ifEmpty { "Spot #${spot.id}" }

            val detailsSb = StringBuilder()
            detailsSb.append("Category: ${spot.category.replace("_", " ").uppercase()}\n")
            if (spot.artists.isNotEmpty()) {
                detailsSb.append("Artist(s): ${spot.artists.joinToString(", ")}\n")
            }
            if (spot.photographer.isNotEmpty()) {
                detailsSb.append("Photographer: ${spot.photographer}\n")
            }
            detailsSb.append("Logged Date: ${dateFormat.format(Date(spot.createdAt))}\n")

            if (spotDetails.notes.isNotEmpty()) {
                detailsSb.append("\nNotes:\n")
                spotDetails.notes.forEach { note ->
                    detailsSb.append("- ${note.noteText}\n")
                }
            }
            val description = detailsSb.toString().trim()

            sb.append("    <Placemark>\n")
            sb.append("      <name>${escapeXml(name)}</name>\n")
            sb.append("      <description><![CDATA[$description]]></description>\n")
            sb.append("      <Point>\n")
            // Note: KML format specifies longitude first, then latitude
            sb.append("        <coordinates>${spot.longitude},${spot.latitude},0</coordinates>\n")
            sb.append("      </Point>\n")
            sb.append("    </Placemark>\n")
        }

        sb.append("  </Document>\n")
        sb.append("</kml>")
        return sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
