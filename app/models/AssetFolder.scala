package models

/**
 * track a Folder of assets
 */
case class AssetFolder(name: String, assets: List[Asset], folders: List[AssetFolder])


object AssetFolder {

  /**
   * an empty folder
   */
  val Empty = AssetFolder("", List(), List())
}