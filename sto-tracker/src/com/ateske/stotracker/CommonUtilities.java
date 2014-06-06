package com.ateske.stotracker;

import java.util.Calendar;

import com.ateske.stotracker.ApplicationController.Days;

import android.content.SharedPreferences;
import android.graphics.Color;

public class CommonUtilities {
	
	private static SharedPreferences m_preferenceManager;
	
	public static void setPreferenceManager(SharedPreferences preferenceManager)
	{
		m_preferenceManager = preferenceManager;
	}
	
	public static boolean getShowAllStops()
	{
		return m_preferenceManager.getBoolean("show_all_stops", true);
	}
	
	public static boolean getShowRouteDirections()
	{
		return m_preferenceManager.getBoolean("show_route_directions", true);
	}
	
	public static int getSelectedTheme()
	{
        String theme = m_preferenceManager.getString("example_list", "0");
        
        switch (theme)
        {
        case "0":
        	return android.R.style.Theme_Holo_Light;
        case "1":
        	return android.R.style.Theme_Holo;
        case "2":
        	return android.R.style.Theme_Holo_Light_DarkActionBar;
        }
        
        //We should never reach this code... but if something goes wrong,
        //default to holo light.
        return android.R.style.Theme_Holo_Light;
	}
	
	public static int getEnabledTextColor()
	{
		if (isDarkThemeSelected())
			return Color.WHITE;
		
		return Color.BLACK;
	}
	
	public boolean isStringTime(String string)
	{
		String[] text = string.split(" ");
		if (text.length == 2 && (text[1].equals("AM") || text[1].equals("PM")))
			return true;
		return false;
	}
	
	public static boolean isBusTimeInPast(String time, Days busDay)
	{			
		String[] text = time.split(" ");
		if (text.length == 2 && (text[1].equals("AM") || text[1].equals("PM")))
		{
			String[] timeComponents = text[0].split(":");
			
			int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			//currentHour = 7;
			int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);
			int currentDay_int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
			
			Days currentDay = Days.WEEKDAY;
			if (currentDay_int == Calendar.SATURDAY)
				currentDay = Days.SATURDAY;
			if (currentDay_int == Calendar.SUNDAY)
				currentDay = Days.SUNDAY;
			
			if (!currentDay.equals(busDay))
				return true;
			
			int busHour = Integer.parseInt(timeComponents[0]);
			int busMinute = Integer.parseInt(timeComponents[1]);
			
			if (text[1].equals("PM") && busHour != 12)
			{
				busHour = busHour +  12;
			}
			
			if (currentHour == 0)
			{
				if ( busHour == 12 && text[1].equals("AM") && currentMinute < busMinute)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
			else if (busHour == 12 && text[1].equals("AM"))
			{
				return false;
			}
			else
			{
				if  (currentHour >  busHour || (currentHour == busHour && currentMinute > busMinute))
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			
		}
		
		return false;
	}
	
	private static boolean isDarkThemeSelected()
	{
		return m_preferenceManager.getString("example_list", "0").equals("1");
	}
	
	
}
