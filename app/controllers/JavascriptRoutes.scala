package controllers

import play.api._
import play.api.mvc._
import play.core.Router._

/**
 * utility javascript router
 */
object JavascriptRoutes extends Controller {

    /**
     * generate javascript routes
     */
    def generateRoutes = Action { implicit request =>
      import routes.javascript._
      val ajaxRoutes = Routes.javascriptRouter("jsRoutes")(
          Application.index,
          Application.findFolder
          // add any other endpoints you want to use in Javascript here
        )

      val assetRoutes = generateAssetRoutes("jsRoutes")(
        FileServer.serve
        // add any other static asset routes for Javascript here
        )

      val combined = ajaxRoutes.trim() + ";\n" + assetRoutes

      Ok(combined).as("text/javascript")
    }

    /**
     * generate routes for static assets to be used in url/hrefs
     */
    private def generateAssetRoutes(name: String)(routes: JavascriptReverseRoute*): String = {

      """|(function(_root){
             |var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g};
             |var _wA = function(r){return r.url};
             |%s
             |})(%s);
          """.stripMargin.format(
        routes.map { route =>
          "_nS('%s'); _root.%s = %s;".format(
            route.name.split('.').dropRight(1).mkString("."),
            route.name,
            route.f)
        }.mkString("\n"),
        name)
    }
}