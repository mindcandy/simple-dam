package models

/**
 * track a Folder of assets
 */
case class AssetFolder(name: String, assets: List[Asset], folders: List[AssetFolder]) {

  /**
   * recurse through all assets
   */
  def allAssetsUnsorted: List[Asset] = assets ++ folders.flatMap( _.allAssetsUnsorted )

  /**
   * get all assets in this folder and below - but sorted!
   * note, if doing this for entire library this is cached for filtering
   */
  def allAssets: List[Asset] = allAssetsUnsorted.sortBy(_.nameLower)

}


object AssetFolder {

  /**
   * an empty folder
   */
  val Empty = AssetFolder("", List(), List())
}