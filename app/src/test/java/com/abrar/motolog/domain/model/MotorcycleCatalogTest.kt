package com.abrar.motolog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorcycleCatalogTest {

    @Test
    fun `catalog contains extensive preloaded motorcycles across major manufacturers`() {
        assertTrue("Catalog should contain over 100 bikes", MotorcycleCatalog.allMotorcycles.size > 100)

        val brands = MotorcycleCatalog.brands
        assertTrue("Catalog should contain Royal Enfield", brands.contains("Royal Enfield"))
        assertTrue("Catalog should contain KTM", brands.contains("KTM"))
        assertTrue("Catalog should contain Yamaha", brands.contains("Yamaha"))
        assertTrue("Catalog should contain Kawasaki", brands.contains("Kawasaki"))
        assertTrue("Catalog should contain Honda", brands.contains("Honda"))
        assertTrue("Catalog should contain BMW Motorrad", brands.contains("BMW Motorrad"))
        assertTrue("Catalog should contain Ducati", brands.contains("Ducati"))
        assertTrue("Catalog should contain Triumph", brands.contains("Triumph"))
        assertTrue("Catalog should contain Harley-Davidson", brands.contains("Harley-Davidson"))
    }

    @Test
    fun `getModelsForBrand returns correct models`() {
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
    fun `search finds models by partial query and brand tokens`() {
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
