package models

import util.AuthenticatedUser

/** 
 * define what one user can see in the Library -- if no Authenticated User, assume auth is off so see everything!
 */
case class UserView (library: AssetLibrary, user: Option[AuthenticatedUser]) {

  lazy val isEverythingVisible: Boolean = user match {
    case Some(authUser) => (visibleGroupIds.size == library.groups.size)
    case None => true
  }

  lazy val topFolders: List[AssetFolder] = filterFolders(library.topFolder.folders)

  lazy val keywords: List[String] = if (isEverythingVisible) library.keywords else userKeywords
  
  private def userKeywords = { 
    topFolders.foldLeft (Set[String]()) {
      case (set, folder) => set ++ library.keywordsByTopFolder(folder)
    } }.toList.sortBy(_.toLowerCase)

  /** note: group IDs can change with each Library reload **/
  private lazy val visibleGroupIds: Set[Int] = user match {
    case Some(authUser) => authUser.groups.flatMap { group => library.groups.get(group.toLowerCase) }.toSet
    case _ => Set()
  }

  def filterAssets(assets: Seq[Asset]) = if (isEverythingVisible) assets else assets.filter { asset => visibleGroupIds.contains(asset.group) }

  def filterFolders(folders: List[AssetFolder]) = if (isEverythingVisible) folders else folders.filter { folder => visibleGroupIds.contains(folder.group) }
}
