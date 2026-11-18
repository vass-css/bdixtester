package com.jubayerhossain.bdixtester.core.util

import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint

enum class ServerDisplayCategory(
  val shortLabel: String,
) {
  TV("TV"),
  FTP("FTP"),
  MOVIE("Movie"),
}

/**
 * Non-destructive UI-layer classifier that groups endpoints into TV, FTP, or Movie
 * for the reference Server List bottom navigation tabs without altering underlying
 * built-in catalog audit metadata.
 */
object ServerDisplayClassifier {
  private val tvKeywords =
    listOf(
      "tv",
      "iptv",
      "live",
      "stream",
      "channel",
      "bioscope",
      "bongo",
      "toffee",
      "jagobd",
      "t-sports",
      "tsports",
    )

  private val movieKeywords =
    listOf(
      "movie",
      "movies",
      "cinema",
      "flix",
      "cine",
      "plex",
      "emby",
      "jellyfin",
      "media",
      "film",
      "series",
      "hd",
      "circleftp",
      "dhakaflix",
      "samonline",
      "discovery",
      "crazyctg",
      "naturalbd",
      "roarzone",
      "icc",
      "bdiptv",
      "showtime",
      "entertainment",
    )

  fun classify(endpoint: ServerEndpoint): ServerDisplayCategory {
    when (endpoint.category) {
      ServerCategory.LIVE_TV -> return ServerDisplayCategory.TV
      ServerCategory.MOVIES -> return ServerDisplayCategory.MOVIE
      ServerCategory.FTP_FILES -> return ServerDisplayCategory.FTP
      else -> Unit
    }

    val haystack = "${endpoint.name} ${endpoint.host} ${endpoint.originalUrl}".lowercase()
    if (tvKeywords.any { haystack.contains(it) }) {
      return ServerDisplayCategory.TV
    }
    if (movieKeywords.any { haystack.contains(it) }) {
      return ServerDisplayCategory.MOVIE
    }
    if (endpoint.protocol == ServerProtocol.FTP || endpoint.protocol == ServerProtocol.FTPS) {
      return ServerDisplayCategory.FTP
    }
    return ServerDisplayCategory.FTP
  }

  fun displayLabel(endpoint: ServerEndpoint): String =
    when (endpoint.category) {
      ServerCategory.LIVE_TV -> ServerDisplayCategory.TV.shortLabel
      ServerCategory.MOVIES -> ServerDisplayCategory.MOVIE.shortLabel
      ServerCategory.FTP_FILES -> ServerDisplayCategory.FTP.shortLabel
      ServerCategory.SOFTWARE -> endpoint.category.displayName
      ServerCategory.WEB -> endpoint.category.displayName
      ServerCategory.OTHER -> endpoint.category.displayName
      ServerCategory.ALL, ServerCategory.UNCATEGORIZED -> classify(endpoint).shortLabel
    }
}
