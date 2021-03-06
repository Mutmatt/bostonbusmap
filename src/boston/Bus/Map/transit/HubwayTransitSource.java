package boston.Bus.Map.transit;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.HubwayStopLocation;
import boston.Bus.Map.data.IAlerts;
import boston.Bus.Map.data.Location;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.PredictionStopLocationPair;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.Selection;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.TransitDrawables;
import boston.Bus.Map.data.TransitSourceTitles;
import boston.Bus.Map.database.Schema;
import boston.Bus.Map.parser.HubwayParser;
import boston.Bus.Map.util.DownloadHelper;
import boston.Bus.Map.util.SearchHelper;

/**
 * Created by schneg on 9/1/13.
 */
public class HubwayTransitSource implements TransitSource {
	public static final String stopTagPrefix = "hubway_";
	private static final String routeTag = "Hubway";
	private static final String dataUrl = "http://www.thehubway.com/data/stations/bikeStations.xml";

	private final TransitDrawables drawables;
	private final TransitSourceTitles routeTitles;
	private final TransitSystem transitSystem;


	public HubwayTransitSource(TransitDrawables drawables, TransitSourceTitles routeTitles,
							   TransitSystem transitSystem) {

		this.drawables = drawables;
		this.routeTitles = routeTitles;
		this.transitSystem = transitSystem;
	}

	@Override
	public void refreshData(RouteConfig routeConfig, Selection selection,
							int maxStops, double centerLatitude, double centerLongitude,
							ConcurrentHashMap<String, BusLocation> busMapping,
							RoutePool routePool, Directions directions,
							Locations locationsObj) throws IOException, ParserConfigurationException, SAXException {
		Selection.Mode mode = selection.getMode();
		if (mode == Selection.Mode.VEHICLE_LOCATIONS_ALL ||
			mode == Selection.Mode.VEHICLE_LOCATIONS_ONE) {
				// no need for that here
		}
		else if (mode == Selection.Mode.BUS_PREDICTIONS_ALL ||
				mode == Selection.Mode.BUS_PREDICTIONS_ONE ||
				mode == Selection.Mode.BUS_PREDICTIONS_STAR) {
				RouteConfig hubwayRouteConfig = routePool.get(routeTag);
				DownloadHelper downloadHelper = new DownloadHelper(dataUrl);

				downloadHelper.connect();


				InputStream stream = downloadHelper.getResponseData();

				HubwayParser parser = new HubwayParser(hubwayRouteConfig);
				parser.runParse(stream);
				List<PredictionStopLocationPair> pairs = parser.getPairs();

				for (PredictionStopLocationPair pair : pairs) {
					pair.stopLocation.clearPredictions(null);
					pair.stopLocation.addPrediction(pair.prediction);
				}

		}
		else
		{
			throw new RuntimeException("Unknown mode encountered");

		}


	}

	@Override
	public boolean hasPaths() {
		return false;
	}

	@Override
	public String searchForRoute(String indexingQuery, String lowercaseQuery) {
		return SearchHelper.naiveSearch(indexingQuery, lowercaseQuery, routeTitles);
	}

	@Override
	public TransitDrawables getDrawables() {
		return drawables;
	}

	@Override
	public StopLocation createStop(float latitude, float longitude, String stopTag, String stopTitle, String route) {
		HubwayStopLocation stop = new HubwayStopLocation.HubwayBuilder(latitude,
				longitude, stopTag, stopTitle).build();
		stop.addRoute(route);
		return stop;
	}

	@Override
	public TransitSourceTitles getRouteTitles() {
		return routeTitles;
	}

	@Override
	public int getLoadOrder() {
		return 4;
	}

	@Override
	public int getTransitSourceId() {
		return Schema.Routes.enumagencyidHubway;
	}

	@Override
	public boolean requiresSubwayTable() {
		return false;
	}

	@Override
	public IAlerts getAlerts() {
		return transitSystem.getAlerts();
	}

	@Override
	public String getDescription() {
		return "Hubway";
	}
}
