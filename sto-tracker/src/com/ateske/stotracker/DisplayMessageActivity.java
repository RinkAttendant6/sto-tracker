package com.ateske.stotracker;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Xml;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class DisplayMessageActivity extends Activity implements OnItemClickListener {
	
	private class Stop
	{
		public LinkedHashMap<String, String> stops = new LinkedHashMap<String, String>();
	}
	private class Day
	{
		//Day List -> Stop list (e.g. Saturday --> [stop1, stop2...])
		public LinkedHashMap<Days, Stop> day = new LinkedHashMap<Days, Stop>();
	}
	private class DirectionList
	{
		//Direction name -> daylist (e.g. Ottawa via portage --> [weekday, sat, sun]
		public LinkedHashMap<String, Day> direction = new LinkedHashMap<String, Day>();
	}
	private class BusSchedule
	{
		//Route -> Direction (e.g. 11 --> [dir1, dir2]
		public LinkedHashMap<String, DirectionList> routes = new LinkedHashMap<String, DirectionList>();
		
	}
	
	private enum TabMode { DIRECTION, DAY }
	private enum Days { WEEKDAY, SATURDAY, SUNDAY }
	
	private class BusScheduleXmlParser implements TabListener {
	    private BusSchedule schedule;
	    private String selectedRoute = null;
	    private String selectedDirection = null;
	    private String selectedStop = null;
	    private int selectedTab = 0;
	    private TabMode currentTabMode = null;
	    
	    private void parseXml() throws XmlPullParserException, IOException{
	    	
	    	XmlPullParser xpp = getParser();
	    	int eventType = xpp.getEventType();
	    	
	    	schedule = new BusSchedule();	 
	    	
	    	String currentBusRoute = null;
	    	String currentDirection = null;
	    	Days currentDay = null;
	    	String currentStop = null;
	    	boolean expectStopInfo = false;
	
	    	while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("bus_schedule"))){
	    		if (eventType == XmlPullParser.START_TAG){
	    			String name = xpp.getAttributeValue(null, "name");
	    			switch (xpp.getName())
	    			{
	    			case "route":
	    				currentBusRoute = name;
	    				schedule.routes.put(name, new DirectionList());
	    				break;
	    			case "direction":
	    				currentDirection = name;
	    				schedule.routes.get(currentBusRoute).direction.put(currentDirection, new Day());
	    				break;
	    			case "day":	    				
	    				switch (name)
	    				{
	    				case "Weekday":
	    					currentDay = Days.WEEKDAY;
	    					break;
	    				case "Saturday":
	    					currentDay = Days.SATURDAY;
	    					break;
	    				case "Sunday":
	    					currentDay = Days.SUNDAY;  
	    					break;	
	    				default:
	    					throw new XmlPullParserException("Invalid day");
	    				}
	    				schedule.routes.get(currentBusRoute).direction.get(currentDirection).day.put(currentDay, new Stop());	    				
	    				break;
	    			case "stop":
	    				currentStop = name;
	    				expectStopInfo = true;
	    				break;
	    			}	  	
	    		}
	    		if (eventType == XmlPullParser.TEXT && expectStopInfo)
	    		{
	    			expectStopInfo = false;
	    			schedule.routes.get(currentBusRoute).direction.get(currentDirection).day.get(currentDay).stops.put(currentStop,  xpp.getText());
	    		}
	    		eventType = xpp.next();
	    	}	
	    }
 	    
	    public boolean back()
	    {
	    	if (selectedStop != null){
	    		selectedStop = null;
	    	}
	    	else if (selectedRoute != null){
	    		selectedDirection = null;
	    		selectedStop = null;
	    		selectedRoute = null;
	    	}
	    	else
	    		return false;
	    	return true;
	    }
	    
	    public boolean addSelectionInfo(String selectionInfo)
	    {
	    	if (selectedRoute == null){
	    		selectedRoute = selectionInfo;
	    	}
	    	else if (selectedStop == null){
	    		selectedDirection = actionBar.getTabAt(selectedTab).getText().toString();
	    		selectedStop = selectionInfo;
	    	}
	    	else
	    		return false;
	    	return true;
	    }
	    
	    public String[] getCurrentView()
	    {
	        actionBar.setDisplayHomeAsUpEnabled(true);
	        actionBar.setHomeButtonEnabled(true);
	        
	    	List<String> selection = new ArrayList<String>();
	    	
	    	try{
		    	if (selectedRoute == null){
		    		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		    		currentTabMode = null;
		    		
		    		selection = getBusRoutes(); //display routes
		    		setTitle(getString(R.string.route_page_title));
			        actionBar.setDisplayHomeAsUpEnabled(false);
			        actionBar.setHomeButtonEnabled(false);
		    	}
		    	else if (selectedStop == null){		    		
		    		List<String> directions = getDirections();
		    		
		    		if (currentTabMode != TabMode.DIRECTION)
		    		{
		    			selectedDirection = directions.get(0);
		    			currentTabMode = TabMode.DIRECTION;
			    		actionBar.removeAllTabs();
			            actionBar.addTab(actionBar.newTab().setText(directions.get(0)).setTabListener(m_xmlParser));
			            actionBar.addTab(actionBar.newTab().setText(directions.get(1)).setTabListener(m_xmlParser));
			            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		    		}
		    		            
		    		selection = getStops(); //display stop
		    		setTitle("Route " + selectedRoute);
		    	}
		    	else{	
			        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			        
		    		if (currentTabMode != TabMode.DAY)
		    		{
		    			currentTabMode = TabMode.DAY;
			    		actionBar.removeAllTabs();
			            actionBar.addTab(actionBar.newTab().setText(getString(R.string.weekday_label)).setTabListener(m_xmlParser));
			            actionBar.addTab(actionBar.newTab().setText(getString(R.string.saturday_label)).setTabListener(m_xmlParser));
			            actionBar.addTab(actionBar.newTab().setText(getString(R.string.sunday_label)).setTabListener(m_xmlParser));
		    		}
			              
		    		selection = getTimes(); //display times
		    		setTitle(selectedStop);	
		    	}
	    	}
	    	catch(Exception e){
	    		System.out.println("EXCEPTION: " + e);
	    	}
	    	
	    	return selection.toArray(new String[selection.size()]);	    	
	    	
	    }
	   
	    private XmlPullParser getParser() {
	    	
	        try {
	        	AssetManager assetManager = getAssets();
	        	InputStream in = assetManager.open("stoscrape.xml");
	            XmlPullParser parser = Xml.newPullParser();
	            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	            parser.setInput(in, null);
	            parser.nextTag();
	            return parser;
	        } catch (Exception e){
	        	System.out.println("CAUGHT EXCEPTION: " + e);
	        	return null;
	        }
	    }
	    
		private List<String> getBusRoutes() throws XmlPullParserException, IOException {
			
			Set<String> keys = schedule.routes.keySet();
			
			List<String> result = new ArrayList<String>();
			result.addAll(keys);
			return result;
			
		}
		
	    public List<String> getDirections() throws XmlPullParserException, IOException{
	    	
	    	DirectionList directions = schedule.routes.get(selectedRoute);
	    	
	    	List<String> direction = new ArrayList<String>();
	    	
	    	for (Map.Entry<String, Day> entry : directions.direction.entrySet())
	    	{
	    		direction.add(entry.getKey());
	    	}
	    	
	    	return direction;
	    }
		
		private List<String> getStops(){
	    	Stop stop = schedule.routes.get(selectedRoute).direction.get(actionBar.getTabAt(selectedTab).getText().toString()).day.get(Days.WEEKDAY);
	    	
	    	List<String> direction = new ArrayList<String>();
	    	
	    	for (Map.Entry<String, String> entry : stop.stops.entrySet())
	    	{
	    		direction.add(entry.getKey());
	    	}
	    	
	    	return direction;
		}
		
		private List<String> getTimes(){
			
	    	Stop stop = schedule.routes.get(selectedRoute).direction.get(selectedDirection).day.get(getSelectedDay());
	    	String msg = stop == null? getString(R.string.no_service_message) : stop.stops.get(selectedStop);
	    	List<String> times = Arrays.asList(msg.split(","));
	    	
	    	List<String> results = new ArrayList<String>();
	    	
	    	for (int i = 0; i < times.size(); ++i)
	    	{
	    		if (!times.get(i).equals(""))
	    			results.add(times.get(i));
	    	}
	    	
	    	return results;
		}

		private Days getSelectedDay()
		{
			String selectedTabText = actionBar.getTabAt(selectedTab).getText().toString();
			
			if (selectedTabText.equals(getString(R.string.weekday_label)))
				return Days.WEEKDAY;
			if (selectedTabText.equals(getString(R.string.saturday_label)))
				return Days.SATURDAY;
			if (selectedTabText.equals(getString(R.string.sunday_label)))
				return Days.SUNDAY;
			
			return null;
		}
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// do nothing
			
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			
			int newIndex = actionBar.getSelectedNavigationIndex();
			
			if (newIndex == selectedTab){
				return;
			}
			
			if (newIndex < selectedTab)
				forward = false;
			
			selectedTab = newIndex;
			renderView();
			
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// do nothing
			
		}
	}
	
	private BusScheduleXmlParser m_xmlParser = new BusScheduleXmlParser();
	ActionBar actionBar;
	ListView currentView;
	boolean forward = true;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        actionBar = getActionBar();
        
    	try {
    		m_xmlParser.parseXml();
		} catch (Exception e) {
			System.out.println("EXCEPTION WHILE PARSING XML: " + e);
		}
        
        renderView();
    }
    
    private void renderView()
    {
        String[] options = m_xmlParser.getCurrentView();
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_list_item_1, options);
        
        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        
        if (options.length >= 20)
        {
	        listView.setFastScrollEnabled(true);
	        listView.setFastScrollAlwaysVisible(true);
        }

        // Set the text view as the activity layout
        if (currentView != null)
        {
        	if (forward){
            	currentView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.forward_out));
            	listView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.forward_in));
        	}
        	
        	else{
        		forward = true;
            	currentView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.back_out));
            	listView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.back_in));
        	}

        }
        setContentView(listView);
        currentView = listView;
        
    }
    
    
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
    {
    	if (m_xmlParser.addSelectionInfo((String)arg0.getItemAtPosition(arg2)))
    		renderView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == android.R.id.home){
        	onBackPressed();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /** Called when the user clicks the Send button */
    public void sendMessage(String selection) {
    	
    	m_xmlParser.addSelectionInfo(selection);
    	
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
    	if (m_xmlParser.back()){
    		forward = false;
    		renderView();
    		
    	}
    	else
    		super.onBackPressed();
    }
}
