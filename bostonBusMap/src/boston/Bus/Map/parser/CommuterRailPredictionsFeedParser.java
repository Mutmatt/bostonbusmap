package boston.Bus.Map.parser;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.drawable.Drawable;
import android.text.format.Time;
import android.util.Log;
import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.CommuterRailPrediction;
import boston.Bus.Map.data.CommuterTrainLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.Prediction;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.SubwayStopLocation;
import boston.Bus.Map.transit.CommuterRailTransitSource;
import boston.Bus.Map.transit.SubwayTransitSource;
import boston.Bus.Map.transit.TransitSystem;
import boston.Bus.Map.util.LogUtil;

public class CommuterRailPredictionsFeedParser
{
	private final RouteConfig routeConfig;
	private final Directions directions;
	private final Drawable rail;
	private final Drawable railArrow;

	private final SimpleDateFormat format;
	private final HashMap<String, Integer> indexes = new HashMap<String, Integer>();

	
	private final ConcurrentHashMap<Integer, BusLocation> busMapping;
	private final HashMap<String, String> routeKeysToTitles;

	public CommuterRailPredictionsFeedParser(RouteConfig routeConfig, Directions directions, Drawable bus, Drawable railArrow, 
			ConcurrentHashMap<Integer, BusLocation> busMapping, HashMap<String, String> routeKeysToTitles)
	{
		this.routeConfig = routeConfig;
		this.directions = directions;
		this.rail = bus;
		this.railArrow = railArrow;
		this.busMapping = busMapping;
		this.routeKeysToTitles = routeKeysToTitles;

		format = new SimpleDateFormat("M/d/y K:m:s");
		format.setTimeZone(TransitSystem.getTimeZone());
	}

	private void clearPredictions(String route) throws IOException
	{
		if (routeConfig != null)
		{
			for (StopLocation stopLocation : routeConfig.getStops())
			{
				stopLocation.clearPredictions(routeConfig);
			}
		}
	}

	public void runParse(Reader data) throws IOException
	{
		BufferedReader reader = new BufferedReader(data);

		String firstLine = reader.readLine();
		if (firstLine == null)
		{
			//bizarre; at least that line should exist
			return;
		}
		
		String[] definitions = firstLine.split(",");
		
		for (int i = 0; i < definitions.length; i++)
		{
			indexes.put(definitions[i], i);
		}
		
		//store everything here, then write out all out at once
		ArrayList<Prediction> predictions = new ArrayList<Prediction>(); 
		ArrayList<StopLocation> stopLocations = new ArrayList<StopLocation>(); 

		//start off with all the buses to be removed, and if they're still around remove them from toRemove
		HashSet<Integer> toRemove = new HashSet<Integer>();
		for (Integer id : busMapping.keySet())
		{
			BusLocation busLocation = busMapping.get(id);
			if (busLocation.isDisappearAfterRefresh())
			{
				toRemove.add(id);
			}
		}

		String route = routeConfig.getRouteName();
		try
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] array = line.split(",");
			

				String stopKey = getItem("Stop", array);

				StopLocation stopLocation = (StopLocation)routeConfig.getStop(CommuterRailTransitSource.stopTagPrefix + stopKey);

				if (stopLocation == null)
				{
					continue;
				}

				String timeStr = getItem("Scheduled", array);
				long scheduledArrivalEpochTime = Long.parseLong(timeStr) * 1000;
				int offset = TransitSystem.getTimeZone().getOffset(scheduledArrivalEpochTime);
				scheduledArrivalEpochTime += offset;

				String nowStr = getItem("TimeStamp", array);
				long nowEpochTime = Long.parseLong(nowStr) * 1000;
				int nowOffset = TransitSystem.getTimeZone().getOffset(nowEpochTime);
				nowEpochTime += nowOffset;
				
				long currentMillis = TransitSystem.currentTimeMillis();
				long diff = scheduledArrivalEpochTime - currentMillis;
				int minutes = (int)(diff / 1000 / 60);

				if (diff < 0 && minutes == 0)
				{
					//just to make sure we don't count this
					minutes = -1;
				}

				String direction = getItem("Destination", array);
				directions.add(direction, direction, "", routeConfig.getRouteName());

				String informationType = getItem("Flag", array);

				int flagEnum = CommuterRailPrediction.toFlagEnum(informationType);
				
				int lateness = Prediction.NULL_LATENESS;
				String latenessStr = getItem("Lateness", array);
				if (latenessStr.length() != 0)
				{
					try
					{
						lateness = Integer.parseInt(latenessStr);
					}
					catch (NumberFormatException e)
					{
						//oh well
					}
					
				}
				 
				int vehicleId = 0;
				try
				{
					String idStr = getItem("Vehicle", array);
					if (idStr != null && idStr.length() != 0)
					{
						vehicleId = Integer.parseInt(idStr);
					}
				}
				catch (NumberFormatException e)
				{
					LogUtil.e(e);
				}

				
				predictions.add(new CommuterRailPrediction(minutes, vehicleId, directions.getTitleAndName(direction),
						routeConfig.getRouteName(), false, false, lateness, flagEnum));
				stopLocations.add(stopLocation);

				float lat = 0;
				float lon = 0;
				String latString = getItem("Latitude", array);
				String lonString = getItem("Longitude", array);
				
				try
				{
					lat = Float.parseFloat(latString);
					lon = Float.parseFloat(lonString);
				}
				catch (NumberFormatException e)
				{
					//oh well
				}
				
				if (lat != 0 && lon != 0 && vehicleId != 0)
				{
					//StopLocation nextStop = getNextStop(routeConfig, stopLocation, direction);

					final int arrowTopDiff = 9;

					//first, see if there's a subway car which pretty much matches an old BusLocation
					BusLocation busLocation = null;

					String heading = getItem("Heading", array);
					if (heading != null && heading.length() == 0)
					{
						heading = null;
					}
					
					String routeTitle = routeKeysToTitles.get(route);
					if (routeTitle == null)
					{
						routeTitle = route;
					}
					
					busLocation = new CommuterTrainLocation(lat, lon,
							vehicleId, nowEpochTime, currentMillis, heading, true, direction, null, rail, 
							railArrow, route, directions, routeTitle, true, arrowTopDiff);
					busMapping.put(vehicleId, busLocation);

					toRemove.remove(vehicleId);


					//set arrow to point to correct direction

					/*if (nextStop != null)
					{
						//Log.v("BostonBusMap", "at " + stopLocation.getTitle() + " moving to " + nextStop.getTitle());
						busLocation.movedTo(nextStop.getLatitudeAsDegrees(), nextStop.getLongitudeAsDegrees());
					}
					else
					{
						//Log.v("BostonBusMap", "at " + stopLocation.getTitle() + ", nothing to move to");
					}*/
				}
			}

		}
		catch (ClassCastException e)
		{
			//probably updating the wrong url?
			Log.e("BostonBusMap", e.getMessage());
		}

		clearPredictions(route);

		for (int i = 0; i < stopLocations.size(); i++)
		{
			StopLocation stopLocation = stopLocations.get(i);
			stopLocation.addPrediction(predictions.get(i));
		}

		for (Integer id : toRemove)
		{
			busMapping.remove(id);
		}
	}


	private String getItem(String key, String[] array)
	{
		Integer intKey = indexes.get(key);
		if (null != intKey)
		{
			int i = intKey.intValue();
			if (i >= 0 && i < array.length)
			{
				return array[i];
			}
		}
		return "";
	}

	public Map<? extends Integer, ? extends BusLocation> getBusMapping() {
		return busMapping;
	}
}