package com.abrar.motolog.test

import com.abrar.motolog.shared.domain.model.GpsPoint
import java.io.InputStream
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Lightweight GPX parser for unit tests.
 * Lives in the test source set only (not packaged in the application binary).
 */
object GpxParser {

    fun parse(inputStream: InputStream): List<GpsPoint> {
        val points = mutableListOf<GpsPoint>()
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        doc.documentElement.normalize()

        val trkptList = doc.getElementsByTagName("trkpt")
        for (i in 0 until trkptList.length) {
            val node = trkptList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val lat = element.getAttribute("lat").toDouble()
                val lon = element.getAttribute("lon").toDouble()

                var ele: Double? = null
                var timestampMs: Long = 0L
                var speedMps: Float? = null
                var accuracyMeters: Float = 10f
                var speedAccuracyMps: Float? = null

                val children = element.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child.nodeType == Node.ELEMENT_NODE) {
                        when (child.nodeName) {
                            "ele" -> ele = child.textContent.toDoubleOrNull()
                            "time" -> {
                                val text = child.textContent.trim()
                                timestampMs = try {
                                    Instant.parse(text).toEpochMilli()
                                } catch (e: Exception) {
                                    text.toLongOrNull() ?: 0L
                                }
                            }
                            "speed" -> speedMps = child.textContent.toFloatOrNull()
                            "extensions" -> {
                                val extChildren = child.childNodes
                                for (k in 0 until extChildren.length) {
                                    val extChild = extChildren.item(k)
                                    if (extChild.nodeType == Node.ELEMENT_NODE) {
                                        when (extChild.nodeName) {
                                            "speed" -> speedMps = extChild.textContent.toFloatOrNull()
                                            "accuracy" -> accuracyMeters = extChild.textContent.toFloatOrNull() ?: 10f
                                            "speedAccuracy" -> speedAccuracyMps = extChild.textContent.toFloatOrNull()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                points.add(
                    GpsPoint(
                        timestampEpochMs = timestampMs,
                        latitude = lat,
                        longitude = lon,
                        speedMps = speedMps,
                        accuracyMeters = accuracyMeters,
                        speedAccuracyMps = speedAccuracyMps,
                        altitudeMeters = ele
                    )
                )
            }
        }
        return points
    }
}
