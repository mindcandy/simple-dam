package models

/**
 * track a Folder of assets
 */
case class AssetFolder(name: String, assets: List[Asset], folders: List[AssetFolder])

