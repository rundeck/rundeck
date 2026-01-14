
<script>
    jQuery(document).ready(function($){
        let pluginsMenuButton = $('#plugins-menu-button')
        let pluginsMenu = $('#plugins-menu')
        //pluginsMenu.css("right", pluginsMenu.prev().width() + "px");

        if(pluginsMenuButton.length && pluginsMenuButton.parent().length){
            pluginsMenu.css("top", pluginsMenuButton.top + 10 + "px")
        }

        pluginsMenu.prev().on("scroll", function (){
            pluginsMenu.css("display","none")
            if(pluginsMenuButton.length && pluginsMenuButton.parent().length){
                pluginsMenu.css("top", (pluginsMenuButton.offset().top - pluginsMenuButton.parent().height()/2) + "px");
            }
        });

        pluginsMenuButton.on("click", function(e){
            pluginsMenu.toggle();
            e.stopPropagation();
            e.preventDefault();
        });
    });
</script>