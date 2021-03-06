package boston.Bus.Map.transit;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import boston.Bus.Map.data.BusLocation;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.IAlerts;
import boston.Bus.Map.data.Locations;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.RouteTitles;
import boston.Bus.Map.data.Selection;
import boston.Bus.Map.data.StopLocation;
import boston.Bus.Map.data.TransitDrawables;

public interface ITransitSystem {

	public abstract void setDefaultTransitSource(TransitDrawables busDrawables,
			TransitDrawables subwayDrawables,
			TransitDrawables commuterRailDrawables,
			TransitDrawables hubwayDrawables,
			Context context);

	public abstract TransitSource getDefaultTransitSource();

	public abstract TransitSource getTransitSource(String routeToUpdate);

	public abstract RouteTitles getRouteKeysToTitles();

	public abstract void refreshData(RouteConfig routeConfig,
			Selection selection, int maxStops, double centerLatitude,
			double centerLongitude,
			ConcurrentHashMap<String, BusLocation> busMapping,
			RoutePool routePool, Directions directions, Locations locations)
			throws IOException, ParserConfigurationException, SAXException;

	public abstract String searchForRoute(String indexingQuery,
			String lowercaseQuery);

	public abstract StopLocation createStop(float latitude, float longitude,
			String stopTag, String stopTitle,
			String route);

	IAlerts getAlerts();
}