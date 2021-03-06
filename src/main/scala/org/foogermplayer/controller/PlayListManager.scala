package org.foogermplayer.controller

import scala.util.{Try, Success}
import play.api.libs.json._
import org.apache.commons.codec.binary.Base64
import org.foogermplayer.settings.Settings
import org.foogermplayer.utils.{JvmFileHandling, FileHandling}

case class PlayList(files:List[String])

object PlayList {
  val jsFiles = "items"

  import Json._
  import Writes._

  implicit val format = new Format[PlayList] {
    def reads(json: JsValue): JsResult[PlayList] = (json \ jsFiles).validate[List[String]].map(PlayList(_)).map { pl =>
      pl.copy(files = pl.files.map(s => new String(Base64.decodeBase64(s))))
    }

    def writes(o: PlayList): JsValue = obj(jsFiles -> o.files.map(s => new String(Base64.encodeBase64(s.getBytes))))
  }
}

case class PlayListFile(location:String,playlist:PlayList)

object PlayListFile extends PlayerFiles {
  val defaultLoc = defaultLocation(PlayListManager.defaultPlaylistName)
}

object PlayListManager extends PlayerFiles {
  val defaultPlaylistName = "scfx-def-playlist.playlist"

  def saveToDefault(playlist:PlayList)(implicit fileHandling:FileHandling = JvmFileHandling):Try[Unit] =
    defaultLocation(defaultPlaylistName).flatMap(loc => save(loc,playlist))

  def openDefault(implicit fileHandling:FileHandling = JvmFileHandling):Try[PlayList] =
    defaultLocation(defaultPlaylistName).flatMap(loc => open[PlayList](loc))

  def openFromSettings(implicit fileHandling:FileHandling = JvmFileHandling):Try[PlayListFile] = for {
    settings <- Settings.open.transform(
     s => Success(s),
     f => {
       Settings.default.map(Settings.save)
       Settings.default
     }
    )
    loc <- location(settings.playlistLocation).transform(
      s => Success(s),
      f => {
        Settings.default.map(Settings.save)
        PlayListFile.defaultLoc
      }
    )
    playlist <- open[PlayList](loc)
  } yield PlayListFile(loc,playlist)


  def saveInSettings(plFile:PlayListFile)(implicit fileHandling:FileHandling = JvmFileHandling):Try[Unit] = for {
    settings <- Settings.openOrDefault
    _ <- save(plFile.location,plFile.playlist)
    _ <- Settings.save(settings.copy(playlistLocation = plFile.location))
  } yield ()


  def saveCurrent(playList:PlayList)(implicit fileHandling:FileHandling = JvmFileHandling):Try[Unit] = for {
    settings <- Settings.openOrDefault
    _ <- save(settings.playlistLocation,playList)
  } yield ()
}
