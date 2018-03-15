package onethreeseven.trajsuite.core.view;

import javafx.stage.Stage;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.view.*;

/**
 * The menu commands the core program can supply.
 * @author Luke Bermingham
 */
public class CoreMenusSupplier implements MenuSupplier {


    @Override
    public void supplyMenus(AbstractMenuBarPopulator menuBarPopulator, BaseTrajSuiteProgram program, Stage stage) {

        TrajSuiteMenu aboutMenu = new TrajSuiteMenu("About", 100);
        TrajSuiteMenuItem helpItem = new TrajSuiteMenuItem("Help", ()->{
            ViewUtil.loadUtilityView(CoreMenusSupplier.class,
                    stage,
                    "",
                    "/onethreeseven/trajsuite/core/view/about.fxml");
        });
        aboutMenu.addChild(helpItem);

        menuBarPopulator.addMenu(aboutMenu);

    }
}
