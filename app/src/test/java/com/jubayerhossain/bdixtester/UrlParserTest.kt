package com.jubayerhossain.bdixtester

import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.validation.UrlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlParserTest {
  @Test
  fun httpUrlWithFtpInHostnameIsClassifiedAsHttp() {
    val parsed = UrlParser.parse("http://ftp.example.net:8080/movies/")
    assertTrue(parsed.isValid)
    assertEquals(ServerProtocol.HTTP, parsed.protocol)
    assertEquals("ftp.example.net", parsed.host)
    assertEquals(8080, parsed.explicitPort)
    assertTrue(parsed.hasNonRootPath)
    assertFalse(parsed.isPrivateIp)
  }

  @Test
  fun ftpSchemeIsClassifiedAsFtp() {
    val parsed = UrlParser.parse("ftp://files.local.net")
    assertTrue(parsed.isValid)
    assertEquals(ServerProtocol.FTP, parsed.protocol)
    assertEquals("files.local.net", parsed.host)
    assertNull(parsed.explicitPort)
  }

  @Test
  fun ftpsSchemeIsClassifiedSeparatelyFromFtp() {
    val parsed = UrlParser.parse("ftps://secure.files.net:990/")
    assertTrue(parsed.isValid)
    assertEquals(ServerProtocol.FTPS, parsed.protocol)
    assertEquals(990, parsed.explicitPort)
    assertTrue(parsed.requiresManualReview)
  }

  @Test
  fun rfc1918PrivateIpv4RangesAreDetected() {
    assertTrue(UrlParser.parse("http://10.16.100.244/").isPrivateIp)
    assertTrue(UrlParser.parse("http://172.16.50.2/").isPrivateIp)
    assertTrue(UrlParser.parse("http://192.168.1.10/").isPrivateIp)
    assertFalse(UrlParser.parse("http://172.32.0.1/").isPrivateIp)
    assertFalse(UrlParser.parse("https://8.8.8.8/").isPrivateIp)
  }

  @Test
  fun invalidOrUnsupportedUrlsAreFlaggedWithoutMutation() {
    val raw = "not a valid url"
    val parsed = UrlParser.parse(raw)
    assertFalse(parsed.isValid)
    assertEquals(raw, parsed.originalUrl)
    assertTrue(parsed.requiresManualReview)
  }
}
