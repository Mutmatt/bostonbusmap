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
package boston.Bus.Map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


import boston.Bus.Map.BusLocations.BusLocation;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.TextView;

/**
 * Handles the heavy work of downloading and parsing the XML in a separate thread from the UI.
 *
 */
public class UpdateAsyncTask extends AsyncTask<Object, String, List<BusLocation>>
{
	private final TextView textView;
	private final Drawable busPicture;
	private final MapView mapView;
	private final String finalMessage;
	private final Drawable arrow;
	private final Drawable tooltip;
	private final Updateable updateable;
	
	private boolean silenceUpdates;
	
	public UpdateAsyncTask(TextView textView, Drawable busPicture, MapView mapView, String finalMessage,
			Drawable arrow, Drawable tooltip, Updateable updateable)
	{
		super();
		
		//NOTE: these should only be used in one of the UI threads
		this.textView = textView;
		this.busPicture = busPicture;
		this.mapView = mapView;
		this.finalMessage = finalMessage;
		this.arrow = arrow;
		this.tooltip = tooltip;
		this.updateable = updateable;
	}
	
	/**
	 * A type safe wrapper around execute
	 * @param latitude
	 * @param longitude
	 * @param maxOverlays
	 * @param busLocations
	 * @param doShowUnpredictable
	 */
	public void runUpdate(double latitude, double longitude, int maxOverlays,
			BusLocations busLocations, boolean doShowUnpredictable, boolean doRefresh)
	{
		execute(latitude, longitude, maxOverlays, busLocations, doShowUnpredictable, doRefresh);
	}

	@Override
	protected List<BusLocation> doInBackground(Object... args) {
		// these are the arguments passed in on the execute() function
		double latitude = ((Double)args[0]).doubleValue();
		double longitude = ((Double)args[1]).doubleValue();
		//number of bus pictures to draw. Too many will make things slow
		int maxOverlays = ((Integer)args[2]).intValue();
		BusLocations busLocations = (BusLocations)args[3];
		boolean doShowUnpredictable = ((Boolean)args[4]).booleanValue();
		boolean doRefresh = ((Boolean)args[5]).booleanValue();
		
		return updateBusLocations(latitude, longitude, maxOverlays, busLocations, doShowUnpredictable, doRefresh);
	}

	@Override
	protected void onProgressUpdate(String... strings)
	{
		if (silenceUpdates == false)
		{
			textView.setText(strings[0]);
		}
	}

	public List<BusLocation> updateBusLocations(double latitude, double longitude, int maxOverlays, BusLocations busLocations,
			boolean doShowUnpredictable, boolean doRefresh)
	{
		if (doRefresh == false)
		{
			//if doRefresh is false, we just want to resort the overlays for a new center. Don't bother updating the text
			silenceUpdates = true;
		}
		
		publishProgress("Fetching bus location data...");

		if (doRefresh)
		{
			try
			{

				busLocations.Refresh();
			}
			catch (IOException e)
			{
				//this probably means that there is no Internet available, or there's something wrong with the feed
				publishProgress("Bus feed is inaccessable; try again later");
				e.printStackTrace();
				return new ArrayList<BusLocation>();

			} catch (SAXException e) {
				publishProgress("XML parsing exception; cannot update. Maybe there was a hiccup in the feed?");
				e.printStackTrace();
				return new ArrayList<BusLocation>();
			} catch (ParserConfigurationException e) {
				publishProgress("XML parser configuration exception; cannot update");
				e.printStackTrace();
				return new ArrayList<BusLocation>();
			} catch (FactoryConfigurationError e) {
				publishProgress("XML parser factory configuration exception; cannot update");
				e.printStackTrace();
				return new ArrayList<BusLocation>();
			}
			catch (Exception e)
			{
				publishProgress("Unknown exception occurred");
				e.printStackTrace();
				return new ArrayList<BusLocation>();
			}
		}
		publishProgress("Preparing to draw bus overlays...");
		
		ArrayList<BusLocation> ret = new ArrayList<BusLocation>();
		
		ret.addAll(busLocations.getBusLocations(maxOverlays, latitude, longitude, doShowUnpredictable));
		
		publishProgress("Adding bus overlays to map...");
		
		if (ret.size() == 0)
		{
			//no data? oh well
			//sometimes the feed provides an empty XML message; completely valid but without any vehicle elements
			publishProgress("Finished update, no data provided");
		}
		
    	return ret;
    }
	
	@Override
	protected void onPostExecute(List<BusLocation> busLocations)
	{
		if (busLocations.size() == 0)
		{
			//an error probably occurred; keep buses where they were before, and don't overwrite message in textbox
			return;
		}
		
		List<com.google.android.maps.Overlay> overlays = mapView.getOverlays();
        
		int selectedBusId = -1;
		if (overlays.size() > 0 && overlays.get(0) instanceof BusOverlay)
		{
			BusOverlay oldBusOverlay = (BusOverlay)overlays.get(0);
			selectedBusId = oldBusOverlay.getSelectedBusId();
		}
		
        overlays.clear();
        
        publishProgress("Drawing overlays...");
        
        
    	BusOverlay busOverlay = new BusOverlay(busPicture, textView.getContext(), busLocations, selectedBusId,
    			arrow, tooltip, updateable);
    	
    	
    	//draw the buses on the map
        for (BusLocation busLocation : busLocations)
        {
        	GeoPoint point = new GeoPoint((int)(busLocation.latitude * 1000000), (int)(busLocation.longitude * 1000000));
        	
        	String title = busLocation.makeTitle();
        	
        	//the title is displayed when someone taps on the icon
        	OverlayItem overlay = new OverlayItem(point, title, title);
        	busOverlay.addOverlay(overlay);
        }
        overlays.add(busOverlay);

        //make sure we redraw map
        mapView.invalidate();
        
        if (finalMessage != null)
        {
        	publishProgress(finalMessage);
        }
        
	}
}
