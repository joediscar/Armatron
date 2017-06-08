package guru.syzygy.armatron;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Map;

/**
 * This class handles saving data to the application's preferences.  Generally it should
 * be instantiated at the applications Main Activity, then you simply call the appropriate
 * static methods to do things
 *
 * Created by discar on 6/7/2017.
 */

public class Prefs {
    private static SharedPreferences prefs;
    static String PREFS_KEY = "guru.syzygy.armatron.APP_PREFERENCES";

    /**
     * Initialize the preferences.
     *
     * @param context
     * @return
     */
    public static SharedPreferences getInstance(Context context) {
        if (prefs == null) {
            prefs =context.getSharedPreferences(PREFS_KEY, Activity.MODE_PRIVATE);
        }
        return prefs;
    }

    /**
     * Take a string ArrayList and convert it into a single string...
     * we use this method to "serialize" macros.  The special character "|"
     * Is used to separate lines.  This is okay since macros don't use
     * "|".
     *
     * @param array
     * @return
     */
    private static String serializeArray(ArrayList<String> array) {
        StringBuffer s = new StringBuffer();
        for (String a: array) {
            if (s.length() > 0) s.append("|");
            s.append(a);
        }
        return s.toString();
    }


    /**
     * Take a serialized ArrayList and convert back to its original ArrayList
     *
     * @param string
     * @return
     */
    private static ArrayList<String> deserializeArray(String string) {
        String a[] = string.split("\\|");
        ArrayList<String> array = new ArrayList<String>();
        for (String aa: a) {
            array.add(aa);
        }
        return array;
    }


    /**
     * Save the macro.  The filename gets translated to have a prefix of "MACRO-"
     * so that we can later identify it as a macro.
     *
     * @param filename - filename of the macro.
     * @param macro - contents of the macro
     */
    public static void saveMacro(String filename, ArrayList<String> macro) {
        String saveAs = "MACRO-"+filename;
        SharedPreferences.Editor editor = prefs.edit();
        String macro_serialized = serializeArray(macro);
        editor.putString(saveAs, macro_serialized);
        editor.commit();
    }

    /**
     * Get all the names of the macros that have been saved.  That's why we prefix
     * the filenames with "MACRO-"
     *
     * @return
     */
    public static ArrayList<String> getMacroNames() {
        ArrayList<String> array = new ArrayList<String>();

        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry: allEntries.entrySet()) {
            String key = entry.getKey();
            System.out.println("MACRO KEY: "+key);
            String value = entry.getValue().toString();
            if (key.startsWith("MACRO-")) {
                array.add(key.substring(6));
            }
        }
        return array;
    }

    /**
     * Returns the macro that has the given name.   NOTE: the macroNAME
     * does not have the word "MACRO-" prepended.
     *
     * @param macroName
     * @return
     */
    public static ArrayList<String> getMacro(String macroName) {
        String filename = "MACRO-"+macroName;
        String content = prefs.getString(filename, "");
        if (content.equals("")) return null;
        return deserializeArray(content);
    }

}
