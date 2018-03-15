import onethreeseven.trajsuite.core.model.TrajsuiteProgramSupplier;
import onethreeseven.trajsuitePlugin.model.ProgramSupplier;

module onethreeseven.trajsuite.core {
    requires java.logging;
    requires javafx.swing;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires java.desktop;
    requires onethreeseven.collections;
    requires onethreeseven.geo;
    requires onethreeseven.datastructures;
    requires onethreeseven.jclimod;
    requires onethreeseven.trajsuitePlugin;
    requires onethreeseven.roi;
    requires onethreeseven.spm;
    requires onethreeseven.stopmove;
    requires onethreeseven.common;
    requires onethreeseven.simplification;

    requires graphhopper.core;
    requires graphhopper.map.matching.core;
    requires graphhopper.reader.osm;
    requires jogl.all.fat;
    requires gluegen.rt.fat;
    requires hppc;
    requires jts;
    requires osmosis.core;
    requires osmosis.pbf2;
    requires osmosis.xml;
    requires jcommander;


    //subject to module upgrade when I get around to it
    requires ww.core;

    //for fxml resources
    exports onethreeseven.trajsuite.core.view;
    opens onethreeseven.trajsuite.core.view;

    //exports
    exports onethreeseven.trajsuite.core;
    exports onethreeseven.trajsuite.core.algorithm;
    exports onethreeseven.trajsuite.core.model;
    exports onethreeseven.trajsuite.core.util;
    exports onethreeseven.trajsuite.osm.data;
    exports onethreeseven.trajsuite.osm.model.markov;
    exports onethreeseven.trajsuite.osm.util;
    exports onethreeseven.trajsuite.core.graphics;

    opens onethreeseven.trajsuite.core.view.controller to javafx.fxml;
    exports onethreeseven.trajsuite.core.view.controller to javafx.fxml;

    //for consuming menus from plugins
    uses onethreeseven.trajsuitePlugin.view.MenuSupplier;
    provides onethreeseven.trajsuitePlugin.view.MenuSupplier with onethreeseven.trajsuite.core.view.CoreMenusSupplier;

    uses ProgramSupplier;
    provides ProgramSupplier with TrajsuiteProgramSupplier;

}