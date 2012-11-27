/* shared javascript */

/* set up function */
jQuery(document).ready(function() {

    $(".inner-asset a").tooltip();

    $("img.lazy").lazyload({
       threshold : 200,
       event: "scrollstop"
    });

  var setButtonStates = function(anySelected) {
    if (anySelected) {
      $("#deselectAllBtn").removeClass("disabled");
      $("#massEditMetaBtn").removeClass("disabled");

    } else {
      $("#deselectAllBtn").addClass("disabled");
      $("#massEditMetaBtn").addClass("disabled");

    }
  };

  $("#selectAllBtn").click(function(e) {
    $(".inner-asset").addClass("selectedAsset");
    $("#selectModeBtn").addClass("active");
    setButtonStates(true);
  });

  $("#deselectAllBtn").click(function(e) {
    $(".selectedAsset").removeClass("selectedAsset");
    $("#selectModeBtn").removeClass("active");
    setButtonStates(false);
  });

  // either select/deselect or go to individual view
  $(".inner-asset").click(function(e) {

    var asset = $(this);

    // is select mode active?
    if ($("#selectModeBtn").hasClass("active")) {
      asset.toggleClass("selectedAsset");

      // TODO: track how many selected items or just count them
      setButtonStates(true);

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
    alert("Mass-download is not implemented yet, sorry!");
  });
});
  