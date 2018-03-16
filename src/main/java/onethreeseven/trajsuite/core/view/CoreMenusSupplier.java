package onethreeseven.trajsuite.core.view;

import javafx.stage.Stage;
import onethreeseven.trajsuite.core.model.TrajsuiteLayers;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.Layers;
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


        TrajSuiteMenu viewMenu = new TrajSuiteMenu("View", 7);
        TrajSuiteMenuItem mapLayersItem = new TrajSuiteMenuItem("Map Layers", ()->{
            Layers layers = program.getLayers();
            if(layers instanceof TrajsuiteLayers){
                ViewUtil.loadUtilityView(stage, "Map Layers",
                        new MapLayerView(((TrajsuiteLayers) layers).getRenderableLayers()));
            }
        });
        viewMenu.addChild(mapLayersItem);

        menuBarPopulator.addMenu(aboutMenu);
        menuBarPopulator.addMenu(viewMenu);

    }
}
