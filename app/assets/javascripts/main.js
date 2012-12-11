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

 var spinnerOptions = { lines:15, length:24, width:8, radius:40, trail:25, speed:0.8, top:50, left:'auto' };

var statusText = function(text) {
  $("#statusText").html(text);
  console.log(text);
};

var updateOrderText = function() {
  var text = "A->Z";

  if (LibraryUI.order === "time")       { text = "Oldest"; }
  else if (LibraryUI.order === "-time") { text = "Newest"; }
  else if (LibraryUI.order === "size")  { text = "Smallest"; }
  else if (LibraryUI.order === "-size") { text = "Largest"; }
  else if (LibraryUI.order === "-name") { text = "Z->A"; }

  $("#currentOrderText").html(text);
}

var doSearch = function(searchType, searchParam, order) {
  // console.log("inner search: ", searchType, searchParam, order);
  statusText("Searching...");
  LibraryUI.searchType = searchType;
  LibraryUI.searchParam = searchParam;
  LibraryUI.order = order;
  updateOrderText();

  // clear past results
  deselectAllAssets();
  LibraryUI.assets = [];
  $("#results").empty();
  $("#results").spin(spinnerOptions);

  // call jquery etc
  jsRoutesAjax.controllers.LibraryService.search(searchType, searchParam, order)
  .ajax({
    success: function(data) {
      // console.log("search suceeded, data = ", data);      
      LibraryUI.assets = data.assets;
      // TODO: local sort
      $("#results").spin(false);
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

  initUI();

  if (individualAsset) {
    // show asset
    LibraryUI.showIndividualAsset(individualAsset, -1);

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
  updateSearchLocation(jsRoutes.controllers.LibraryUI.index(searchParam, "", LibraryUI.order));
  doSearch('text', searchParam, LibraryUI.order);
};

LibraryUI.searchFolder = function(path) {
  updateSearchLocation(jsRoutes.controllers.LibraryUI.listAssetsInFolder(path, LibraryUI.order));
  doSearch('folder', path, LibraryUI.order);
};

LibraryUI.searchKeyword = function(keyword) {
  updateSearchLocation(jsRoutes.controllers.LibraryUI.index("", keyword, LibraryUI.order));
  doSearch('keyword', keyword, LibraryUI.order);
};

LibraryUI.changeOrder = function(newOrder) {
  LibraryUI.order = newOrder;
  if (LibraryUI.searchType === 'folder') {
    LibraryUI.searchFolder(LibraryUI.searchParam);
  } else if (LibraryUI.searchType == 'keyword') {
    LibraryUI.searchKeyword(LibraryUI.searchParam);
  } else {
    LibraryUI.searchAssets(LibraryUI.searchParam);
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
  statusText(assets.length + " Assets found.");

  var results = $("#results");
  results.empty();
  $.each(assets, function(index,asset) {
    var name = asset.path.substr(asset.path.lastIndexOf('/') + 1);   

    var html = '<div class="asset pull-left">' +
      '<div class="inner-asset" data-index="' + index + '" data-original="' + asset.path + '" data-size-bytes="' + asset.size + '">' + 
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
    LibraryUI.showIndividualAsset(asset.attr("data-original"), asset.attr("data-index")); 
  }
};

var ensureNotEmpty = function(s) {
  if (s === "")
    return "&nbsp;";
  else
    return s;
}

var onDetailPanelClosed = function() {
  // restore URL when individual panel is shut
  if (LibraryUI.locationBeforeModal) {
    updateSearchLocation(LibraryUI.locationBeforeModal);
    LibraryUI.locationBeforeModal = '';
  }
}

LibraryUI.showIndividualAsset = function(path, index, navigatingList) {

  navigatingList = navigatingList || false;

  // LibraryUI.searchType = 'individual';
  // LibraryUI.searchParam = individualAsset;

  // store previous url so we can restore it after closing the modal
  if (navigatingList === false) {
    LibraryUI.locationBeforeModal = window.location.href;
    updateSearchLocation(jsRoutes.controllers.LibraryUI.showAsset(path));
  }

  statusText("Loading asset details...");
  $("#adTitle").html("Loading...");
  $("#adDetails").hide(); 
  $("#adFooter").hide();
  $("#eaFooter").hide();
  $("#adLoading").spin({top:20, left:20}); 
  $("#adLoading").show();
  if (LibraryUI.isAdmin) {
    $("#adEditAsset").hide();
  }
  if (navigatingList === false) {
    $("#assetDetailPanel").modal('show');    
  }

  LibraryUI.currentIndex = index;
  if (index >= 0) {
    $("#adForwardBack").show();
  } else {
    $("#adForwardBack").hide();
  }

  jsRoutesAjax.controllers.LibraryService.getAsset(path)
  .ajax({
    success: function(data) {

      statusText("Got asset details");
      var asset = data.asset;
      LibraryUI.currentAsset = asset;

      // fill in asset details
      var imgSrc = "http://placehold.it/320x320";
      if (asset.hasPreview) {
        imgSrc = jsRoutes.controllers.FileServer.serve(asset.preview);
      }      
      $("#adPreview").attr('src', imgSrc);

      $("#adTitle").html(asset.name);
      $("#adDescription").html(ensureNotEmpty(asset.description));

      var extension = asset.path.substr(asset.path.lastIndexOf('.') + 1).toUpperCase();
      $("#adType").html(ensureNotEmpty(extension));

      // TODO: handle clicks on path or keywords
      var assetFolder = asset.path.substr(0, asset.path.lastIndexOf('/') + 1);
      $("#adFolder").html(assetFolder)
        .attr("href", jsRoutes.controllers.LibraryUI.listAssetsInFolder(assetFolder, LibraryUI.order));

      $("#adSize").html(ensureNotEmpty(asset.size));
      var keywordLinks = $("#adKeywords");
      keywordLinks.empty();
      $.each(asset.keywords, function(index, keyword) {
          keywordLinks.append('<a class="keyword" href="' + 
            jsRoutes.controllers.LibraryUI.index("", keyword, LibraryUI.order) +
            '">' + keyword + '</a>, ');
      });
      keywordLinks.append("&nbsp;");
      $("a.keyword").click(assetkeywordClicked);

      $("#adLoading").spin(false);
      $("#adLoading").hide();
      $("#adDetails").show();
      $("#adFooter").show(); 
    },
    error: function(jqXHR, textStatus, errorThrown) {
      console.error("Load failed", textStatus, errorThrown);
      $("#assetDetailPanel").modal('hide');
    }
  });
};

var assetNextClicked = function(e) {
  if (LibraryUI.assets && LibraryUI.currentIndex) {
    var newIndex = Number(LibraryUI.currentIndex) + 1;
    var length = LibraryUI.assets.length

    if (newIndex < length) {
      console.log("calling sia", LibraryUI.assets[newIndex].path);
      LibraryUI.showIndividualAsset(LibraryUI.assets[newIndex].path, newIndex, true);
    }
  }
};

var assetPreviousClicked = function(e) {
  if (LibraryUI.assets && LibraryUI.currentIndex) {
    var newIndex = Number(LibraryUI.currentIndex) - 1;
    if (newIndex >= 0) {
      LibraryUI.showIndividualAsset(LibraryUI.assets[newIndex].path, newIndex, true);
    }
  }
};

var assetkeywordClicked = function(e) {
  $("#assetDetailPanel").modal('hide');

  var keyword = $(this).text();
  e.preventDefault();
  LibraryUI.searchKeyword(keyword);  
}

var assetFolderClicked = function(e) {
  $("#assetDetailPanel").modal('hide');

  e.preventDefault();
  var asset = LibraryUI.currentAsset;
  var assetFolder = asset.path.substr(0, asset.path.lastIndexOf('/') + 1);

  LibraryUI.searchFolder(assetFolder);
}

var downloadAssetClicked = function(e) {
  e.preventDefault();

  if (LibraryUI.currentAsset) {
    // TODO: open in 'new' window
    window.location.href = jsRoutes.controllers.FileServer.serve(LibraryUI.currentAsset.path); 
  }
}

var editAssetMetaClicked = function(e) {
  e.preventDefault();

  if (LibraryUI.currentAsset) {
    var asset = LibraryUI.currentAsset;

    // show the edit parts of the dialog, hide the rest
    $("#adEditAsset").show();
    $("#eaFooter").show();
    $("#adLoading").hide();
    $("#adFooter").hide();    
    $("#adDetails").hide();

    $("#eaTitle").html(asset.name);
    $("#eaForm").attr("action", jsRoutes.controllers.Admin.editMetadata(asset.path));
    $("#eaForm #description").val(asset.description);
    $("#eaForm #keywords").val(asset.keywords.toString());
  }
}


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

var submitSearchForm = function(e) {
  e.preventDefault();
  var textSearch = $("#search").val();
  if (textSearch) {
   LibraryUI.searchAssets(textSearch);
  }
  return false;
};

var showEverythingClicked = function(e) {
  LibraryUI.searchAssets('');
};  

var massEditMetaClicked = function(e) {
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
};

var massEditMetaSubmitClicked = function(e) {
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
};

var getAssetsToDownload = function() {
  var assets = $(".selectedAsset");

  if (assets.length === 0) {
    // download all if no selection
    return $(".inner-asset");
  }
  return assets;
};

var downloadAllButtonClicked = function(e) {
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
};

var downloadSubmitButtonClicked = function(e) {
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
};

var orderChangeMenuItemClicked = function(e) {
  e.preventDefault();
  var newOrder = $(this).attr("data-ordering");
  LibraryUI.changeOrder(newOrder);
};

// set up UI when its loaded - mainly onclick functions
var initUI = function() {

  $("#searchForm").submit(submitSearchForm);
  $("#everything").click(showEverythingClicked);

  $("#selectAllBtn").click(selectAllAssets);
  $("#deselectAllBtn").click(deselectAllAssets);

  $("#downloadAllBtn").click(downloadAllButtonClicked);
  $("#massDownloadSubmitBtn").click(downloadSubmitButtonClicked);
  $(".orderChangeMenuItem").click(orderChangeMenuItemClicked);

  $("#adDownload").click(downloadAssetClicked);
  $("#assetDetailPanel").modal({show: false}).on('hidden', onDetailPanelClosed);    
  $("#adFolder").click(assetFolderClicked);
  $("#adPrevious").click(assetPreviousClicked);
  $("#adNext").click(assetNextClicked);

  if (LibraryUI.isAdmin) {
    $("#adEditMeta").click(editAssetMetaClicked);

    $("#massEditMetaBtn").click(massEditMetaClicked);
    $("#massEditMetaSubmitBtn").click(massEditMetaSubmitClicked);
  }
}; 
  
})(); // close and call anonymous function
