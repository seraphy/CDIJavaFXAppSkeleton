#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.common;

import javafx.scene.control.MenuBar;

public interface MainMenuCustomizer {

	void createCustomizeMenu(MenuBar menuBar);

	void removeCustomizeMenu(MenuBar menuBar);

}
