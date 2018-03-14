package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A tile map layer getting tiles resolve Stamen servers.
 * @author Luke Bermingham
 */
public class StamenTonerBaseMapLayer extends BasicMercatorTiledImageLayer {

    public StamenTonerBaseMapLayer() {
        super(makeLevels());
        setUseMipMaps(true);
        setSplitScale(1.5);
    }

    private static LevelSet makeLevels()
    {
        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 256);
        params.setValue(AVKey.TILE_HEIGHT, 256);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/Stamen-Toner");
        //params.setValue(AVKey.SERVICE, "https://cartodb-basemaps-a.global.ssl.fastly.net/dark_all/");
        params.setValue(AVKey.SERVICE, "http://a.tile.stamen.com/toner/");
        params.setValue(AVKey.DATASET_NAME, "*");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
        params.setValue(AVKey.NUM_LEVELS, 16);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle
                .fromDegrees(22.5d), Angle.fromDegrees(45d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());
        params.setValue(AVKey.DETAIL_HINT, 1d);




        return new LevelSet(params);
    }

    private static class URLBuilder implements TileUrlBuilder
    {
        public URL getURL(Tile tile, String imageFormat)
                throws MalformedURLException
        {
            URL url = new URL(tile.getLevel().getService()
                    + (tile.getLevelNumber() + 3) + "/" + tile.getColumn() + "/"
                    + ((1 << (tile.getLevelNumber()) + 3) - 1 - tile.getRow()) + ".png");
            return url;
        }
    }

    @Override
    public String toString()
    {
        return "Stamen Toner";
    }




}
