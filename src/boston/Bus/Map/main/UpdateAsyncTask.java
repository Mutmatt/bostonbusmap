/*
    BostonBusMap
 
    Copyright (C) 2009  George Schneeloch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */
package boston.Bus.Map.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.CircularRedirectException;
import org.xml.sax.SAXException;



import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.Direction;
import boston.Bus.Map.data.Location;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.Path;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RouteTitles;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.UpdateArguments;
import boston.Bus.Map.transit.TransitSystem;
import boston.Bus.Map.ui.BusOverlay;
import boston.Bus.Map.ui.LocationOverlay;
import boston.Bus.Map.ui.ProgressMessage;
import boston.Bus.Map.ui.RouteOverlay;
import boston.Bus.Map.util.Constants;
import boston.Bus.Map.util.FeedException;
import boston.Bus.Map.util.LogUtil;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles the heavy work of downloading and parsing the XML in a separate thread from the UI.
 *
 */
public class UpdateAsyncTask extends AsyncTask<Double, Object, ImmutableList<Location>>
{
	private final boolean doShowUnpredictable;
	private final boolean doRefresh;
	/**
	 * For now this is always false. I need to figure out how to download a 1 megabyte file gracefully
	 */
	private final boolean doInit;
	private final int maxOverlays;
	private final boolean drawCircle;
	
	private String progressDialogTitle;
	private String progressDialogMessage;
	private int progressDialogMax;
	private int progressDialogProgress;
	private boolean progressDialogIsShowing;
	private boolean progressIsShowing;
	
	private boolean silenceUpdates;
	
	private final boolean inferBusRoutes;
	
	private final String routeToUpdate;
	private final int selectedBusPredictions;

	private final UpdateArguments arguments;
	
	/*public UpdateAsyncTask(ProgressBar progress, MapView mapView, LocationOverlay locationOverlay,
			boolean doShowUnpredictable, boolean doRefresh, int maxOverlays,
			boolean drawCircle, boolean inferBusRoutes, BusOverlay busOverlay, RouteOverlay routeOverlay, 
			DatabaseHelper helper, String routeToUpdate,
			int selectedBusPredictions, boolean doInit, boolean showRouteLine,
		TransitSystem transitSystem, ProgressDialog progressDialog, int idToSelect)*/
	public UpdateAsyncTask(UpdateArguments arguments, boolean doShowUnpredictable,
			boolean doRefresh, int maxOverlays, boolean drawCircle, boolean inferBusRoutes,
			String routeToUpdate, int selectedBusPredictions, boolean doInit)
	{
		super();
		
		this.arguments = arguments;
		//NOTE: these should only be used in one of the UI threads
		this.doShowUnpredictable = doShowUnpredictable;
		this.doRefresh = doRefresh;
		this.maxOverlays = maxOverlays;
		this.drawCircle = drawCircle;
		this.inferBusRoutes = inferBusRoutes;
		this.routeToUpdate = routeToUpdate;
		this.selectedBusPredictions = selectedBusPredictions;
		this.doInit = doInit;
	}
	
	/**
	 * A type safe wrapper around execute
	 * @param busLocations
	 */
	public void runUpdate(double centerLatitude, double centerLongitude)
	{
		execute(centerLatitude, centerLongitude);
	}

	@Override
	protected ImmutableList<Location> doInBackground(Double... args) {
		//number of bus pictures to draw. Too many will make things slow
		return updateBusLocations(args[0], args[1]);
	}

	@Override
	protected void onProgressUpdate(Object... strings)
	{
		if (silenceUpdates == false)
		{
			final ProgressDialog progressDialog = arguments.getProgressDialog();
			final ProgressBar progress = arguments.getProgress();
			if (progressDialog == null || progress == null)
			{
				return;
			}

			Object string = strings[0];
			if (string instanceof Integer)
			{
				int value = (Integer)string;
				progressDialog.setProgress(value);
				progressDialogProgress = value;
			}
			else if (string instanceof ProgressMessage)
			{
				ProgressMessage message = (ProgressMessage)string;

				switch (message.type)
				{
				case ProgressMessage.PROGRESS_OFF:
					if (progressDialog != null)
					{
						progressDialog.dismiss();
					}
					progressDialogIsShowing = false;

					if (progress != null)
					{
						progress.setVisibility(View.INVISIBLE);
					}
					progressIsShowing = false;
					break;
				case ProgressMessage.PROGRESS_DIALOG_ON:
					if (progressDialog != null)
					{
						progressDialog.setTitle(message.title);
						progressDialog.setMessage(message.message);
						progressDialog.show();
					}
					progressDialogTitle = message.title;
					progressDialogMessage = message.message;
					progressDialogIsShowing = true;

					break;
				case ProgressMessage.SET_MAX:
					if (progressDialog != null)
					{
						progressDialog.setMax(message.max);
					}
					progressDialogMax = message.max;
					break;
				case ProgressMessage.PROGRESS_SPINNER_ON:
					if (progress != null)
					{
						progress.setVisibility(View.VISIBLE);
					}
					progressIsShowing = true;
					break;
				case ProgressMessage.TOAST:
					Log.v("BostonBusMap", "Toast made: " + string);
					Toast.makeText(arguments.getContext(), message.message, Toast.LENGTH_LONG).show();
					break;
				}
			}
		}
	}

	public ImmutableList<Location> updateBusLocations(double centerLatitude, double centerLongitude)
	{
		if (doRefresh == false)
		{
			//if doRefresh is false, we just want to resort the overlays for a new center. Don't bother updating the text
			silenceUpdates = true;
		}
		
		final Locations busLocations = arguments.getBusLocations();
		busLocations.select(routeToUpdate, selectedBusPredictions);

		
		if (doRefresh)
		{
			try
			{
				publish(new ProgressMessage(ProgressMessage.PROGRESS_SPINNER_ON, null, null));
				
				RouteTitles allRoutes = arguments.getTransitSystem().getRouteKeysToTitles();
				final Context context = arguments.getContext();
				busLocations.initializeAllRoutes(this, context, allRoutes);
				
				busLocations.refresh(arguments.getContext(), inferBusRoutes, routeToUpdate, selectedBusPredictions,
						centerLatitude, centerLongitude, this, arguments.getOverlayGroup().getRouteOverlay().isShowLine());
			}
			catch (IOException e)
			{
				//this probably means that there is no Internet available, or there's something wrong with the feed
				publish(new ProgressMessage(ProgressMessage.TOAST, null, "Feed is inaccessible; try again later"));

				LogUtil.e(e);
				
				return null;
			} catch (SAXException e) {
				publish(new ProgressMessage(ProgressMessage.TOAST, null,
						"XML parsing exception; cannot update. Maybe there was a hiccup in the feed?"));

				LogUtil.e(e);
				
				return null;
			} catch (NumberFormatException e) {
				publish(new ProgressMessage(ProgressMessage.TOAST, null, "XML number parsing exception; cannot update. Maybe there was a hiccup in the feed?"));

				LogUtil.e(e);
				
				return null;
			} catch (ParserConfigurationException e) {
				publish(new ProgressMessage(ProgressMessage.TOAST, null, "XML parser configuration exception; cannot update"));

				LogUtil.e(e);
				
				return null;
			} catch (FactoryConfigurationError e) {
				publish(new ProgressMessage(ProgressMessage.TOAST, null, "XML parser factory configuration exception; cannot update"));

				LogUtil.e(e);
				
				return null;
			}
			catch (RuntimeException e)
			{
				if (e.getCause() instanceof FeedException)
				{
					publish(new ProgressMessage(ProgressMessage.TOAST, null, "The feed is reporting an error"));

					LogUtil.e(e);
					
					return null;
				}
				else
				{
					throw e;
				}
			}
			catch (Exception e)
			{
				publish(new ProgressMessage(ProgressMessage.TOAST, null, "Unknown exception occurred"));

				LogUtil.e(e);
				
				return null;
			}
			catch (AssertionError e)
			{
				Throwable cause = e.getCause();
				if (cause != null)
				{
					if (cause instanceof SocketTimeoutException)
					{
						publish(new ProgressMessage(ProgressMessage.TOAST, null, "Connection timed out"));

						LogUtil.e(e);
						
						return null;
					}
					else if (cause instanceof SocketException)
					{
						publish(new ProgressMessage(ProgressMessage.TOAST, null, "Connection error occurred"));

						LogUtil.e(e);
						
						return null;
					}
					else
					{
						publish(new ProgressMessage(ProgressMessage.TOAST, null, "Unknown exception occurred"));

						LogUtil.e(e);
						
						return null;
					}
				}
				else
				{
					publish(new ProgressMessage(ProgressMessage.TOAST, null, "Unknown exception occurred"));

					LogUtil.e(e);
					
					return null;
				}
			}
			catch (Error e)
			{
				publish(new ProgressMessage(ProgressMessage.TOAST, null, "Unknown exception occurred"));

				LogUtil.e(e);
				
				return null;
			}
			finally
			{
				//we should always set the icon to invisible afterwards just in case
				publish(new ProgressMessage(ProgressMessage.PROGRESS_OFF, null, null));
			}
		}

		try {
			return busLocations.getLocations(maxOverlays, centerLatitude, centerLongitude, doShowUnpredictable);
		} catch (IOException e) {
			publish(new ProgressMessage(ProgressMessage.TOAST, null, "Error getting route data from database"));

			LogUtil.e(e);
			
			return null;
		}
    }
	
	@Override
	protected void onPostExecute(final ImmutableList<Location> locations)
	{
		try
		{
			postExecute(locations);
		}
		catch (Throwable t)
		{
			LogUtil.e(t);
		}
	}
	
	private void postExecute(final ImmutableList<Location> locationsNearCenter)
	{
		if (locationsNearCenter == null)
		{
			//we probably posted an error message already; just return
			return;
		}
		
		//if doRefresh is false, we should skip this, it prevents the icons from updating locations
		if (locationsNearCenter.size() == 0 && doRefresh)
		{
			//no data? oh well
			//sometimes the feed provides an empty XML message; completely valid but without any vehicle elements
			publish(new ProgressMessage(ProgressMessage.TOAST, null, "Finished update, no data provided"));

			//an error probably occurred; keep buses where they were before, and don't overwrite message in textbox
			return;
		}
		
		final BusOverlay busOverlay = arguments.getOverlayGroup().getBusOverlay();
		//get currently selected location id, or -1 if nothing is selected
		
		busOverlay.setDrawHighlightCircle(drawCircle);
		
		final RouteOverlay routeOverlay = arguments.getOverlayGroup().getRouteOverlay();
		//routeOverlay.setDrawCoarseLine(showCoarseRouteLine);
		
		//get a list of lat/lon pairs which describe the route
        Path[] paths;
		Locations locationsObj = arguments.getBusLocations();
		try {
			paths = locationsObj.getSelectedPaths();
		} catch (IOException e) {
			LogUtil.e(e);
			paths = RouteConfig.nullPaths;
		}
		
		RouteConfig selectedRouteConfig;
		if (selectedBusPredictions == Main.BUS_PREDICTIONS_STAR || 
				selectedBusPredictions == Main.BUS_PREDICTIONS_ALL ||
				selectedBusPredictions == Main.BUS_PREDICTIONS_INTERSECT)
		{
			//we want this to be null. Else, the snippet drawing code would only show data for a particular route
			try {
				//get the currently drawn route's color
				RouteConfig route = locationsObj.getSelectedRoute();
				String routeName = route != null ? route.getRouteName() : "";
				routeOverlay.setPathsAndColor(paths, Color.BLUE, routeName);

			} catch (IOException e) {
				LogUtil.e(e);
				routeOverlay.setPathsAndColor(paths, Color.BLUE, null);
			}
			selectedRouteConfig = null;
		}
		else
		{
			try {
				selectedRouteConfig = locationsObj.getSelectedRoute();
			} catch (IOException e) {
				LogUtil.e(e);
				selectedRouteConfig = null;
			}
			
			if (selectedRouteConfig != null)
			{
				routeOverlay.setPathsAndColor(paths, selectedRouteConfig.getColor(), selectedRouteConfig.getRouteName());
			}
		}
		

		
		//we need to run populate even if there are 0 busLocations. See this link:
		//http://groups.google.com/group/android-beginners/browse_thread/thread/6d75c084681f943e?pli=1
		final int selectedBusId = busOverlay != null ? busOverlay.getSelectedBusId() : BusOverlay.NOT_SELECTED;
		busOverlay.clear();
		//busOverlay.doPopulate();

		busOverlay.setLocations(locationsObj);
		
		RouteTitles routeKeysToTitles = arguments.getTransitSystem().getRouteKeysToTitles();
		
		//point hash to index in busLocations
		Map<Long, Integer> points = Maps.newHashMap();
		
		//draw the buses on the map
		int newSelectedBusId = selectedBusId;
		List<Location> busesToDisplay = Lists.newArrayList();
		for (int i = 0; i < locationsNearCenter.size(); i++)
		{
			Location busLocation = locationsNearCenter.get(i);
			
			final int latInt = (int)(busLocation.getLatitudeAsDegrees() * Constants.E6);
			final int lonInt = (int)(busLocation.getLongitudeAsDegrees() * Constants.E6);
					
			//make a hash to easily compare this location's position against others
			//get around sign extension issues by making them all positive numbers
			final int latIntHash = (latInt < 0 ? -latInt : latInt);
			final int lonIntHash = (lonInt < 0 ? -lonInt : lonInt);
			long hash = (long)((long)latIntHash << 32) | (long)lonIntHash;
			Integer index = points.get(hash);
			final Context context = arguments.getContext();
			if (null != index)
			{
				//two stops in one space. Just use the one overlay, and combine textboxes in an elegant manner
				Location parent = locationsNearCenter.get(index);
				parent.addToSnippetAndTitle(selectedRouteConfig, busLocation, routeKeysToTitles, context);
				
				if (busLocation.getId() == selectedBusId)
				{
					//the thing we want to select isn't available anymore, choose the other icon
					newSelectedBusId = parent.getId();
				}
			}
			else
			{
				busLocation.makeSnippetAndTitle(selectedRouteConfig, routeKeysToTitles, context);
			
			
				points.put(hash, i);
		
				//the title is displayed when someone taps on the icon
				busesToDisplay.add(busLocation);
			}
		}
		for (Location location : busesToDisplay) {
			// we need to do this here because addLocation creates PredictionViews, which needs
			// to happen after makeSnippetAndTitle and addToSnippetAndTitle
			busOverlay.addLocation(location);
		}
		busOverlay.setSelectedBusId(newSelectedBusId);
		//busOverlay.refreshBalloons();
		
		final MapView mapView = arguments.getMapView();
		arguments.getOverlayGroup().refreshMapView(mapView);
		
		
		//make sure we redraw map
		mapView.invalidate();
	}
	
	/**
	 * public method exposing protected publishProgress()
	 * @param msg
	 */
	public void publish(ProgressMessage msg)
	{
		publishProgress(msg);
	}
	
	public void publish(int value)
	{
		publishProgress(value);
	}

	
	
	public void nullifyProgress()
	{
		arguments.setProgress(null);
		arguments.setProgressDialog(null);
	}
	
	/**
	 * This must get run in the UI thread. Neither parameter can be null; use nullifyProgress for that
	 * 
	 * @param progress
	 * @param progressDialog
	 */
	public void setProgress(ProgressBar progress, ProgressDialog progressDialog) {
		arguments.setProgress(progress);
		arguments.setProgressDialog(progressDialog);
		
		progress.setVisibility(progressIsShowing ? View.VISIBLE : View.INVISIBLE);
		progressDialog.setTitle(progressDialogTitle);
		progressDialog.setMessage(progressDialogMessage);
		progressDialog.setMax(progressDialogMax);
		if (progressDialogIsShowing)
		{
			progressDialog.show();
		}
	}
}
