package tools;

import java.util.Calendar;
import java.util.Map;

public class DayOfWeekService {

    public static Map<String, String> getCurrentDayOfWeek(){
        Calendar calendar = Calendar.getInstance();
        var day = calendar.get(Calendar.DAY_OF_WEEK);

        var dayString = switch (day) {
            case Calendar.SUNDAY -> "Sunday";
            case Calendar.MONDAY -> "Monday";
            case Calendar.TUESDAY -> "Tuesday";
            case Calendar.WEDNESDAY -> "Wednesday";
            case Calendar.THURSDAY -> "Thursday";
            case Calendar.FRIDAY -> "Friday";
            case Calendar.SATURDAY -> "Saturday";
            default -> "Sunday";
        };

        return Map.of(
                "status", "success",
                "day", dayString);
    }
}
