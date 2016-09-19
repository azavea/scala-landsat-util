package com.azavea.landsatutil

import java.io._
import org.apache.commons.io.IOUtils

/** Allows providing a hook that can either replace or filter InputStream */
trait IOHook {
  def apply(image: LandsatImage, band: String, is: => InputStream): InputStream
}

object IOHook {
  /** Passthrough the InputStream unmodified */
  def passthrough = new IOHook {
    def apply(image: LandsatImage, band: String, is: => InputStream): InputStream = is
  }

  /** Cache the InputStream bytes to a local file and read from disk for all future requests */
  def localCache(cacheDir: File) = new IOHook {
    def apply(image: LandsatImage, band: String, is: => InputStream): InputStream = {
      val cacheFile = new File(cacheDir, s"${image.baseS3Path}/$band")
      if (cacheFile.exists) {
        new FileInputStream(cacheFile)
      } else {
        cacheFile.getParentFile.mkdirs()
        val out = new FileOutputStream(cacheFile)
        try {
          IOUtils.copy(is, out)
          new FileInputStream(cacheFile)
        } finally { out.close(); is.close() }
      }
    }
  }
}
