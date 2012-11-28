/* shared javascript */

/* set up function */
jQuery(document).ready(function() {


  var updateUiState = function(selectionCount) {
    if (selectionCount > 0) {
      $("#deselectAllBtn").removeClass("disabled");
      $("#massEditMetaBtn").removeClass("disabled");
      $("#downloadAllBtnLabel").html("Download Selection");
      $("#statusText").html(selectionCount + " Assets selected.");
    } else {
      $("#deselectAllBtn").addClass("disabled");
      $("#massEditMetaBtn").addClass("disabled");
      $("#downloadAllBtnLabel").html("Download All");
      $("#statusText").html("No assets selected.");
    }
  };

  $("#selectAllBtn").click(function(e) {
    var count = $(".inner-asset").addClass("selectedAsset").length;
    $("#selectModeBtn").addClass("active");
    updateUiState(count);
  });

  $("#deselectAllBtn").click(function(e) {
    $(".selectedAsset").removeClass("selectedAsset");
    $("#selectModeBtn").removeClass("active");
    updateUiState(0);
  });

  // either select/deselect or go to individual view
  $(".inner-asset").click(function(e) {

    var asset = $(this);

    // is select mode active?
    if ($("#selectModeBtn").hasClass("active")) {
      asset.toggleClass("selectedAsset");
      
      updateUiState($(".selectedAsset").length);

    } else {
      // look at single item
      // TODO: use jsrouting?
      window.location.href = "/asset/" + asset.attr("data-original");
    }

  });

  $("#massEditMetaBtn").click(function(e) {
    // populate the list of assets to edit
    var options = "";

    $(".selectedAsset").each(function() {
        options += '<option selected>' + $(this).attr("data-original") + '</option>';
    });

    $("#massEditAssetList").html("").append(options);
    $("#massEditAsset").modal();
  })

  $("#downloadAllBtn").click(function(e) {
    var assets = $(".selectedAsset");

    if (assets.length === 0) {
      // download all if no selection
      assets = $(".inner-asset");
    }

    if (assets.length === 0) {
      alert("Nothing to download!");
      return;

    } else if (assets.length === 1) {
      // exactly one asset, download it
      assets.each(function() {
        window.location.href = jsRoutes.controllers.FileServer.serve($(this).attr("data-original"));
      });
      return;
    }

    var options = "";
    var totalSizeMB = 0.0;

    assets.each(function() {
        options += '<option selected>' + $(this).attr("data-original") + '</option>';
        totalSizeMB += ($(this).attr("data-size-bytes") / (1024 * 1024));
    });

    // TODO: limit number or total size of files that can be downloaded in one go?
    //       or provide warning?

    totalSizeMB = Math.floor(0.5 + totalSizeMB);
    $("#massDownloadAssetLabel").html(assets.length + " Assets, approx " + totalSizeMB + " MB");
    $("#massDownloadAssetList").html("").append(options);
    $("#massDownload").modal();
  });

});
  
