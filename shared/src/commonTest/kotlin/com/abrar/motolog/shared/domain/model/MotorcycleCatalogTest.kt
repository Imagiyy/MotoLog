package com.abrar.motolog.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue

class MotorcycleCatalogTest {

    @Test
    fun catalogContainsExtensivePreloadedMotorcyclesAcrossMajorManufacturers() {
        assertTrue(MotorcycleCatalog.allMotorcycles.size > 100, "Catalog should contain over 100 bikes")

        val brands = MotorcycleCatalog.brands
        assertTrue(brands.contains("Royal Enfield"), "Catalog should contain Royal Enfield")
        assertTrue(brands.contains("KTM"), "Catalog should contain KTM")
        assertTrue(brands.contains("Yamaha"), "Catalog should contain Yamaha")
        assertTrue(brands.contains("Kawasaki"), "Catalog should contain Kawasaki")
        assertTrue(brands.contains("Honda"), "Catalog should contain Honda")
        assertTrue(brands.contains("BMW Motorrad"), "Catalog should contain BMW Motorrad")
        assertTrue(brands.contains("Ducati"), "Catalog should contain Ducati")
        assertTrue(brands.contains("Triumph"), "Catalog should contain Triumph")
        assertTrue(brands.contains("Harley-Davidson"), "Catalog should contain Harley-Davidson")
    }

    @Test
    fun getModelsForBrandReturnsCorrectModels() {
        val ktmModels = MotorcycleCatalog.getModelsForBrand("KTM")
        assertTrue(ktmModels.contains("390 Duke"))
        assertTrue(ktmModels.contains("RC 390"))
        assertTrue(ktmModels.contains("390 Adventure"))

        val reModels = MotorcycleCatalog.getModelsForBrand("Royal Enfield")
        assertTrue(reModels.contains("Classic 350"))
        assertTrue(reModels.contains("Hunter 350"))
        assertTrue(reModels.contains("Himalayan 450"))
    }

    @Test
    fun searchFindsModelsByPartialQueryAndBrandTokens() {
        val dukeMatches = MotorcycleCatalog.search("390")
        assertTrue(dukeMatches.any { it.fullName == "KTM 390 Duke" })
        assertTrue(dukeMatches.any { it.fullName == "KTM 390 Adventure" })

        val ninjaMatches = MotorcycleCatalog.search("ninja 400")
        assertTrue(ninjaMatches.any { it.fullName == "Kawasaki Ninja 400" })

        val caseInsensitiveMatches = MotorcycleCatalog.search("HIMALAYAN")
        assertTrue(caseInsensitiveMatches.any { it.model.contains("Himalayan") })

        val emptyMatches = MotorcycleCatalog.search("   ")
        assertTrue(emptyMatches.isEmpty())
    }
}
