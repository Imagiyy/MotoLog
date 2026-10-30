package com.abrar.motolog.data.io

import android.util.Log
import com.abrar.motolog.shared.domain.model.GpsPoint
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.time.Instant
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/**
 * Hardened GPX 1.1 / 1.0 streaming SAX parser.
 *
 * Security:
 * - Enables XMLConstants.FEATURE_SECURE_PROCESSING (Fix #9)
 * - Disables DTD and external entities (XXE defense)
 * - Validates coordinate finiteness and geographic bounds (Fix #6, #8)
 * - Maximum point limit per track (50,000) and global (100,000) to prevent memory exhaustion (Fix #19)
 * - Fallbacks to current time for tracks without timestamps to avoid 1970 dates (Fix #20)
 * - Robust error handling that isolates failures per track
 */
object GpxImporter {

    const val MAX_POINTS_PER_TRACK: Int = 50_000
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
        val handler = GpxSaxHandler(MAX_POINTS_LIMIT, MAX_POINTS_PER_TRACK)
        try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = true
                try {
                    setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                } catch (e: Exception) {
                    Log.w("GpxImporter", "FEATURE_SECURE_PROCESSING not supported: ${e.message}")
                }
                try { setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Exception) {}
                try { setFeature("http://xml.org/sax/features/external-parameter-entities", false) } catch (_: Exception) {}
                try { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) } catch (_: Exception) {}
                try { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (_: Exception) {}
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

    private class GpxSaxHandler(
        private val maxTotalPoints: Int,
        private val maxPointsPerTrack: Int
    ) : DefaultHandler() {
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
        private var trackFallbackStartTimeMs: Long = System.currentTimeMillis()
        private var trackLimitReachedWarned = false

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
                    trackFallbackStartTimeMs = System.currentTimeMillis()
                    trackLimitReachedWarned = false
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
                    val ele = text.toDoubleOrNull()
                    if (ele != null && !ele.isNaN() && !ele.isInfinite() && ele in -1000.0..15000.0) {
                        ptEle = ele
                        trackHasElevation = true
                    }
                }
                "time" -> {
                    try {
                        val instant = Instant.parse(text)
                        val ms = instant.toEpochMilli()
                        if (ms > 0L) {
                            ptTimeMs = ms
                            trackHasTimestamps = true
                        }
                    } catch (_: Exception) {}
                }
                "speed" -> {
                    val s = text.toDoubleOrNull()
                    if (s != null && !s.isNaN() && !s.isInfinite() && s >= 0.0) {
                        ptSpeed = s
                    }
                }
                "accuracy" -> {
                    val a = text.toFloatOrNull()
                    if (a != null && !a.isNaN() && !a.isInfinite() && a > 0.0f) {
                        ptAccuracy = a
                    }
                }
                "trkpt" -> {
                    val lat = ptLat
                    val lon = ptLon

                    // Fix #6, #8: Strict bounds and finiteness validation
                    val isValidCoord = lat != null && lon != null &&
                        !lat.isNaN() && !lat.isInfinite() && lat in -90.0..90.0 &&
                        !lon.isNaN() && !lon.isInfinite() && lon in -180.0..180.0

                    if (!isValidCoord) {
                        if (lat != null || lon != null) {
                            warnings.add("Skipped point with invalid coordinates (lat: $lat, lon: $lon).")
                        }
                        return
                    }

                    // Fix #19: Separate per-track and total limit checks
                    if (currentPoints.size >= maxPointsPerTrack) {
                        if (!trackLimitReachedWarned) {
                            warnings.add("Track '$currentTrackName' reached per-track limit ($maxPointsPerTrack points). Additional points omitted.")
                            trackLimitReachedWarned = true
                        }
                        return
                    }

                    if (totalPointCount >= maxTotalPoints) {
                        if (totalPointCount == maxTotalPoints) {
                            warnings.add("Total file limit ($maxTotalPoints points) reached. Additional points omitted.")
                        }
                        return
                    }

                    // Fix #20: Handle missing timestamps smoothly without defaulting to epoch 1970
                    val pointTimestampMs = ptTimeMs ?: if (currentPoints.isNotEmpty()) {
                        currentPoints.last().timestampEpochMs + 1000L
                    } else {
                        trackFallbackStartTimeMs
                    }

                    currentPoints.add(
                        GpsPoint(
                            latitude = lat!!,
                            longitude = lon!!,
                            altitudeMeters = ptEle,
                            speedMps = ptSpeed?.toFloat(),
                            accuracyMeters = ptAccuracy ?: 5.0f,
                            timestampEpochMs = pointTimestampMs,
                            isPaused = false,
                            isGap = false
                        )
                    )
                    totalPointCount++
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
