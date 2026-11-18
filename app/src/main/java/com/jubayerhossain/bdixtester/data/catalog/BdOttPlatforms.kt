package com.jubayerhossain.bdixtester.data.catalog

enum class OttRegion {
  BANGLADESH,
  GLOBAL_IN_BD,
}

enum class OttRegionFilter {
  ALL,
  BANGLADESH,
  GLOBAL_IN_BD,
}

enum class OttGenre {
  ORIGINALS_MOVIES,
  LIVE_TV_AGGREGATOR,
  LIVE_SPORTS,
  KIDS_FAMILY,
  ANIME_DRAMA,
}

data class OttPlatform(
  val id: String,
  val name: String,
  val url: String,
  val tagline: String,
  val region: OttRegion,
  val genre: OttGenre,
)

/**
 * Curated directory of 26 verified OTT streaming platforms available in Bangladesh:
 * - 17 Bangladeshi & regional Bangla OTT platforms
 * - 9 International OTT platforms officially accessible in Bangladesh
 *
 * Kept separate from [FtpLinks] so all 369 verbatim BDIX endpoints and catalog audit
 * invariants remain 100% untouched.
 */
object BdOttPlatforms {
  val all: List<OttPlatform> =
    listOf(
      // --- Bangladeshi & Regional Bangla OTT Platforms (17) ---
      OttPlatform(
        id = "ott_chorki",
        name = "Chorki",
        url = "https://chorki.net",
        tagline = "Bangla Originals, Films & Web Series",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_bongo",
        name = "Bongo",
        url = "https://bongobd.com",
        tagline = "Bangla Movies, Natok & Live TV",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_hoichoi",
        name = "Hoichoi",
        url = "https://hoichoi.tv",
        tagline = "Bengali Originals, Thrillers & Cinema",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_toffee",
        name = "Toffee",
        url = "https://toffeelive.com",
        tagline = "Live TV, Sports & Entertainment by Banglalink",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_TV_AGGREGATOR,
      ),
      OttPlatform(
        id = "ott_bioscope",
        name = "Bioscope+",
        url = "https://bioscopelive.com",
        tagline = "Grameenphone OTT Aggregator & Live TV",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_TV_AGGREGATOR,
      ),
      OttPlatform(
        id = "ott_binge",
        name = "Binge",
        url = "https://binge.buzz",
        tagline = "Web Series, Movies & Live TV by Robi",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_deeptoplay",
        name = "DeeptoPlay",
        url = "https://deeptoplay.com",
        tagline = "Deepto TV Originals, Series & Dramas",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_iscreen",
        name = "iScreen",
        url = "https://iscreen.com.bd",
        tagline = "Bangla Cinema & Channel i Archive",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_tsports",
        name = "T Sports",
        url = "https://www.tsports.com",
        tagline = "Live Cricket, Football & Sports Broadcast",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_SPORTS,
      ),
      OttPlatform(
        id = "ott_rabbithole",
        name = "Rabbitholebd",
        url = "https://rabbitholebd.com",
        tagline = "Live Cricket & Sports Streaming",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_SPORTS,
      ),
      OttPlatform(
        id = "ott_tapmad",
        name = "Tapmad",
        url = "https://www.tapmad.com",
        tagline = "Live Tournaments, Sports & Entertainment",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_SPORTS,
      ),
      OttPlatform(
        id = "ott_akashgo",
        name = "Akash Go",
        url = "https://akashgo.com",
        tagline = "Akash Digital TV Live & On-Demand",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_TV_AGGREGATOR,
      ),
      OttPlatform(
        id = "ott_banglaflix",
        name = "Banglaflix",
        url = "https://www.banglaflix.com.bd",
        tagline = "Bengali Movies & Video-on-Demand",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_cinematic",
        name = "Cinematic",
        url = "https://www.cinematic.mobi",
        tagline = "Local Bangla Films & Drama VOD",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_addatimes",
        name = "Addatimes",
        url = "https://www.addatimes.com",
        tagline = "Bengali Web Series, Thrillers & Shorts",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_jagobd",
        name = "Jagobd",
        url = "https://jagobd.com",
        tagline = "Bangladeshi Live TV Channels Portal",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.LIVE_TV_AGGREGATOR,
      ),
      OttPlatform(
        id = "ott_duronto",
        name = "Duronto TV",
        url = "https://duronto.tv",
        tagline = "Bangla Kids & Family Streaming",
        region = OttRegion.BANGLADESH,
        genre = OttGenre.KIDS_FAMILY,
      ),
      // --- International OTT Platforms Officially Available in Bangladesh (9) ---
      OttPlatform(
        id = "ott_netflix",
        name = "Netflix",
        url = "https://www.netflix.com",
        tagline = "Global Series, Films & Documentaries",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_primevideo",
        name = "Amazon Prime Video",
        url = "https://www.primevideo.com",
        tagline = "Amazon Originals, Hollywood & Regional Hits",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_zee5",
        name = "ZEE5",
        url = "https://www.zee5.com",
        tagline = "South Asian Movies, Bangla & Hindi Series",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_sonyliv",
        name = "SonyLIV",
        url = "https://www.sonyliv.com",
        tagline = "Live Sports, Indian Originals & TV Shows",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.LIVE_SPORTS,
      ),
      OttPlatform(
        id = "ott_lionsgateplay",
        name = "Lionsgate Play",
        url = "https://lionsgateplay.com",
        tagline = "Hollywood Blockbusters & Action Series",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_appletv",
        name = "Apple TV+",
        url = "https://tv.apple.com",
        tagline = "Apple Originals & Award-Winning Cinema",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
      OttPlatform(
        id = "ott_crunchyroll",
        name = "Crunchyroll",
        url = "https://www.crunchyroll.com",
        tagline = "Official Anime Simulcasts & Series",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ANIME_DRAMA,
      ),
      OttPlatform(
        id = "ott_viki",
        name = "Rakuten Viki",
        url = "https://www.viki.com",
        tagline = "Korean, Chinese & Asian Dramas",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ANIME_DRAMA,
      ),
      OttPlatform(
        id = "ott_mubi",
        name = "MUBI",
        url = "https://mubi.com",
        tagline = "Curated World & Independent Cinema",
        region = OttRegion.GLOBAL_IN_BD,
        genre = OttGenre.ORIGINALS_MOVIES,
      ),
    )

  val bangladeshCount: Int = all.count { it.region == OttRegion.BANGLADESH }
  val globalCount: Int = all.count { it.region == OttRegion.GLOBAL_IN_BD }
}
