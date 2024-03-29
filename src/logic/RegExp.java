package logic;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.PandaLogger;

//@author A0101810A
/*
 * RegExp class contains regular expression patterns of supported time and date formats
 * It is called by TaskParser.java to extract time and date values from input strings
 * RegExp will match and extract matched date and time patterns to return to TaskParser in string forms
 * 
 * List of examples: 
 * "next Thursday"
 * "tomorrow"
 * "5:15pm"
 * "2359"
 * "17:00"
 * "mon"
 * "1am"
 * 
 * Written by A0101810A - Tan Zheng Jie (Matthew)
 */

public class RegExp {

	private static final int INDEX_FIRST_CASE = 0;
	private static final int INDEX_SECOND_CASE = 1;
	private static final int INDEX_THIRD_CASE = 2;
	
	private static final int INDEX_DAY = 0;
	private static final int INDEX_MONTH = 1;
	private static final int INDEX_YEAR = 2;
	
	private static final int NUM_MATCHER_GROUP_1 = 1;
	private static final int NUM_MATCHER_GROUP_2 = 2;
	private static final int NUM_MATCHER_GROUP_4 = 4;
	private static final int NUM_MATCHER_GROUP_8 = 8;
	private static final int NUM_MATCHER_GROUP_15 = 15;
	private static final int NUM_MATCHER_GROUP_36 = 36;


    /*
     *  Time Input Expressions
     *  These patterns contains keywords and time formats (eg. by 2:15pm)
     *  These patterns are used to compare with user's raw input to extract pure time formats (eg. 2:15pm)
     */
    private static String[] regexTimeInputArray = {
    	// Case 1: "from TIME to TIME", where TIME is HH:MM(am/pm) or HH(am/pm), all cases are insensitive and HH is between 1-12 and MM is between 0 to 59
    	"\\bfrom\\s(([1-9]|1[0-2])(:[0-5][0-9])?[aApP][mM])\\sto\\s(([1-9]|1[0-2])(:[0-5][0-9])?[aApP][mM])\\b",
    	// Case 2: "from TIME to TIME", where TIME is HH:MM or HHMM, H is between 0-23 and MM is between 0 to 59									
    	"\\bfrom\\s(([0-1][0-9]|2[0-3]):?([0-5][0-9]))\\sto\\s(([0-1][0-9]|2[0-3]):?[0-5][0-9])\\b",											
    	// Case 3: "at/by TIME", where TIME can be HH:MM(am/pm) or HH(am/pm) or HH:MM or HHMM, all cases are insensitive
    	"\\b(at|by)\\s((([1-9]|1[0-2])(:[0-5][0-9])?[aApP][mM])|(([0-1][0-9]|2[0-3]):?[0-5][0-9]))\\b"
    	};
    
    /* 
     * Hybrid patterns that captures date and time under one keyword such as "from, by, to" (eg. by 16 mar 5pm)
     * These patterns are used twice, once by parseTime and once by parseDate
     * As of v0.5, regexDateInputArray is merged with this array due to overlap in patterns matched. 
     * Hence, these patterns can be used to detect date patterns followed by an optional time pattern
     */ 
    private static String[] regexHybridInputArray = {
    	// Case 1: (by/on/from/to/at) <date> <time> or (by/on/from/to/at) <date>
    	"\\b(?i)(by |on |from |to |at )(((0?[1-9]|[12]\\d|3[01])[-/](0?[13578]|1[02])[-/](\\d{4}|\\d{2})|(0?[1-9]|[12]\\d|30)[-/](0?[1-9]|1[012])[-/](\\d{4}|\\d{2}))|(((0?[1-9]|[12]\\d|3[01])\\s(jan(uary)?|mar(ch)?|may|jul(y)?|aug(ust)?|oct(ober)?|dec(ember)?)(\\s\\d{4})?|(0?[1-9]|[12]\\d|30)\\s(jan(uary)?|feb(ruary)?|mar(ch)?|apr(il)?|may|jun(e)?|jul(y)?|aug(ust)?|sep(tember)?|oct(ober)?|nov(ember)?|dec(ember)?)(\\s\\d{4})?)))(\\s(([1-9]|1[0-2])(:[0-5][0-9])?[AaPp][Mm]|([0-1][0-9]|2[0-3]):?[0-5][0-9]))?\\b",
        // Case 2: (by/on/from/to/at) <relative date> <time>
    	"\\b(?i)(by |on |from |to |at )(((next|this)?\\s)?((mon(day)?|tues(day)?|wed(nesday)?|thurs(day)?|fri(day)?|sat(urday)?|sun(day)?))|(the day after )?tomorrow)(\\s(([1-9]|1[0-2])(:[0-5][0-9])?[AaPp][Mm]|([0-1][0-9]|2[0-3]):?[0-5][0-9]))?\\b",
    	// Case 3: (by/on/from/to/at) <time> <relative date>
    	"\\b(?i)(by |on |at )(((([1-9]|1[0-2])(:[0-5][0-9])?[AaPp][Mm]|([0-1][0-9]|2[0-3]):?[0-5][0-9])))(((\\snext|this)?\\s)?((mon(day)?|tues(day)?|wed(nesday)?|thurs(day)?|fri(day)?|sat(urday)?|sun(day)?))|( tomorrow))?\\b"
    };
    
    /*
     *  Hash Tag Regular Expressions
     */
    private static String REGEX_HASHTAG = "(?<=^|(?<=[^a-zA-Z0-9-\\.]))#([A-Za-z]+[A-Za-z0-9]+)";
          
    /*
     *  This pattern is used to identify the date format: DD/MM/YYYY or DD-MM-YYYY
     *  and used to interchange DD and MM to the correct parsing of NattyTime
     */
    private static String REGEX_DATE_EXCHANGE_PATTERN = "((0?[1-9]|[12]\\d|3[01])[-/](0?[13578]|1[02])[-/](\\d{4}|\\d{2})|(0?[1-9]|[12]\\d|30)[-/](0?[1-9]|1[012])[-/](\\d{4}|\\d{2}))";
    
    
    /*
     *  Method parses raw input into an array list of date strings
     *  It will match user input with all supported date patterns 
     *  @return an ArrayList<String> of dates 
     */
    public static ArrayList<String> parseDate(String userInput) {
    	ArrayList<String> dateArray = new ArrayList<String>();

    	// Case 1: in the form of proper date format followed by an optional time input (eg. by 14/2/2014 2pm whereby the time is optional) 
    	Pattern pattern = Pattern.compile(regexHybridInputArray[INDEX_FIRST_CASE]);
    	Matcher matcher = pattern.matcher(userInput);
    	while(matcher.find()) {
    		if(matcher.group(NUM_MATCHER_GROUP_2) != null) {
    			dateArray.add(matcher.group(NUM_MATCHER_GROUP_2));
    		}
    	}

    	// Case 2: in the form of hybrid input with relative date followed by an optional time input (eg. by next wed 2pm) 
    	pattern = Pattern.compile(regexHybridInputArray[INDEX_SECOND_CASE]);
    	matcher = pattern.matcher(userInput);   	
    	while(matcher.find()) {
    		if(matcher.group(NUM_MATCHER_GROUP_2) != null) {
    			dateArray.add(matcher.group(NUM_MATCHER_GROUP_2));
    		}
    	}
    	
    	// Case 3: in the form of hybrid input with time followed by an optional relative date (eg. by 5pm tomorrow, at 2359 next monday)
    	pattern = Pattern.compile(regexHybridInputArray[INDEX_THIRD_CASE]);
		matcher = pattern.matcher(userInput);
		while(matcher.find()) {
			if(matcher.group(NUM_MATCHER_GROUP_8) != null) {
				dateArray.add(matcher.group(NUM_MATCHER_GROUP_8));
			}
		}

		return dateArray;
    }
    
    /*
     *  Method parses raw input into an array list of time strings
     *  It will match user input with all supported time patterns 
     *  @return an ArrayList<String> of time
     */
    public static ArrayList<String> parseTime(String userInput) {
    	ArrayList<String> timeArray = new ArrayList<String>();
    	
    	// Case 1: "from TIME to TIME", where TIME can be HH:MM(am/pm) or HH(am/pm)
    	Pattern pattern = Pattern.compile(regexTimeInputArray[INDEX_FIRST_CASE]);
		Matcher matcher = pattern.matcher(userInput);
		if(matcher.find()) {
			timeArray.add(matcher.group(NUM_MATCHER_GROUP_1));
			timeArray.add(matcher.group(NUM_MATCHER_GROUP_4));
			return timeArray;
		}
    	
		// Case 2: "from TIME to TIME", where TIME can be HH:MM or HHMM
		pattern = Pattern.compile(regexTimeInputArray[INDEX_SECOND_CASE]);
		matcher = pattern.matcher(userInput);
		if(matcher.find()) {
			timeArray.add(matcher.group(NUM_MATCHER_GROUP_1));
			timeArray.add(matcher.group(NUM_MATCHER_GROUP_4));
			return timeArray;
		}

		
		// Case 3: in the form of proper date format followed by an optional time input (eg. by 14/2/2014 2pm whereby the time is optional)
		pattern = Pattern.compile(regexHybridInputArray[INDEX_FIRST_CASE]);
		matcher = pattern.matcher(userInput);
		while(matcher.find()) {
			// As pattern contains both time and date, this condition does not add time if date is specified but time is not
			if(matcher.group(NUM_MATCHER_GROUP_36)!=null) {
				timeArray.add(matcher.group(NUM_MATCHER_GROUP_36));
			}
		}
    	
    	// Case 4: in the form of relative date hybrid input followed by optional time input (eg. by next wed 2pm)  
    	pattern = Pattern.compile(regexHybridInputArray[INDEX_SECOND_CASE]);
    	matcher = pattern.matcher(userInput);   	
    	while(matcher.find()) {
    		// As pattern contains both time and date, this condition does not add time if date is specified but time is not
    		if(matcher.group(NUM_MATCHER_GROUP_15) != null) {
    			timeArray.add(matcher.group(NUM_MATCHER_GROUP_15));
    		}
    	}
    	
		// Case 5: in the form of hybrid input with time followed by an optional relative date (eg. by 5pm tomorrow, at 2359 next monday)
		pattern = Pattern.compile(regexHybridInputArray[INDEX_THIRD_CASE]);
		matcher = pattern.matcher(userInput);
		while(matcher.find()) {
			// As pattern contains both time and date, this condition does not add time if date is specified but time is not
			if(matcher.group(NUM_MATCHER_GROUP_2) != null) {
				timeArray.add(matcher.group(NUM_MATCHER_GROUP_2));
			}
			return timeArray;
		}
    	
    	return timeArray; 
    }

    /*
     *  Method will remove all matched patterns to obtain task description
     *  @returns task description:String
     */
	public static String parseDescription(String taskDescription) {
		taskDescription = removeHybridPatterns(taskDescription);
		taskDescription = removeTimePatterns(taskDescription);
		taskDescription = removeHashtagPatterns(taskDescription);
		PandaLogger.getLogger().info("REGEX - Task Description parsed and obtained: " + taskDescription);
		return taskDescription.trim();
	}
	
	private static String removeHybridPatterns(String taskDescription) {
		for(int i=0; i<regexHybridInputArray.length; i++) {
			taskDescription = taskDescription.replaceAll(regexHybridInputArray[i], "");
		}
		return taskDescription;
		
	}
		
	private static String removeTimePatterns(String taskDescription) {
		for(int i=0; i<regexTimeInputArray.length; i++) {
			taskDescription = taskDescription.replaceAll(regexTimeInputArray[i], "");
		}
		return taskDescription;
		
	}
	
	private static String removeHashtagPatterns(String taskDescription) {
		taskDescription = taskDescription.replaceAll(REGEX_HASHTAG, "");
		return taskDescription;
	}
	
	/*
	 *  Method will attempt to parse hashtags given user input
	 *  @return a list of hashtags or empty list if no hashtag exists
	 */
	public static ArrayList<String> parseHashtag(String userInput) {
		ArrayList<String> hashtag = new ArrayList<String>();
		Pattern pattern = Pattern.compile(REGEX_HASHTAG);
		Matcher matcher = pattern.matcher(userInput);
		while(matcher.find()) {
			hashtag.add(userInput.substring(matcher.start(), matcher.end()));
		}
		PandaLogger.getLogger().info("REGEX - Hashtag obtained: " + hashtag);
		return hashtag;
	}
	
	/*
	 *  Method changes DD-MM-YYYY format to MM-DD-YYYY to ensure correct parsing by NattyTime that only supports USA date format
	 *  @return a modified userInput with switched date format
	 */
	public static String changeDateFormat(String userInput) {
		Pattern pattern = Pattern.compile(REGEX_DATE_EXCHANGE_PATTERN);
		Matcher matcher = pattern.matcher(userInput);
		while(matcher.find()) {
			String tempDate = userInput.substring(matcher.start(), matcher.end());
			String[] dateStringArray = tempDate.split("[-/]");
			String newDate = dateStringArray[INDEX_MONTH] + "/" + dateStringArray[INDEX_DAY] + "/" + dateStringArray[INDEX_YEAR];
			userInput = userInput.replace(tempDate, newDate);
			PandaLogger.getLogger().info("REGEX - Dates switched: " + tempDate + " to " + newDate);
		}
		return userInput;
	}
}