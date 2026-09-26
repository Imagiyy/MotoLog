package com.abrar.motolog.shared.domain.model

/**
 * Represents a motorcycle brand and model in the preloaded catalog.
 */
data class MotorcycleModel(
    val brand: String,
    val model: String
) {
    val fullName: String get() = "$brand $model"
}

/**
 * Built-in motorcycle catalog containing popular and iconic motorcycles across
 * major global and regional manufacturers.
 */
object MotorcycleCatalog {

    val allMotorcycles: List<MotorcycleModel> = listOf(
        // Royal Enfield
        MotorcycleModel("Royal Enfield", "Classic 350"),
        MotorcycleModel("Royal Enfield", "Hunter 350"),
        MotorcycleModel("Royal Enfield", "Meteor 350"),
        MotorcycleModel("Royal Enfield", "Bullet 350"),
        MotorcycleModel("Royal Enfield", "Himalayan 450"),
        MotorcycleModel("Royal Enfield", "Guerrilla 450"),
        MotorcycleModel("Royal Enfield", "Interceptor 650"),
        MotorcycleModel("Royal Enfield", "Continental GT 650"),
        MotorcycleModel("Royal Enfield", "Super Meteor 650"),
        MotorcycleModel("Royal Enfield", "Shotgun 650"),
        MotorcycleModel("Royal Enfield", "Bear 650"),
        MotorcycleModel("Royal Enfield", "Classic 650"),
        MotorcycleModel("Royal Enfield", "Himalayan 411"),
        MotorcycleModel("Royal Enfield", "Scram 411"),
        MotorcycleModel("Royal Enfield", "Classic 500"),
        MotorcycleModel("Royal Enfield", "Bullet 500"),
        MotorcycleModel("Royal Enfield", "Thunderbird 350"),

        // KTM
        MotorcycleModel("KTM", "390 Duke"),
        MotorcycleModel("KTM", "250 Duke"),
        MotorcycleModel("KTM", "200 Duke"),
        MotorcycleModel("KTM", "125 Duke"),
        MotorcycleModel("KTM", "790 Duke"),
        MotorcycleModel("KTM", "890 Duke R"),
        MotorcycleModel("KTM", "990 Duke"),
        MotorcycleModel("KTM", "1290 Super Duke R"),
        MotorcycleModel("KTM", "1390 Super Duke R"),
        MotorcycleModel("KTM", "RC 390"),
        MotorcycleModel("KTM", "RC 200"),
        MotorcycleModel("KTM", "RC 125"),
        MotorcycleModel("KTM", "390 Adventure"),
        MotorcycleModel("KTM", "250 Adventure"),
        MotorcycleModel("KTM", "790 Adventure"),
        MotorcycleModel("KTM", "890 Adventure R"),
        MotorcycleModel("KTM", "1290 Super Adventure R"),
        MotorcycleModel("KTM", "690 Enduro R"),
        MotorcycleModel("KTM", "690 SMC R"),

        // Yamaha
        MotorcycleModel("Yamaha", "YZF-R15 V4"),
        MotorcycleModel("Yamaha", "MT-15 V2"),
        MotorcycleModel("Yamaha", "YZF-R3"),
        MotorcycleModel("Yamaha", "MT-03"),
        MotorcycleModel("Yamaha", "YZF-R7"),
        MotorcycleModel("Yamaha", "MT-07"),
        MotorcycleModel("Yamaha", "YZF-R6"),
        MotorcycleModel("Yamaha", "YZF-R1"),
        MotorcycleModel("Yamaha", "YZF-R1M"),
        MotorcycleModel("Yamaha", "MT-09"),
        MotorcycleModel("Yamaha", "MT-10"),
        MotorcycleModel("Yamaha", "XSR155"),
        MotorcycleModel("Yamaha", "XSR700"),
        MotorcycleModel("Yamaha", "XSR900"),
        MotorcycleModel("Yamaha", "Tenere 700"),
        MotorcycleModel("Yamaha", "Tracer 9 GT"),
        MotorcycleModel("Yamaha", "FZ-S FI"),
        MotorcycleModel("Yamaha", "FZ-X"),
        MotorcycleModel("Yamaha", "FZ25"),
        MotorcycleModel("Yamaha", "Aerox 155"),
        MotorcycleModel("Yamaha", "RayZR 125"),
        MotorcycleModel("Yamaha", "Fascino 125"),

        // Kawasaki
        MotorcycleModel("Kawasaki", "Ninja 300"),
        MotorcycleModel("Kawasaki", "Ninja 400"),
        MotorcycleModel("Kawasaki", "Ninja 500"),
        MotorcycleModel("Kawasaki", "Ninja 650"),
        MotorcycleModel("Kawasaki", "Ninja ZX-4R"),
        MotorcycleModel("Kawasaki", "Ninja ZX-4RR"),
        MotorcycleModel("Kawasaki", "Ninja ZX-6R"),
        MotorcycleModel("Kawasaki", "Ninja ZX-10R"),
        MotorcycleModel("Kawasaki", "Ninja 1000SX"),
        MotorcycleModel("Kawasaki", "Ninja H2"),
        MotorcycleModel("Kawasaki", "Z400"),
        MotorcycleModel("Kawasaki", "Z500"),
        MotorcycleModel("Kawasaki", "Z650"),
        MotorcycleModel("Kawasaki", "Z900"),
        MotorcycleModel("Kawasaki", "Z H2"),
        MotorcycleModel("Kawasaki", "Z900RS"),
        MotorcycleModel("Kawasaki", "Z650RS"),
        MotorcycleModel("Kawasaki", "Versys 650"),
        MotorcycleModel("Kawasaki", "Versys 1000"),
        MotorcycleModel("Kawasaki", "Versys-X 300"),
        MotorcycleModel("Kawasaki", "Vulcan S"),
        MotorcycleModel("Kawasaki", "Eliminator 500"),
        MotorcycleModel("Kawasaki", "KLX 230"),
        MotorcycleModel("Kawasaki", "KLX 300"),
        MotorcycleModel("Kawasaki", "W800"),
        MotorcycleModel("Kawasaki", "W175"),

        // Honda
        MotorcycleModel("Honda", "CB300R"),
        MotorcycleModel("Honda", "CB300F"),
        MotorcycleModel("Honda", "H'ness CB350"),
        MotorcycleModel("Honda", "CB350RS"),
        MotorcycleModel("Honda", "CB500X"),
        MotorcycleModel("Honda", "NX500"),
        MotorcycleModel("Honda", "CB650R"),
        MotorcycleModel("Honda", "CBR650R"),
        MotorcycleModel("Honda", "CBR1000RR-R Fireblade"),
        MotorcycleModel("Honda", "CRF1100L Africa Twin"),
        MotorcycleModel("Honda", "XL750 Transalp"),
        MotorcycleModel("Honda", "Rebel 300"),
        MotorcycleModel("Honda", "Rebel 500"),
        MotorcycleModel("Honda", "Rebel 1100"),
        MotorcycleModel("Honda", "Gold Wing"),
        MotorcycleModel("Honda", "Hornet 2.0"),
        MotorcycleModel("Honda", "CBR150R"),
        MotorcycleModel("Honda", "CBR250R"),
        MotorcycleModel("Honda", "SP 125"),
        MotorcycleModel("Honda", "Shine 125"),
        MotorcycleModel("Honda", "Activa 6G"),
        MotorcycleModel("Honda", "Activa 125"),
        MotorcycleModel("Honda", "Dio 125"),

        // Suzuki
        MotorcycleModel("Suzuki", "Hayabusa (GSX1300R)"),
        MotorcycleModel("Suzuki", "GSX-8R"),
        MotorcycleModel("Suzuki", "GSX-8S"),
        MotorcycleModel("Suzuki", "GSX-R1000R"),
        MotorcycleModel("Suzuki", "GSX-R750"),
        MotorcycleModel("Suzuki", "GSX-R600"),
        MotorcycleModel("Suzuki", "Katana"),
        MotorcycleModel("Suzuki", "V-Strom 250SX"),
        MotorcycleModel("Suzuki", "V-Strom 650XT"),
        MotorcycleModel("Suzuki", "V-Strom 800DE"),
        MotorcycleModel("Suzuki", "V-Strom 1050DE"),
        MotorcycleModel("Suzuki", "Gixxer SF 250"),
        MotorcycleModel("Suzuki", "Gixxer 250"),
        MotorcycleModel("Suzuki", "Gixxer SF 150"),
        MotorcycleModel("Suzuki", "Gixxer 150"),
        MotorcycleModel("Suzuki", "SV650"),
        MotorcycleModel("Suzuki", "Access 125"),
        MotorcycleModel("Suzuki", "Burgman Street 125"),
        MotorcycleModel("Suzuki", "Avenis 125"),

        // BMW Motorrad
        MotorcycleModel("BMW Motorrad", "S 1000 RR"),
        MotorcycleModel("BMW Motorrad", "M 1000 RR"),
        MotorcycleModel("BMW Motorrad", "S 1000 R"),
        MotorcycleModel("BMW Motorrad", "M 1000 R"),
        MotorcycleModel("BMW Motorrad", "S 1000 XR"),
        MotorcycleModel("BMW Motorrad", "R 1300 GS"),
        MotorcycleModel("BMW Motorrad", "R 1250 GS Adventure"),
        MotorcycleModel("BMW Motorrad", "R 1250 RT"),
        MotorcycleModel("BMW Motorrad", "R 18"),
        MotorcycleModel("BMW Motorrad", "F 900 GS"),
        MotorcycleModel("BMW Motorrad", "F 900 R"),
        MotorcycleModel("BMW Motorrad", "F 900 XR"),
        MotorcycleModel("BMW Motorrad", "G 310 R"),
        MotorcycleModel("BMW Motorrad", "G 310 GS"),
        MotorcycleModel("BMW Motorrad", "CE 04"),
        MotorcycleModel("BMW Motorrad", "CE 02"),

        // Ducati
        MotorcycleModel("Ducati", "Panigale V4"),
        MotorcycleModel("Ducati", "Panigale V4 S"),
        MotorcycleModel("Ducati", "Panigale V2"),
        MotorcycleModel("Ducati", "Streetfighter V4"),
        MotorcycleModel("Ducati", "Streetfighter V2"),
        MotorcycleModel("Ducati", "Monster"),
        MotorcycleModel("Ducati", "Monster Plus"),
        MotorcycleModel("Ducati", "Monster SP"),
        MotorcycleModel("Ducati", "Hypermotard 698 Mono"),
        MotorcycleModel("Ducati", "Hypermotard 950"),
        MotorcycleModel("Ducati", "DesertX"),
        MotorcycleModel("Ducati", "DesertX Rally"),
        MotorcycleModel("Ducati", "Multistrada V4"),
        MotorcycleModel("Ducati", "Multistrada V4 Rally"),
        MotorcycleModel("Ducati", "Multistrada V2"),
        MotorcycleModel("Ducati", "Diavel V4"),
        MotorcycleModel("Ducati", "Scrambler Icon"),
        MotorcycleModel("Ducati", "Scrambler Nightshift"),
        MotorcycleModel("Ducati", "Scrambler Full Throttle"),

        // Triumph
        MotorcycleModel("Triumph", "Speed 400"),
        MotorcycleModel("Triumph", "Scrambler 400 X"),
        MotorcycleModel("Triumph", "Street Triple 765 RS"),
        MotorcycleModel("Triumph", "Street Triple 765 R"),
        MotorcycleModel("Triumph", "Speed Triple 1200 RS"),
        MotorcycleModel("Triumph", "Trident 660"),
        MotorcycleModel("Triumph", "Daytona 660"),
        MotorcycleModel("Triumph", "Tiger Sport 660"),
        MotorcycleModel("Triumph", "Tiger Sport 800"),
        MotorcycleModel("Triumph", "Tiger 900 Rally Pro"),
        MotorcycleModel("Triumph", "Tiger 900 GT"),
        MotorcycleModel("Triumph", "Tiger 1200 Rally Pro"),
        MotorcycleModel("Triumph", "Tiger 1200 GT"),
        MotorcycleModel("Triumph", "Bonneville T120"),
        MotorcycleModel("Triumph", "Bonneville T100"),
        MotorcycleModel("Triumph", "Speed Twin 1200"),
        MotorcycleModel("Triumph", "Speed Twin 900"),
        MotorcycleModel("Triumph", "Scrambler 1200"),
        MotorcycleModel("Triumph", "Scrambler 900"),
        MotorcycleModel("Triumph", "Rocket 3 R"),
        MotorcycleModel("Triumph", "Rocket 3 GT"),

        // Harley-Davidson
        MotorcycleModel("Harley-Davidson", "X440"),
        MotorcycleModel("Harley-Davidson", "Sportster S"),
        MotorcycleModel("Harley-Davidson", "Nightster"),
        MotorcycleModel("Harley-Davidson", "Nightster Special"),
        MotorcycleModel("Harley-Davidson", "Iron 883"),
        MotorcycleModel("Harley-Davidson", "Forty-Eight"),
        MotorcycleModel("Harley-Davidson", "Fat Boy 114"),
        MotorcycleModel("Harley-Davidson", "Breakout 117"),
        MotorcycleModel("Harley-Davidson", "Street Bob 114"),
        MotorcycleModel("Harley-Davidson", "Low Rider S"),
        MotorcycleModel("Harley-Davidson", "Low Rider ST"),
        MotorcycleModel("Harley-Davidson", "Heritage Classic"),
        MotorcycleModel("Harley-Davidson", "Road King Special"),
        MotorcycleModel("Harley-Davidson", "Street Glide"),
        MotorcycleModel("Harley-Davidson", "Road Glide"),
        MotorcycleModel("Harley-Davidson", "Pan America 1250 Special"),

        // Bajaj
        MotorcycleModel("Bajaj", "Pulsar NS400Z"),
        MotorcycleModel("Bajaj", "Pulsar NS200"),
        MotorcycleModel("Bajaj", "Pulsar NS160"),
        MotorcycleModel("Bajaj", "Pulsar NS125"),
        MotorcycleModel("Bajaj", "Pulsar N250"),
        MotorcycleModel("Bajaj", "Pulsar F250"),
        MotorcycleModel("Bajaj", "Pulsar N160"),
        MotorcycleModel("Bajaj", "Pulsar N150"),
        MotorcycleModel("Bajaj", "Pulsar 220F"),
        MotorcycleModel("Bajaj", "Pulsar 150"),
        MotorcycleModel("Bajaj", "Pulsar 125"),
        MotorcycleModel("Bajaj", "Pulsar RS200"),
        MotorcycleModel("Bajaj", "Dominar 400"),
        MotorcycleModel("Bajaj", "Dominar 250"),
        MotorcycleModel("Bajaj", "Avenger Cruise 220"),
        MotorcycleModel("Bajaj", "Avenger Street 160"),
        MotorcycleModel("Bajaj", "Chetak EV"),
        MotorcycleModel("Bajaj", "Platina 110"),

        // TVS
        MotorcycleModel("TVS", "Apache RR 310"),
        MotorcycleModel("TVS", "Apache RTR 310"),
        MotorcycleModel("TVS", "Apache RTR 200 4V"),
        MotorcycleModel("TVS", "Apache RTR 180"),
        MotorcycleModel("TVS", "Apache RTR 160 4V"),
        MotorcycleModel("TVS", "Apache RTR 160 2V"),
        MotorcycleModel("TVS", "Ronin 225"),
        MotorcycleModel("TVS", "Raider 125"),
        MotorcycleModel("TVS", "Ntorq 125"),
        MotorcycleModel("TVS", "Jupiter 110"),
        MotorcycleModel("TVS", "Jupiter 125"),
        MotorcycleModel("TVS", "iQube EV"),

        // Hero MotoCorp
        MotorcycleModel("Hero", "Mavrick 440"),
        MotorcycleModel("Hero", "Karizma XMR 210"),
        MotorcycleModel("Hero", "Xpulse 200 4V"),
        MotorcycleModel("Hero", "Xpulse 200T 4V"),
        MotorcycleModel("Hero", "Xtreme 160R 4V"),
        MotorcycleModel("Hero", "Xtreme 125R"),
        MotorcycleModel("Hero", "Xtreme 200S 4V"),
        MotorcycleModel("Hero", "Splendor Plus"),
        MotorcycleModel("Hero", "Splendor Plus XTEC"),
        MotorcycleModel("Hero", "HF Deluxe"),
        MotorcycleModel("Hero", "Glamour XTEC"),
        MotorcycleModel("Hero", "Passion Plus"),
        MotorcycleModel("Hero", "Vida V1 EV"),

        // Aprilia
        MotorcycleModel("Aprilia", "RS 457"),
        MotorcycleModel("Aprilia", "RS 660"),
        MotorcycleModel("Aprilia", "Tuono 660"),
        MotorcycleModel("Aprilia", "Tuareg 660"),
        MotorcycleModel("Aprilia", "RSV4 1100 Factory"),
        MotorcycleModel("Aprilia", "Tuono V4 Factory"),
        MotorcycleModel("Aprilia", "SR 160"),
        MotorcycleModel("Aprilia", "SR 125"),
        MotorcycleModel("Aprilia", "SXR 160"),

        // Husqvarna
        MotorcycleModel("Husqvarna", "Svartpilen 401"),
        MotorcycleModel("Husqvarna", "Svartpilen 250"),
        MotorcycleModel("Husqvarna", "Vitpilen 401"),
        MotorcycleModel("Husqvarna", "Vitpilen 250"),
        MotorcycleModel("Husqvarna", "Norden 901"),
        MotorcycleModel("Husqvarna", "701 Enduro"),
        MotorcycleModel("Husqvarna", "701 Supermoto"),

        // Jawa & Yezdi
        MotorcycleModel("Jawa", "350"),
        MotorcycleModel("Jawa", "42"),
        MotorcycleModel("Jawa", "42 Bobber"),
        MotorcycleModel("Jawa", "Perak"),
        MotorcycleModel("Yezdi", "Roadster"),
        MotorcycleModel("Yezdi", "Scrambler"),
        MotorcycleModel("Yezdi", "Adventure"),

        // CFMoto
        MotorcycleModel("CFMoto", "450SR / 450SS"),
        MotorcycleModel("CFMoto", "450NK"),
        MotorcycleModel("CFMoto", "450MT / Ibex 450"),
        MotorcycleModel("CFMoto", "800NK"),
        MotorcycleModel("CFMoto", "800MT / Ibex 800"),
        MotorcycleModel("CFMoto", "300NK"),
        MotorcycleModel("CFMoto", "300SS / 300SR"),
        MotorcycleModel("CFMoto", "650NK"),
        MotorcycleModel("CFMoto", "700CL-X"),

        // Benelli
        MotorcycleModel("Benelli", "TRK 502"),
        MotorcycleModel("Benelli", "TRK 502X"),
        MotorcycleModel("Benelli", "TRK 702"),
        MotorcycleModel("Benelli", "Leoncino 500"),
        MotorcycleModel("Benelli", "Imperiale 400"),
        MotorcycleModel("Benelli", "TNT 300"),
        MotorcycleModel("Benelli", "TNT 600i"),
        MotorcycleModel("Benelli", "502C"),

        // Indian Motorcycle
        MotorcycleModel("Indian", "Scout"),
        MotorcycleModel("Indian", "Scout Bobber"),
        MotorcycleModel("Indian", "Scout Rogue"),
        MotorcycleModel("Indian", "Chief"),
        MotorcycleModel("Indian", "Chief Dark Horse"),
        MotorcycleModel("Indian", "FTR 1200"),
        MotorcycleModel("Indian", "Challenger"),
        MotorcycleModel("Indian", "Chieftain"),

        // Moto Guzzi
        MotorcycleModel("Moto Guzzi", "V7 Stone"),
        MotorcycleModel("Moto Guzzi", "V7 Special"),
        MotorcycleModel("Moto Guzzi", "V9 Bobber"),
        MotorcycleModel("Moto Guzzi", "V85 TT"),
        MotorcycleModel("Moto Guzzi", "V100 Mandello"),
        MotorcycleModel("Moto Guzzi", "Stelvio")
    )

    /**
     * Unique list of all brands sorted alphabetically.
     */
    val brands: List<String> by lazy {
        allMotorcycles.map { it.brand }.distinct().sorted()
    }

    /**
     * Popular brands shown as quick filter chips.
     */
    val popularBrands: List<String> = listOf(
        "Royal Enfield",
        "KTM",
        "Yamaha",
        "Kawasaki",
        "Honda",
        "Suzuki",
        "BMW Motorrad",
        "Ducati",
        "Triumph",
        "Harley-Davidson",
        "Bajaj",
        "TVS"
    )

    /**
     * Returns all models for a specific brand.
     */
    fun getModelsForBrand(brand: String): List<String> {
        return allMotorcycles
            .filter { it.brand.equals(brand.trim(), ignoreCase = true) }
            .map { it.model }
            .distinct()
            .sorted()
    }

    /**
     * Searches motorcycles by query across both brand and model name.
     */
    fun search(query: String, maxResults: Int = 12): List<MotorcycleModel> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        return allMotorcycles.filter { entry ->
            val fullText = "${entry.brand} ${entry.model}".lowercase()
            tokens.all { token -> fullText.contains(token) }
        }.take(maxResults)
    }
}
