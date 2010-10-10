package boston.Bus.Map.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import boston.Bus.Map.data.Directions;
import boston.Bus.Map.data.RouteConfig;
import boston.Bus.Map.data.RoutePool;
import boston.Bus.Map.data.StopLocation;

public class BusPredictionsFeedParser extends DefaultHandler
{
	private static final String stopTagKey = "stopTag";
	private static final String minutesKey = "minutes";
	private static final String epochTimeKey = "epochTime";
	private static final String vehicleKey = "vehicle";
	private static final String dirTagKey = "dirTag";
	private static final String predictionKey = "prediction";
	private static final String predictionsKey = "predictions";
	private static final String routeTagKey = "routeTag";
	
	private final RoutePool stopMapping;
	private StopLocation currentLocation;
	private RouteConfig currentRoute;
	private final Directions directions;
	
	public BusPredictionsFeedParser(RoutePool stopMapping, Directions directions) {
		this.stopMapping = stopMapping;
		this.directions = directions;
	}

	public void runParse(InputStream data) throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxParserFactory.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();
		xmlReader.setContentHandler(this);
		xmlReader.parse(new InputSource(data));
		 
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (localName.equals(predictionsKey))
		{
			String currentRouteTag = attributes.getValue(routeTagKey);
			try
			{
				currentRoute = stopMapping.get(currentRouteTag);
			}
			catch (IOException e)
			{
				StringWriter writer = new StringWriter();
				e.printStackTrace(new PrintWriter(writer));
				Log.e("BostonBusMap", writer.toString());
				currentRoute = null;
			}
			
			currentLocation = null;
			if (currentRoute != null)
			{
				String stopTag = attributes.getValue(stopTagKey);
				currentLocation = currentRoute.getStop(stopTag);
				
				if (currentLocation != null)
				{
					currentLocation.clearPredictions(currentRoute);
				}
			}
		}
		else if (localName.equals(predictionKey))
		{

			if (currentLocation != null && currentRoute != null)
			{
				int minutes = Integer.parseInt(attributes.getValue(minutesKey));

				long epochTime = Long.parseLong(attributes.getValue(epochTimeKey));

				int vehicleId = Integer.parseInt(attributes.getValue(vehicleKey));


				String dirTag = attributes.getValue(dirTagKey);

				currentLocation.addPrediction(minutes, epochTime, vehicleId, dirTag, currentRoute, directions);
			}
		}
	}
	
}