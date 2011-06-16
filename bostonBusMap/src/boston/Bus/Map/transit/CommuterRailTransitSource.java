package boston.Bus.Map.transit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.Location;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.main.Main;
import boston.Bus.Map.main.UpdateAsyncTask;
import boston.Bus.Map.parser.CommuterRailPredictionsFeedParser;
import boston.Bus.Map.parser.CommuterRailRouteConfigParser;
import boston.Bus.Map.parser.SubwayPredictionsFeedParser;
import boston.Bus.Map.parser.SubwayRouteConfigFeedParser;
import boston.Bus.Map.ui.ProgressMessage;
import boston.Bus.Map.util.DownloadHelper;

public class CommuterRailTransitSource implements TransitSource {
	private final Drawable busStop;
	private final Drawable rail;
	private final Drawable railArrow;
	private final ArrayList<String> routes = new ArrayList<String>(12);
	private final HashMap<String, String> routeKeysToTitles = new HashMap<String, String>(12);
	
	public CommuterRailTransitSource(Drawable busStop, Drawable rail, Drawable railArrow)
	{
		this.busStop = busStop;
		this.rail = rail;
		this.railArrow = railArrow;
		
		addRoute("CR-1","Greenbush");
		addRoute("CR-2","Kingston");
		addRoute("CR-3","Middleborough/Lakeville");
		addRoute("CR-4","Fairmount");
		addRoute("CR-5","Providence/Stoughton");
		addRoute("CR-6","Franklin");
		addRoute("CR-7","Needham");
		addRoute("CR-8","Framingham/Worcester");
		addRoute("CR-9","Fitchburg");
		addRoute("CR-10","Lowell");
		addRoute("CR-11","Haverhill");
		addRoute("CR-12","Newburyport/Rockport");
	}
	
	private void addRoute(String key, String title) {
		routeKeysToTitles.put(key, title);
		routes.add(key);
	}

	public static String getRouteConfigUrl()
	{

		return null;
	}


	@Override
	public void populateStops(RoutePool routeMapping, String routeToUpdate,
			RouteConfig oldRouteConfig, Directions directions,
			UpdateAsyncTask task) throws ClientProtocolException, IOException,
			ParserConfigurationException, SAXException
	{
		
		//this will probably never be executed
		//final String urlString = getRouteConfigUrl();

		//DownloadHelper downloadHelper = new DownloadHelper(urlString);
		
		//downloadHelper.connect();
		//just initialize the route and then end for this round
		
		CommuterRailRouteConfigParser parser = new CommuterRailRouteConfigParser(busStop,
				directions, oldRouteConfig, this);

		//parser.runParse(downloadHelper.getResponseData()); 
		parser.runParse();

		parser.writeToDatabase(routeMapping, false, task);
	}

	@Override
	public void refreshData(RouteConfig routeConfig,
			int selectedBusPredictions, int maxStops, double centerLatitude,
			double centerLongitude,
			ConcurrentHashMap<Integer, BusLocation> busMapping,
			String selectedRoute, RoutePool routePool, Directions directions,
			Locations locationsObj) throws IOException,
			ParserConfigurationException, SAXException
	{
		HashSet<String> outputUrls = new HashSet<String>();
		switch (selectedBusPredictions)
		{
		case  Main.BUS_PREDICTIONS_ONE:
		case Main.VEHICLE_LOCATIONS_ONE:
		{

			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false);

			//ok, do predictions now
			getPredictionsUrl(locations, maxStops, routeConfig.getRouteName(), outputUrls, selectedBusPredictions);
			break;
		}
		case Main.BUS_PREDICTIONS_ALL:
		case Main.VEHICLE_LOCATIONS_ALL:
		case Main.BUS_PREDICTIONS_STAR:
		{
			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false);
			
			getPredictionsUrl(locations, maxStops, null, outputUrls, selectedBusPredictions);

		}
		break;

		}

		Log.v("BostonBusMap", "refreshing commuter data for " + outputUrls.size() + " routes");

		for (String url : outputUrls)
		{
			DownloadHelper downloadHelper = new DownloadHelper(url);
			
			downloadHelper.connect();

			InputStream data = downloadHelper.getResponseData();

			//bus prediction

			String id = url.substring(dataUrlPrefix.length());
			id = id.substring(0, id.length() - 5);
			RouteConfig railRouteConfig = routePool.get("CR-" + id);
			CommuterRailPredictionsFeedParser parser = new CommuterRailPredictionsFeedParser(railRouteConfig, directions,
					rail, railArrow, busMapping);

			parser.runParse(data);
		}
		
	}
	private static final String dataUrlPrefix = "http://developer.mbta.com/lib/RTCR/RailLine_";

	private void getPredictionsUrl(List<Location> locations, int maxStops,
			String routeName, HashSet<String> outputUrls,
			int mode) {
		//http://developer.mbta.com/lib/RTCR/RailLine_1.json
		
		//BUS_PREDICTIONS_ONE or VEHICLE_LOCATIONS_ONE
		if (routeName != null)
		{
			//we know we're updating only one route
			if (isCommuterRail(routeName))
			{
				String index = routeName.substring(3); //snip off beginning "CR-"
				outputUrls.add(dataUrlPrefix + index + ".json");
				return;
			}
		}
		else
		{
			if (mode == Main.BUS_PREDICTIONS_STAR)
			{
				//ok, let's look at the locations and see what we can get
				for (Location location : locations)
				{
					if (location instanceof StopLocation)
					{
						StopLocation stopLocation = (StopLocation)location;


						for (String route : stopLocation.getRoutes())
						{
							if (isCommuterRail(route))
							{
								String index = route.substring(3);
								outputUrls.add(dataUrlPrefix + index + ".json");
							}
						}
					}
					else
					{
						//bus location
						BusLocation busLocation = (BusLocation)location;
						String route = busLocation.getRouteId();

						if (isCommuterRail(route))
						{
							String index = route.substring(3);
							outputUrls.add(dataUrlPrefix + index + ".json");
						}
					}
				}
			}
			else
			{
				//add all 12 of them
				
				for (int i = 1; i <= 12; i++)
				{
					outputUrls.add(dataUrlPrefix + i + ".json");
				}
			}
		}
	}

	private boolean isCommuterRail(String routeName) {
		for (String route : routes)
		{
			if (route.equals(routeName))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasPaths() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void initializeAllRoutes(UpdateAsyncTask task, Context context,
			Directions directions, RoutePool routeMapping) throws IOException,
			ParserConfigurationException, SAXException {
		task.publish(new ProgressMessage(ProgressMessage.PROGRESS_DIALOG_ON, "Downloading commuter info", null));
		//final String subwayUrl = getRouteConfigUrl();
		//URL url = new URL(subwayUrl);
		//InputStream in = Locations.downloadStream(url, task);
		
		CommuterRailRouteConfigParser subwayParser = new CommuterRailRouteConfigParser(busStop, directions, null, this);
		
		subwayParser.runParse();
		
		subwayParser.writeToDatabase(routeMapping, false, task);
		
		
	}

	@Override
	public String[] getRoutes() {
		return routes.toArray(new String[0]);
	}

	@Override
	public HashMap<String, String> getRouteKeysToTitles() {
		return routeKeysToTitles;
	}

	@Override
	public Drawable getBusStopDrawable() {
		return busStop;
	}

	@Override
	public StopLocation createStop(float lat, float lon, String stopTag,
			String title, int platformOrder, String branch, String route,
			String dirTag) {
		StopLocation stopLocation = new StopLocation(lat, lon, busStop, stopTag, title);
		stopLocation.addRouteAndDirTag(route, dirTag);
		return stopLocation;
	}

	@Override
	public void bindPredictionElementsForUrl(StringBuilder urlString,
			String route, String stopTag, String dirTag) {
		//do nothing
	}

}
