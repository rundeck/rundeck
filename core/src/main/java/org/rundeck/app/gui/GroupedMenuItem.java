package org.rundeck.app.gui;

public interface GroupedMenuItem
        extends MenuItem
{
    default String getGroupId() {
        return null;
    }

    default String getGroupTitle() {
        return null;
    }

    default String getGroupTitleCode() {
        return null;
    }

    public static String groupId(MenuItem item){
        if(item instanceof GroupedMenuItem){
            return ((GroupedMenuItem) item).getGroupId();
        }
        return null;
    }

    public static String groupTitle(MenuItem item){
        if(item instanceof GroupedMenuItem){
            return ((GroupedMenuItem) item).getGroupTitle();
        }
        return null;
    }
    public static String groupTitleCode(MenuItem item){
        if(item instanceof GroupedMenuItem){
            return ((GroupedMenuItem) item).getGroupTitleCode();
        }
        return null;
    }
}
