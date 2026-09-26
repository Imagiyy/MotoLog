package com.abrar.motolog.data.io

import com.abrar.motolog.shared.domain.model.GpsPoint
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.time.Instant
import javax.xml.parsers.SAXParserFactory

/**
 * Hardened GPX 1.1 / 1.0 streaming SAX parser.
 *
 * Security:
 * - Disables DTD and external entities (XXE defense)
 * - Maximum point limit (100,000 points) to prevent memory exhaustion
 * - Robust error handling that isolates failures per track
 */
object GpxImporter {

    const val MAX_POINTS_LIMIT: Int = 100_000

    data class ParsedTrack(
        val name: String,
        val points: List<GpsPoint>,
        val hasTimestamps: Boolean,
        val hasElevation: Boolean
    )

    data class ParseResult(
        val tracks: List<ParsedTrack>,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList()
    )

    /**
     * Parses a GPX stream into a list of tracks.
     */
    fun parse(inputStream: InputStream): ParseResult {
        val handler = GpxSaxHandler(MAX_POINTS_LIMIT)
        try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = true
                try { setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Exception) {}
                try { setFeature("http://xml.org/sax/features/external-parameter-entities", false) } catch (_: Exception) {}
                try { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) } catch (_: Exception) {}
            }
            val parser = factory.newSAXParser()
            parser.parse(inputStream, handler)
        } catch (e: Exception) {
            handler.errors.add("Failed to parse GPX: ${e.localizedMessage ?: e.message}")
        }

        if (handler.tracks.isEmpty() && handler.errors.isEmpty()) {
            handler.errors.add("No GPS track points found in file.")
        }

        return ParseResult(handler.tracks, handler.warnings, handler.errors)
    }

    private class GpxSaxHandler(private val maxPoints: Int) : DefaultHandler() {
        val tracks = mutableListOf<ParsedTrack>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        private var currentTrackName = "Imported Ride"
        private var currentPoints = mutableListOf<GpsPoint>()
        private var totalPointCount = 0
        private var inTrk = false
        private var inTrkSeg = false
        private val textBuffer = StringBuilder()

        private var ptLat: Double? = null
        private var ptLon: Double? = null
        private var ptEle: Double? = null
        private var ptTimeMs: Long? = null
        private var ptSpeed: Double? = null
        private var ptAccuracy: Float? = null

        private var trackHasTimestamps = false
        private var trackHasElevation = false

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            val tag = (if (!localName.isNullOrBlank()) localName else qName?.substringAfter(':')).orEmpty().lowercase()
            textBuffer.setLength(0)

            when (tag) {
                "trk" -> {
                    inTrk = true
                    currentTrackName = "Imported Ride"
                    currentPoints = mutableListOf()
                    trackHasTimestamps = false
                    trackHasElevation = false
                }
                "trkseg" -> inTrkSeg = true
                "trkpt" -> {
                    var latStr: String? = null
                    var lonStr: String? = null
                    if (attributes != null) {
                        for (i in 0 until attributes.length) {
                            val attr = attributes.getLocalName(i).ifBlank { attributes.getQName(i).substringAfter(':') }.lowercase()
                            if (attr == "lat") latStr = attributes.getValue(i)
                            if (attr == "lon") lonStr = attributes.getValue(i)
                        }
                    }
                    ptLat = latStr?.toDoubleOrNull()
                    ptLon = lonStr?.toDoubleOrNull()
                    ptEle = null
                    ptTimeMs = null
                    ptSpeed = null
                    ptAccuracy = null
                }
            }
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (ch != null && length > 0) {
                textBuffer.append(ch, start, length)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val tag = (if (!localName.isNullOrBlank()) localName else qName?.substringAfter(':')).orEmpty().lowercase()
            val text = textBuffer.toString().trim()

            when (tag) {
                "name" -> {
                    if (inTrk && !inTrkSeg && text.isNotEmpty()) {
                        currentTrackName = text
                    }
                }
                "ele" -> {
                    ptEle = text.toDoubleOrNull()
                    if (ptEle != null) trackHasElevation = true
                }
                "time" -> {
                    try {
                        val instant = Instant.parse(text)
                        ptTimeMs = instant.toEpochMilli()
                        trackHasTimestamps = true
                    } catch (_: Exception) {}
                }
                "speed" -> ptSpeed = text.toDoubleOrNull()
                "accuracy" -> ptAccuracy = text.toFloatOrNull()
                "trkpt" -> {
                    if (ptLat != null && ptLon != null) {
                        if (totalPointCount < maxPoints) {
                            currentPoints.add(
                                GpsPoint(
                                    latitude = ptLat!!,
                                    longitude = ptLon!!,
                                    altitudeMeters = ptEle,
                                    speedMps = ptSpeed?.toFloat(),
                                    accuracyMeters = ptAccuracy ?: 5.0f,
                                    timestampEpochMs = ptTimeMs ?: (currentPoints.size * 1000L),
                                    isPaused = false,
                                    isGap = false
                                )
                            )
                            totalPointCount++
                        } else if (totalPointCount == maxPoints) {
                            warnings.add("Maximum track point limit ($maxPoints) reached. Additional points were omitted.")
                            totalPointCount++
                        }
                    }
                }
                "trkseg" -> inTrkSeg = false
                "trk" -> {
                    inTrk = false
                    if (currentPoints.isNotEmpty()) {
                        tracks.add(
                            ParsedTrack(
                                name = currentTrackName,
                                points = currentPoints.toList(),
                                hasTimestamps = trackHasTimestamps,
                                hasElevation = trackHasElevation
                            )
                        )
                    }
                }
            }
        }
    }
}
