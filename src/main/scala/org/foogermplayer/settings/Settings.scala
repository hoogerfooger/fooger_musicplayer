package org.foogermplayer.settings

import org.apache.commons.codec.binary.Base64
import play.api.libs.json._
import scala.util.Try
import org.foogermplayer.utils.{FileHandling, JvmFileHandling}
import org.foogermplayer.controller.{PlayerFiles, PlayListFile}

case class Settings(playlistLocation:String)

object Settings extends PlayerFiles {
  val default = PlayListFile.defaultLoc.map(Settings(_))

  val jsPlaylistName = "playlistLocation"
  import Json._
  import Reads._
  import Writes._

  implicit val format = new Format[Settings] {
    def reads(json: JsValue): JsResult[Settings] = (json \ jsPlaylistName).validate[String].map { pl =>
      Settings(new String(Base64.decodeBase64(pl)))
    }

    def writes(o: Settings): JsValue =
      obj((jsPlaylistName,new String(Base64.encodeBase64(o.playlistLocation.getBytes))))
  }

  val settingsFileName = "settings.json"

  def save(settings:Settings)(implicit fileHandling:FileHandling = JvmFileHandling):Try[Unit] =
    defaultLocation(settingsFileName).flatMap(loc => save(loc,settings))

  def open(implicit fileHandling:FileHandling = JvmFileHandling):Try[Settings] =
    defaultLocation(settingsFileName).flatMap(loc => open[Settings](loc))


  def openOrDefault(implicit fileHandling:FileHandling = JvmFileHandling):Try[Settings] =
    defaultLocation(settingsFileName).flatMap(loc => open[Settings](loc)) orElse default

}


