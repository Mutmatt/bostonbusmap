package boston.Bus.Map.main;

import org.apache.http.impl.conn.tsccm.RouteSpecificPool;

import boston.Bus.Map.data.Locations;
import boston.Bus.Map.database.DatabaseHelper;
import boston.Bus.Map.transit.TransitSystem;
import boston.Bus.Map.ui.BusOverlay;
import boston.Bus.Map.ui.LocationOverlay;
import boston.Bus.Map.ui.RouteOverlay;
import boston.Bus.Map.util.Constants;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateHandler extends Handler {
	/**
	 * An update which refreshes from the internet
	 */
	public static final int MAJOR = 1;
	/**
	 * An update where we just moved the map
	 */
	public static final int MINOR = 2;
	
	public static final int GET_DIRECTIONS = 3;

	
	/**
	 * The last time we updated, in milliseconds. Used to make sure we don't update more frequently than
	 * every 10 seconds, to avoid unnecessary strain on their server
	 */
	private long lastUpdateTime;

	public final static int fetchDelay = 15000;
	
	private final int maxOverlays = 75;

	private final int IMMEDIATE_REFRESH = 1;

	private int updateConstantlyInterval;
	private boolean hideHighlightCircle;
	private boolean showUnpredictable;
	private UpdateAsyncTask updateAsyncTask;
	private UpdateAsyncTask minorUpdate;
	private GetDirectionsAsyncTask getDirectionsTask;
	
	private final ProgressBar progress;
	private final ProgressDialog progressDialog;
	private final MapView mapView;
	private boolean inferBusRoutes;
	private final Locations busLocations;
	private final DatabaseHelper helper;
	private final Context context;
	
	private final BusOverlay busOverlay; 
	private final RouteOverlay routeOverlay;
	private final LocationOverlay locationOverlay;
	
	private boolean isFirstRefresh;
	private boolean showRouteLine;
	
	private final TransitSystem transitSystem;
	
	public UpdateHandler(ProgressBar progress, MapView mapView,
			Drawable arrow, Drawable tooltip, Locations busLocations, Context context, DatabaseHelper helper, BusOverlay busOverlay,
			RouteOverlay routeOverlay,  LocationOverlay locationOverlay,
			UpdateAsyncTask majorHandler, TransitSystem transitSystem, ProgressDialog progressDialog)
	{
		this.progress = progress;
		this.mapView = mapView;
		this.busLocations = busLocations;
		this.helper = helper;
		this.busOverlay = busOverlay;
		this.routeOverlay = routeOverlay;
		this.locationOverlay = locationOverlay;
		lastUpdateTime = TransitSystem.currentTimeMillis();
		
		this.context = context;
		this.updateAsyncTask = majorHandler;
		this.transitSystem = transitSystem;
		this.progressDialog = progressDialog;
	}
	
	private String routeToUpdate;
	private int selectedBusPredictions;
	
	@Override
	public void handleMessage(Message msg) {
		switch (msg.what)
		{
		case GET_DIRECTIONS:
			//TODO: arguments for stop ids
			if (getDirectionsTask != null) {
				if (getDirectionsTask.getStatus().equals(GetDirectionsAsyncTask.Status.FINISHED) == false) {
					// task is not finished yet
					return;
				}
			}
			
			String start = "8128";
			String end = "2550";
			
			getDirectionsTask = new GetDirectionsAsyncTask();
			getDirectionsTask.runTask(start, end);
			break;
		
		case MAJOR:
			//remove duplicates
			long currentTime = TransitSystem.currentTimeMillis();
			
			int interval = getUpdateConstantlyInterval() * 1000;
			
			if (currentTime - lastUpdateTime > interval)
			{
				//if not too soon, do the update
				runUpdateTask(isFirstRefresh);
				isFirstRefresh = false;
			}
			else if (currentTime - lastUpdateTime > fetchDelay && msg.arg1 == IMMEDIATE_REFRESH)
			{
				runUpdateTask(isFirstRefresh);
				isFirstRefresh = false;
			}

			//make updateBuses execute every 10 seconds (or whatever fetchDelay is)
			//to disable this, the user should go into the settings and uncheck 'Run in background'
			if (msg.arg1 != IMMEDIATE_REFRESH && interval != 0)
			{
				removeMessages(MAJOR);
				sendEmptyMessageDelayed(MAJOR, interval);
			}


			break;
		case MINOR:
			//don't do two updates at once
			if (minorUpdate != null)
			{
				if (minorUpdate.getStatus().equals(UpdateAsyncTask.Status.FINISHED) == false)
				{
					//task is not finished yet
					return;
				}
				
			}

			GeoPoint geoPoint = mapView.getMapCenter();
			double centerLatitude = geoPoint.getLatitudeE6() * Constants.InvE6;
			double centerLongitude = geoPoint.getLongitudeE6() * Constants.InvE6;
			
			//remove duplicate messages
			removeMessages(MINOR);
			
			int idToSelect = msg.arg1;
			minorUpdate = new UpdateAsyncTask(progress, mapView, locationOverlay, getShowUnpredictable(), false, maxOverlays,
					getHideHighlightCircle() == false, getInferBusRoutes(), busOverlay, routeOverlay, helper,
					routeToUpdate, selectedBusPredictions, false, getShowRouteLine(), 
					transitSystem, progressDialog, idToSelect);
			

			minorUpdate.runUpdate(busLocations, centerLatitude, centerLongitude, context);
			
			break;
		}		
	}

	public void removeAllMessages() {
		removeMessages(MAJOR);
		removeMessages(MINOR);
		//removeMessages(LOCATION_NOT_FOUND);
		//removeMessages(LOCATION_FOUND);
	}

	
	public void kill()
	{
		if (updateAsyncTask != null)
		{
			updateAsyncTask.cancel(true);
		}

		if (minorUpdate != null)
		{
			minorUpdate.cancel(true);
		}
	}

	/**
	 * executes the update
	 */
	private void runUpdateTask(boolean isFirstTime) {
		//make sure we don't update too often
		lastUpdateTime = TransitSystem.currentTimeMillis();

		//don't do two updates at once
		if (updateAsyncTask != null)
		{
			if (updateAsyncTask.getStatus().equals(UpdateAsyncTask.Status.FINISHED) == false)
			{
				//task is not finished yet
				return;
			}
			
		}
		
		GeoPoint geoPoint = mapView.getMapCenter();
		double centerLatitude = geoPoint.getLatitudeE6() * Constants.InvE6;
		double centerLongitude = geoPoint.getLongitudeE6() * Constants.InvE6;

		
		updateAsyncTask = new UpdateAsyncTask(progress, mapView, locationOverlay, getShowUnpredictable(), true, maxOverlays,
				getHideHighlightCircle() == false, getInferBusRoutes(), busOverlay, routeOverlay, helper,
				routeToUpdate, selectedBusPredictions, isFirstTime, showRouteLine,
				transitSystem, progressDialog, 0);
		updateAsyncTask.runUpdate(busLocations, centerLatitude, centerLongitude, context);
	}

	public boolean instantRefresh() {
		//removeAllMessages();
		
		if(getUpdateConstantlyInterval() != Main.UPDATE_INTERVAL_NONE)
		{
			//if the runInBackground checkbox is clicked, start the handler updating
			removeMessages(MAJOR);
			sendEmptyMessageDelayed(MAJOR, getUpdateConstantlyInterval() * 1000);
		}
		
		if (TransitSystem.currentTimeMillis() - lastUpdateTime < fetchDelay)
		{
			return false;
		}

		runUpdateTask(isFirstRefresh);
		isFirstRefresh = false;
		return true;

	}

	public int getUpdateConstantlyInterval() {
		return updateConstantlyInterval;
	}
	
	public void setUpdateConstantlyInterval(int updateConstantlyInterval)
	{
		this.updateConstantlyInterval = updateConstantlyInterval;
	}
	
	public boolean getHideHighlightCircle()
	{
		return hideHighlightCircle;
	}
	
	public void setHideHighlightCircle(boolean b)
	{
		hideHighlightCircle = b;
	}
	
	public void setLastUpdateTime(long lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
		
	}


	public long getLastUpdateTime() {
		return lastUpdateTime;
	}
	
	public void setShowUnpredictable(boolean b)
	{
		showUnpredictable = b;
	}
	
	public boolean getShowUnpredictable()
	{
		return showUnpredictable;
	}
	
	public void setInferBusRoutes(boolean b)
	{
		inferBusRoutes = b;
	}

	public boolean getInferBusRoutes()
	{
		return inferBusRoutes;
	}
	
	public void setInitAllRouteInfo(boolean b)
	{
		isFirstRefresh = b;
	}
	
	public boolean getInitAllRouteInfo()
	{
		return isFirstRefresh;
	}
	

	public void setShowRouteLine(boolean b) {
		showRouteLine = b;
	}

	public boolean getShowRouteLine()
	{
		return showRouteLine;
	}
	
	
	public void triggerUpdate(int millis) {
		sendEmptyMessageDelayed(MINOR, millis);
		
	}
	
	public void triggerUpdate() {
		sendEmptyMessage(MINOR);
		
	}

	public void triggerUpdateThenSelect(int id)
	{
		Message msg = new Message();
		msg.arg1 = id;
		msg.what = MINOR;
		sendMessage(msg);
	}

	public void resume() {
		//removeAllMessages();
		if(getUpdateConstantlyInterval() != Main.UPDATE_INTERVAL_NONE)
		{
			//if the runInBackground checkbox is clicked, start the handler updating
		    instantRefresh();
		}
	}



	public void immediateRefresh() {
		Message msg = new Message();
		msg.arg1 = IMMEDIATE_REFRESH;
		msg.what = MAJOR;
		sendMessage(msg);
	}



	public void setRouteToUpdate(String routeToUpdate) {
		this.routeToUpdate = routeToUpdate;
	}

	public void setSelectedBusPredictions(int b)
	{
		selectedBusPredictions = b; 
	}



	public UpdateAsyncTask getMajorHandler() {
		return updateAsyncTask;
	}



	public void nullifyProgress() {
		if (updateAsyncTask != null)
		{
			updateAsyncTask.nullifyProgress();
		}
		
		if (minorUpdate != null)
		{
			//probably not in the middle of something but just in case
			minorUpdate.nullifyProgress();
		}
	}

	public void getDirections() {
		
	}


}
