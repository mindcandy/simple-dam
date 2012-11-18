/* shared javascript */
jQuery(document).ready(function($) {
    $(".tree").jstree({
        core: {
            "animation": 100
        },
        themes: { 
            "icons" : false,
            "theme": "classic",
            "url": "web/tree-themes/classic/style.css"
        },
        plugins: [ "themes", "html_data" ]
    });
});
