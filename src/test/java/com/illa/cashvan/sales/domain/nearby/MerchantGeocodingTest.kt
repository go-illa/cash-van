package com.illa.cashvan.sales.domain.nearby

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

// Ported from the PWA's src/lib/merchantGeocoding.test.ts. The fetch / Rails
// mocks become injected providers; a failing Nominatim response is a provider
// that throws (as the PWA's non-OK branch does).
class MerchantGeocodingTest {

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private val nominatimCairo = json("""{"display_name":"مدينة نصر، القاهرة، مصر","address":{"state":"محافظة القاهرة"}}""")
    private val railsCairo = json("""{"address":"شارع التسعين","governorate_name":"القاهرة"}""")

    // response parsing

    @Test
    fun `parses the documented Rails response`() {
        assertEquals(MerchantGeocodeFields("شارع التسعين", "القاهرة"), parseRailsGeocodeResponse(railsCairo))
    }

    @Test
    fun `parses the nested Rails response used by older environments`() {
        assertEquals(
            MerchantGeocodeFields("التجمع الخامس", "Cairo Governorate"),
            parseRailsGeocodeResponse(
                json("""{"data":{"formatted_address":"التجمع الخامس","components":{"governorate":"Cairo Governorate"}}}"""),
            ),
        )
    }

    @Test
    fun `parses the same Nominatim shape used by Presell`() {
        assertEquals(
            MerchantGeocodeFields("مدينة نصر، القاهرة، مصر", "محافظة القاهرة"),
            parseNominatimGeocodeResponse(nominatimCairo),
        )
    }

    @Test
    fun `trims values, skips blank or non-string ones and tolerates odd payloads (extra)`() {
        assertEquals(
            MerchantGeocodeFields("Main St", "Giza"),
            parseRailsGeocodeResponse(json("""{"address":"  ","detailed_address":5,"formatted_address":" Main St ","governorate":"Giza"}""")),
        )
        assertEquals(MerchantGeocodeFields("", ""), parseRailsGeocodeResponse(null))
        assertEquals(MerchantGeocodeFields("", ""), parseNominatimGeocodeResponse(json("\"text\"")))
        // An array `data` is still an object in JS, so the root is not used as a fallback.
        assertEquals(MerchantGeocodeFields("", ""), parseRailsGeocodeResponse(json("""{"data":[],"address":"Root"}""")))
        assertEquals(
            MerchantGeocodeFields("x", "Alexandria"),
            parseNominatimGeocodeResponse(json("""{"display_name":"x","address":{"state":"","county":"Alexandria"}}""")),
        )
    }

    @Test
    fun `builds the Nominatim reverse URL with JS number strings (extra)`() {
        assertEquals(
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=30.05&lon=31&accept-language=ar%2Cen&addressdetails=1",
            buildNominatimReverseUrl(30.05, 31.0),
        )
    }

    // provider order

    @Test
    fun `uses OpenStreetMap first without calling Rails when the response is complete`() {
        var railsCalls = 0

        val result = runBlocking {
            resolveCashvanMerchantGeocode(
                openStreetMap = { parseNominatimGeocodeResponse(nominatimCairo) },
                rails = { railsCalls++; railsCairo },
            )
        }

        assertEquals(MerchantGeocodeResult("مدينة نصر، القاهرة، مصر", "محافظة القاهرة", GeocodeSource.OPENSTREETMAP), result)
        assertEquals(0, railsCalls)
    }

    @Test
    fun `falls back to Rails only after OpenStreetMap fails`() {
        var railsCalls = 0

        val result = runBlocking {
            resolveCashvanMerchantGeocode(
                openStreetMap = { throw IllegalStateException("Nominatim reverse geocode failed: 503") },
                rails = { railsCalls++; railsCairo },
            )
        }

        assertEquals(MerchantGeocodeResult("شارع التسعين", "القاهرة", GeocodeSource.RAILS), result)
        assertEquals(1, railsCalls)
    }

    @Test
    fun `fills partial OpenStreetMap data from Rails and keeps the OpenStreetMap source (extra)`() {
        val result = runBlocking {
            resolveCashvanMerchantGeocode(
                openStreetMap = { MerchantGeocodeFields("OSM address", "") },
                rails = { railsCairo },
            )
        }

        assertEquals(MerchantGeocodeResult("OSM address", "القاهرة", GeocodeSource.OPENSTREETMAP), result)
    }

    @Test
    fun `returns partial OpenStreetMap data when Rails fails (extra)`() {
        val result = runBlocking {
            resolveCashvanMerchantGeocode(
                openStreetMap = { MerchantGeocodeFields("", "Giza") },
                rails = { throw IllegalStateException("rails down") },
            )
        }

        assertEquals(MerchantGeocodeResult("", "Giza", GeocodeSource.OPENSTREETMAP), result)
    }

    @Test
    fun `throws when neither provider has any detail (extra)`() {
        val noDetails = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                resolveCashvanMerchantGeocode(
                    openStreetMap = { MerchantGeocodeFields("", "") },
                    rails = { json("{}") },
                )
            }
        }
        val railsDown = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                resolveCashvanMerchantGeocode(
                    openStreetMap = { throw IllegalStateException("osm down") },
                    rails = { throw IllegalStateException("rails down") },
                )
            }
        }

        assertEquals("Reverse geocoding returned no location details", noDetails.message)
        assertEquals("rails down", railsDown.message)
    }
}
