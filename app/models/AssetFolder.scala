package models

/**
 * track a Folder of assets
 */
case class AssetFolder(name: String, assets: List[Asset], folders: List[AssetFolder], group: Int) {

  /**
   * recurse through all assets
   */
  def allAssetsUnsorted: List[Asset] = assets ++ folders.flatMap( _.allAssetsUnsorted )

  /**
   * get all assets in this folder and below - but sorted!
   * note, if doing this for entire library this is cached for filtering
   */
  def allAssets: List[Asset] = allAssetsUnsorted.sortBy(_.nameLower)


  /**
   * find assets that match the given search 
   * will AND together terms separated by spaces
   */
  def findAssets(search: String): List[Asset] = {    
    allAssets.filter(_.matches(AssetLibrary.getSearchTerms(search)))
  }


  /** 
   * find by keyword
   */
  def findAssetsByKeyword(keyword: String): List[Asset] = {
    allAssets.filter(_.keywords.contains(keyword) )
  }

}


object AssetFolder {

  /**
   * an empty folder
   */
  val Empty = AssetFolder("", List(), List(), 0)
}