/* shared javascript */

/* set up function */
jQuery(document).ready(function() {

    $(".inner-asset a").tooltip();

    $("img.lazy").lazyload({
       threshold : 200,
       event: "scrollstop"
    });
});
