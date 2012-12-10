/* shared javascript */

// Define the library UI global object
var LibraryUI = {};

// anonymous function for declaring library things in
// 'private' functions/variables are defined like:
//    var myPrivateFunction = function(args) { };
//
// 'public' functions are defined like:
//    LibraryUI.myPublicFunction = function(args) { };
//
(function(){

var statusText = function(text) {
  $("#statusText").html(text);
  console.log(text);
};

var doSearch = function(searchType, searchParam, order) {
  // console.log("inner search: ", searchType, searchParam, order);
  LibraryUI.searchType = searchType;
  LibraryUI.searchParam = searchParam;
  LibraryUI.order = order;

  // TODO: show loading spinner
  // clear past results
  deselectAllAssets();
  LibraryUI.assets = [];
  $("#results").empty();

  // call jquery etc
  jsRoutesAjax.controllers.LibraryService.search(searchType, searchParam, order)
  .ajax({
    success: function(data) {
      // console.log("search suceeded, data = ", data);      
      LibraryUI.assets = data.assets;
      // TODO: local search?
      LibraryUI.renderAssets(data.assets);
    },
    error: function(jqXHR, textStatus, errorThrown) {
      console.error("Search failed", textStatus, errorThrown);
    }
  });
};

// find a node to open in a jstree
var findNodeToOpen = function(nodeSpec, idPrefix, attribute, param) {
  var to_open = [];
  if (param) {
    $(nodeSpec).each(function(index) { 
      var nodeId = idPrefix + index;
      $(this).attr("id", nodeId);

      if ($(this).attr(attribute) === param) {
        to_open.push("#" + nodeId);
      } 
    });
  }

  return to_open;
}


// update the search location so it can be bookmarked
var updateSearchLocation = function(url) {
  if (LibraryUI.initalSearch === true) {
    LibraryUI.initalSearch = false;
  } else {
    window.history.replaceState("", "", url);
  }
};


// do an initial search when the page is first loaded
LibraryUI.init = function(defaultOrder, libraryLoadTime, treeCss, greyAsset, textSearch, keywordSearch, folderSearch, individualAsset, isAdmin) {
  // console.log("init", defaultOrder, libraryLoadTime, treeCss, textSearch, keywordSearch, folderSearch, individualAsset);

  // window.onpopstate = handlePopState;

  // set up state
  LibraryUI.order = defaultOrder;
  LibraryUI.loadTime = libraryLoadTime;
  LibraryUI.initalSearch = true;
  LibraryUI.greyAsset = greyAsset;
  LibraryUI.isAdmin = isAdmin;
  LibraryUI.assets = [];

  if (individualAsset === true) {
    // show asset
    LibraryUI.searchType = 'individual';
    LibraryUI.searchParam = individualAsset;

  } else if (folderSearch) {
    LibraryUI.searchType = 'folder';
    LibraryUI.searchParam = folderSearch;

  } else if (keywordSearch) {
    // seach will be triggered by tree select
    LibraryUI.searchType = 'keyword';
    LibraryUI.searchParam = keywordSearch;

  } else  {
    // default to show text-based search (even if its empty, shows all)
   LibraryUI.searchType = 'text';
   LibraryUI.searchParam = textSearch;
  }


  // setup trees & auto-open any we need to -- this also triggers the initial search!
  var folder_to_open = findNodeToOpen(".tree.folders li", "folder", "data-folder-path", folderSearch);
  var keyword_to_open = findNodeToOpen(".tree.keywords li", "keyword", "data-keyword", keywordSearch);
    
  // settings shared between trees
  var treeTheme = { 
      "icons" : false,
      "theme": "classic",
      "url": treeCss
  };
  var treeCoreSettings = {
      "animation": 100,
      "open_parents": true
  };

  // create trees
  $("#folderTree").jstree({
      core: treeCoreSettings,
      themes: treeTheme,
      ui: {
          "initially_select": folder_to_open
      }, 
      plugins: [ "themes",  "html_data", "ui" ]
  })
  .bind("select_node.jstree", function(e, data) {
    // search for folder
    if (data.rslt.obj) {
        LibraryUI.searchFolder(data.rslt.obj.attr("data-folder-path"));
        $(".tree.keywords").jstree("deselect_all");
    }
  }).show();

  $("#keywordTree").jstree({
      core: treeCoreSettings,
      themes: treeTheme,
      ui: {
          "initially_select": keyword_to_open
      }, 
      plugins: [ "themes",  "html_data", "ui" ]
  })
  .bind("select_node.jstree", function(e, data) {
    // search for keyword on select
    if (data.rslt.obj) {
      LibraryUI.searchKeyword(data.rslt.obj.attr("data-keyword"));
      $(".tree.folders").jstree("deselect_all");
    }   
  }).show();

  // set initial UI state
  updateUiState(0);

  // TODO: show individual asset
  if (LibraryUI.searchType === 'text') {
    // default to show everything!
    LibraryUI.searchAssets(textSearch);
  } 

};



LibraryUI.searchAssets = function(searchParam) {
  // console.log("search assets", searchParam);
  updateSearchLocation(jsRoutes.controllers.LibraryUI.index(searchParam, "", LibraryUI.order));
  doSearch('text', searchParam, LibraryUI.order);
};

LibraryUI.searchFolder = function(path) {
  // console.log("search folder", path);
  updateSearchLocation(jsRoutes.controllers.LibraryUI.listAssetsInFolder(path, LibraryUI.order));
  doSearch('folder', path, LibraryUI.order);
};

LibraryUI.searchKeyword = function(keyword) {
  // console.log("search keyword", keyword);
  updateSearchLocation(jsRoutes.controllers.LibraryUI.index("", keyword, LibraryUI.order));
  doSearch('keyword', keyword, LibraryUI.order);
};

LibraryUI.changeOrder = function(newOrder) {
  // console.log("changed order", newOrder);
  LibraryUI.order = newOrder;
  if (LibraryUI.searchType === 'folder') {
    LibraryUI.searchFolder(LibraryUI.searchParam);
  } else if (LibraryUI.searchType == 'keyword') {
    LibraryUI.searchKeyword(LibraryUI.searchParam);
  } else {
    LibraryUI.seachAssets(LibraryUI.searchParam);
  }
}

var getThumbnailPath = function(path) {
  return path.substr(0, path.lastIndexOf('.')) + "_thumbnail.jpg";
}

var imgSrcThumbnail = function(attr, asset) {
  if (asset.thum) {
    var thumbnailPath = getThumbnailPath(asset.path);
    if(LibraryUI.isAdmin) {
      return attr + '="' + jsRoutes.controllers.FileServer.serve(thumbnailPath) + '?bust=' + LibraryUI.loadTime + '"';
    } else {
      return attr + '="' + jsRoutes.controllers.FileServer.serve(thumbnailPath) + '"';
    }
  } else {
    // placeholder
    return attr + '="http://placehold.it/96x96" width="96" height="96"';
  } 
}

var renderAssetThumbnail = function(index, asset) {
  if (index < 64) {
    return '<img class="thumb" ' + imgSrcThumbnail("src", asset) + ' />';
  } else {
    // lazy load later thumbnails so we can return ALL the results and just load when scrolling 
    return '<img class="thumb lazy" src="' + LibraryUI.greyAsset + '"' +  imgSrcThumbnail("data-original", asset) + ' />'; 
  }
}

var displayName = function(name) {
  if (name.length > 16) {
    return name.substr(0,16) + "...";
  } else {
    return name;
  }
}

LibraryUI.renderAssets = function(assets) {
  // console.log("render assets");
  statusText(assets.length + " Assets found.");

  var results = $("#results");
  results.empty();
  $.each(assets, function(index,asset) {
    var name = asset.path.substr(asset.path.lastIndexOf('/') + 1);   

    var html = '<div class="asset pull-left">' +
      '<div class="inner-asset" data-original="' + asset.path + '" data-size-bytes="' + asset.size + '">' + 
      renderAssetThumbnail(index, asset) +
      '<p class="caption">' +
        '<a href="#" rel="tooltip" title="' + name + '" data-placement="bottom">' + displayName(name) + '</a>' +
      '</p></div></div>';

    results.append(html);
  });

  // set up tooltips on assets
  $(".inner-asset a").tooltip();

  // set up lazy loading on asset thumbnails - use delay to ensure it happens after other things
  $("img.lazy").lazyload({
     threshold : 200,
     event: "scrollstop"
  });

  // Hook up Click on assets: either select/deselect or go to individual view
  $(".inner-asset").click(onAssetClick);  
};

var onAssetClick = function(e) {
  var asset = $(this);

  // is select mode active?
  if ($("#selectModeBtn").hasClass("active")) {
    asset.toggleClass("selectedAsset");
    
    updateUiState($(".selectedAsset").length);

  } else {
    // look at single item
    // TODO: use AJAX
    window.location.href = jsRoutes.controllers.LibraryUI.showAsset(asset.attr("data-original"));
  }
};


var AssetArchiveDownloadLimit = 20;

var postJson = function(url, param) {
  return $.ajax(url, {
    type: 'POST',
    dataType: 'json',
    contentType : 'application/json',
    data: JSON.stringify(param),
    processData : false
  });
};

var disableBtn = function(element) { element.addClass("disabled"); };
var enableBtn = function(element) { element.removeClass("disabled"); };
var isBtnDisabled = function(element) { return element.hasClass("disabled"); };
var setEnabledBtn = function(element, isEnabled) { 
  if (isEnabled) {
    enableBtn(element);
  } else {
    disableBtn(element);
  }
};

var updateUiState = function(selectionCount) {
  if (selectionCount > 0) {
    statusText(selectionCount + " Assets selected.");
    enableBtn($("#deselectAllBtn"));
    enableBtn($("#massEditMetaBtn"));
    $("#downloadAllBtnLabel").html("Download Selection");
    setEnabledBtn($("#downloadAllBtn"), selectionCount <= AssetArchiveDownloadLimit);

  } else {
    var assetCount = $(".inner-asset").length;
    statusText(assetCount + " assets found. None currently selected.");
    disableBtn($("#deselectAllBtn"));
    disableBtn($("#massEditMetaBtn"));
    $("#downloadAllBtnLabel").html("Download All");
    setEnabledBtn($("#downloadAllBtn"), assetCount <= AssetArchiveDownloadLimit);
  }
};

var selectAllAssets = function() {
   var count = $(".inner-asset").addClass("selectedAsset").length;
  $("#selectModeBtn").addClass("active");
  updateUiState(count); 
};

var deselectAllAssets = function() {
  $(".selectedAsset").removeClass("selectedAsset");
  $("#selectModeBtn").removeClass("active");
  updateUiState(0);
};


// set up UI when its loaded - mainly onclick functions
jQuery(document).ready(function() {

  $("#searchForm").submit(function(e) {
    e.preventDefault();
    var textSearch = $("#search").val();
    if (textSearch) {
     LibraryUI.searchAssets(textSearch);
    }
    return false;
  });

  $("#everything").click(function(e) {
    LibraryUI.searchAssets('');
  });

  $("#selectAllBtn").click(selectAllAssets);
  $("#deselectAllBtn").click(deselectAllAssets);

  // Mass edit of assets
  $("#massEditMetaBtn").click(function(e) {
    e.stopPropagation();
    if (isBtnDisabled($(this))) return;    
    // populate the list of assets to edit

    var options = "";

    $(".selectedAsset").each(function() {
        options += '<option>' + $(this).attr("data-original") + '</option>';
    });

    $("#massEditAsset .btn").show(); 
    $("#massEditAssetList").html("").append(options);
    $("#massEditAsset").modal();
  });

  $("#massEditMetaSubmitBtn").click(function(e) {
    e.stopPropagation();
    if (isBtnDisabled($(this))) return;

    // submit ajax query
    var assets = $(".selectedAsset").map(function() { return $(this).attr('data-original'); }).toArray();
    var queryParameters = {
      "addKeywords": $("#addKeywords").val(),
      "removeKeywords": $("#removeKeywords").val(),
      "assets": assets
    };

    // prevent multiple submits
    $("#massEditAsset .btn").hide(); 
    $("#massEditMetaProgress").html("Working...");
    
    // send query
    var reloadAfter = function() { location.reload(); }
    postJson(jsRoutes.controllers.Admin.massEditMetadata(), queryParameters)
    .done(reloadAfter).fail(reloadAfter);
  });

  var getAssetsToDownload = function() {
    var assets = $(".selectedAsset");

    if (assets.length === 0) {
      // download all if no selection
      return $(".inner-asset");
    }
    return assets;
  };

  // mass download of assets
  $("#downloadAllBtn").click(function(e) {
    e.stopPropagation();
    if (isBtnDisabled($(this))) return;

    var assets = getAssetsToDownload();

    if (assets.length === 0) {
      alert("Nothing to download!");
      return;

    } else if (assets.length === 1) {
      // exactly one asset, download it
      assets.each(function() {
        window.location.href = jsRoutes.controllers.FileServer.serve($(this).attr("data-original"));
      });
      return;

    } else if (assets.length > AssetArchiveDownloadLimit) {

      alert("Too many assets! You can only download " + AssetArchiveDownloadLimit + " at a time, apologies.");
      return;
    }

    var options = "";
    var totalSizeMB = 0.0;

    assets.each(function() {
        options += '<option>' + $(this).attr("data-original") + '</option>';
        totalSizeMB += ($(this).attr("data-size-bytes") / (1024 * 1024));
    });

    // TODO: limit number or total size of files that can be downloaded in one go?
    //       or provide warning?

    totalSizeMB = Math.floor(0.5 + totalSizeMB);

    $("#massDownloadAssetLabel").html(assets.length + " Assets, approx " + totalSizeMB + " MB before archiving");
    $("#massDownload .btn").show(); 
    $("#massDownloadAssetList").html("").append(options);
    $("#massDownloadProgress").html("");
    $("#massDownload").modal();
  });

  $("#massDownloadSubmitBtn").click(function(e) {
    e.stopPropagation();
    if (isBtnDisabled($(this))) return;

    // submit ajax query
    var assets = getAssetsToDownload();
    var assetPaths = assets.map(function() { return $(this).attr('data-original'); }).toArray();
    var queryParameters = {     
      "assets": assetPaths
    };

    // prevent multiple submits
    $("#massDownload .btn").hide(); 
    $("#massDownloadProgress").html("Building archive of " + assets.length + " assets, please wait...");
    
    // send query
    postJson(jsRoutes.controllers.ArchiveBuilder.archive(), queryParameters)
    .done(function(data) {
        $('#massDownload').modal('hide');
        window.location = data.archive;

    }).fail(function(jqXHR, textStatus, errorThrown) {
        $('#massDownload').modal('hide');
        alert( "Archive Build failed: " + textStatus + " " + errorThrown);
    });
  });

}); //jquery.docready
  
})(); // close and call anonymous function
