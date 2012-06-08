package boston.Bus.Map.transit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import boston.Bus.Map.data.AlertsMapping;
import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.CommuterRailStopLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.Location;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.MyHashMap;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.SubwayStopLocation;
import boston.Bus.Map.data.TransitDrawables;
import boston.Bus.Map.main.Main;
import boston.Bus.Map.main.UpdateAsyncTask;
import boston.Bus.Map.parser.AlertParser;
import boston.Bus.Map.parser.CommuterRailPredictionsFeedParser;
import boston.Bus.Map.parser.CommuterRailRouteConfigParser;
import boston.Bus.Map.parser.SubwayPredictionsFeedParser;
import boston.Bus.Map.parser.SubwayRouteConfigFeedParser;
import boston.Bus.Map.ui.ProgressMessage;
import boston.Bus.Map.util.DownloadHelper;
import boston.Bus.Map.util.SearchHelper;

public class CommuterRailTransitSource implements TransitSource {
	public static final String stopTagPrefix = "CRK-";
	private final String[] routes;
	private final MyHashMap<String, String> routeKeysToTitles = new MyHashMap<String, String>(12);
	private static final String predictionsUrlSuffix = ".csv";
	public static final String routeTagPrefix = "CR-";
	private static final String dataUrlPrefix = "http://developer.mbta.com/lib/RTCR/RailLine_";
	
	private final MyHashMap<String, String> routeKeysToAlertUrls = new MyHashMap<String, String>();
	private final TransitDrawables drawables;
	
	public CommuterRailTransitSource(TransitDrawables drawables, AlertsMapping alertsMapping)
	{
		this.drawables = drawables;
		
		String[] routeNames = new String[] {
				"Greenbush",
				"Kingston/Plymouth",
				"Middleborough/Lakeville",
				"Fairmount",
				"Providence/Stoughton",
				"Franklin",
				"Needham",
				"Framingham/Worcester",
				"Fitchburg/South Acton",
				"Lowell",
				"Haverhill",
				"Newburyport/Rockport"

		};
		
		
		
		//map alert keys to numbers
		MyHashMap<String, Integer> alertNumbers = alertsMapping.getAlertNumbers(routeNames, routeKeysToTitles);
		
		routes = new String[routeNames.length];
		
		for (int i = 0; i < routeNames.length; i++)
		{
			addRoute(routeTagPrefix + (i+1), routeNames[i], i);
		}

		
		for (int i = 0; i < routeNames.length; i++)
		{
			String routeTitle = routeNames[i];
			int alertKey = alertNumbers.get(routeTitle);
			addAlert(routeTagPrefix + (i+1), alertKey);
		}
	}
	
	private void addAlert(String routeKey, int alertNum) {
		routeKeysToAlertUrls.put(routeKey, AlertsMapping.alertUrlPrefix + alertNum);
	}

	private void addRoute(String key, String title, int index) {
		routeKeysToTitles.put(key, title);
		routes[index] = key;
	}

	public static String getRouteConfigUrl()
	{

		return null;
	}


	@Override
	public void populateStops(RoutePool routeMapping, String routeToUpdate,
			RouteConfig oldRouteConfig, Directions directions,
			UpdateAsyncTask task, boolean silent) throws ClientProtocolException, IOException,
			ParserConfigurationException, SAXException
	{
		
		//this will probably never be executed
		//final String urlString = getRouteConfigUrl();

		//DownloadHelper downloadHelper = new DownloadHelper(urlString);
		
		//downloadHelper.connect();
		//just initialize the route and then end for this round
		
		CommuterRailRouteConfigParser parser = new CommuterRailRouteConfigParser(directions, oldRouteConfig, this);

		//parser.runParse(downloadHelper.getResponseData()); 
		parser.runParse(new StringReader(CommuterRailRouteConfigParser.temporaryInputData));

		parser.writeToDatabase(routeMapping, false, task, silent);
	}

	@Override
	public void refreshData(RouteConfig routeConfig,
			int selectedBusPredictions, int maxStops, double centerLatitude,
			double centerLongitude,
			ConcurrentHashMap<String, BusLocation> busMapping,
			String selectedRoute, RoutePool routePool, Directions directions,
			Locations locationsObj) throws IOException,
			ParserConfigurationException, SAXException
	{
		if (selectedBusPredictions == Main.VEHICLE_LOCATIONS_ALL)
		{
			//for now I'm only refreshing data for buses if this is checked
			return;
		}
		
		ArrayList<String> outputUrls = new ArrayList<String>();
		ArrayList<String> outputAlertUrls = new ArrayList<String>();
		ArrayList<String> outputRoutes = new ArrayList<String>();
		switch (selectedBusPredictions)
		{
		case  Main.BUS_PREDICTIONS_ONE:
		case Main.VEHICLE_LOCATIONS_ONE:
		{

			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false);

			//ok, do predictions now
			getPredictionsUrl(locations, maxStops, routeConfig.getRouteName(), outputUrls, outputAlertUrls, outputRoutes, selectedBusPredictions);
			break;
		}
		case Main.BUS_PREDICTIONS_ALL:
		case Main.VEHICLE_LOCATIONS_ALL:
		case Main.BUS_PREDICTIONS_STAR:
		{
			List<Location> locations = locationsObj.getLocations(maxStops, centerLatitude, centerLongitude, false);
			
			getPredictionsUrl(locations, maxStops, null, outputUrls, outputAlertUrls, outputRoutes, selectedBusPredictions);

		}
		break;

		}

		for (int i = 0; i < outputUrls.size(); i++)
		{
			String url = outputUrls.get(i);
			DownloadHelper downloadHelper = new DownloadHelper(url);
			
			downloadHelper.connect();
			

			InputStream stream = downloadHelper.getResponseData();
			InputStreamReader data = new InputStreamReader(stream);
			//StringReader data = new StringReader(hardcodedData);

			//bus prediction

			String route = outputRoutes.get(i);
			RouteConfig railRouteConfig = routePool.get(route);
			CommuterRailPredictionsFeedParser parser = new CommuterRailPredictionsFeedParser(railRouteConfig, directions,
					drawables, busMapping, routeKeysToTitles);

			parser.runParse(data);
			data.close();
		}
		
		for (int i = 0; i < outputAlertUrls.size(); i++)
		{
			String route = outputRoutes.get(i);
			RouteConfig railRouteConfig = routePool.get(route);

			if (railRouteConfig.obtainedAlerts() == false)
			{

				String url = outputAlertUrls.get(i);
				DownloadHelper downloadHelper = new DownloadHelper(url);
				downloadHelper.connect();

				InputStream stream = downloadHelper.getResponseData();
				InputStreamReader data = new InputStreamReader(stream);

				AlertParser parser = new AlertParser();
				parser.runParse(data);
				railRouteConfig.setAlerts(parser.getAlerts());
				data.close();

			}
		}
		
	}

	private void getPredictionsUrl(List<Location> locations, int maxStops,
			String routeName, ArrayList<String> outputUrls, ArrayList<String> outputAlertUrls,
			ArrayList<String> outputRoutes, int mode)
	{
		//http://developer.mbta.com/lib/RTCR/RailLine_1.csv
		
		//BUS_PREDICTIONS_ONE or VEHICLE_LOCATIONS_ONE
		if (routeName != null)
		{
			//we know we're updating only one route
			if (isCommuterRail(routeName))
			{
				String index = routeName.substring(routeTagPrefix.length()); //snip off beginning "CR-"
				outputUrls.add(dataUrlPrefix + index + predictionsUrlSuffix);
				String alertUrl = routeKeysToAlertUrls.get(routeName);
				outputAlertUrls.add(alertUrl);
				outputRoutes.add(routeName);
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
							if (isCommuterRail(route) && outputRoutes.contains(route) == false)
							{
								String index = route.substring(routeTagPrefix.length());
								outputUrls.add(dataUrlPrefix + index + predictionsUrlSuffix);
								String alertUrl = routeKeysToAlertUrls.get(route);
								outputAlertUrls.add(alertUrl);
								outputRoutes.add(route);
							}
						}
					}
					else
					{
						//bus location
						BusLocation busLocation = (BusLocation)location;
						String route = busLocation.getRouteId();

						if (isCommuterRail(route) && outputRoutes.contains(route) == false)
						{
							String index = route.substring(3);
							outputUrls.add(dataUrlPrefix + index + predictionsUrlSuffix);
							String alertUrl = routeKeysToAlertUrls.get(route);
							outputAlertUrls.add(alertUrl);
							outputRoutes.add(route);
						}
					}
				}
			}
			else
			{
				//add all 12 of them
				
				for (int i = 1; i <= 12; i++)
				{
					outputUrls.add(dataUrlPrefix + i + predictionsUrlSuffix);
					String routeKey = routeTagPrefix + i;
					String alertUrl = routeKeysToAlertUrls.get(routeKey);
					
					outputAlertUrls.add(alertUrl);
					outputRoutes.add(routeKey);
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
		
		CommuterRailRouteConfigParser subwayParser = new CommuterRailRouteConfigParser(directions, null, this);
		
		subwayParser.runParse(new StringReader(CommuterRailRouteConfigParser.temporaryInputData));
		
		subwayParser.writeToDatabase(routeMapping, false, task, false);
		
		
	}

	@Override
	public String[] getRoutes() {
		return routes;
	}

	@Override
	public MyHashMap<String, String> getRouteKeysToTitles() {
		return routeKeysToTitles;
	}

	@Override
	public TransitDrawables getDrawables() {
		return drawables;
	}
	
	@Override
	public StopLocation createStop(float lat, float lon, String stopTag,
			String title, int platformOrder, String branch, String route,
			String dirTag) {
		SubwayStopLocation stopLocation = new CommuterRailStopLocation(lat, lon, drawables, stopTag, title,
				platformOrder, branch);
		stopLocation.addRouteAndDirTag(route, dirTag);
		return stopLocation;
	}

	@Override
	public void bindPredictionElementsForUrl(StringBuilder urlString,
			String route, String stopTag, String dirTag) {
		//do nothing
	}

	@Override
	public String searchForRoute(String indexingQuery, String lowercaseQuery)
	{
		//try splitting up the route keys along the diagonal and see if they match one piece of it
		for (String route : routeKeysToTitles.keySet())
		{
			String title = routeKeysToTitles.get(route);
			if (title.contains("/"))
			{
				String[] pieces = title.split("/");
				for (int i = 0; i < pieces.length; i++)
				{
					if (lowercaseQuery.equals(pieces[i].toLowerCase()))
					{
						return route;
					}
				}
			}
		}
		
		return SearchHelper.naiveSearch(indexingQuery, lowercaseQuery, routes, routeKeysToTitles);
		
	}
}