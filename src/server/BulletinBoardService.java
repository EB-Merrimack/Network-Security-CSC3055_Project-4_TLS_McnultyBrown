package server;

import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;
import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;

public class BulletinBoardService {
    private static String configFile = "config.json"; // default
    private static boolean doHelp = false;
    private static boolean doConfig = false;

    /**
     * Prints the help menu.
     */
    public static void usage() {
        System.out.println("usage:");
        System.out.println("  boardd");
        System.out.println("  boardd --config <configfile>");
        System.out.println("  boardd --help");
        System.out.println("options:");
        System.out.println("  -c, --config  Set the config file.");
        System.out.println("  -h, --help    Display the help.");
        System.exit(1);
    }

    /**
     * Loads the configuration information from the configuration file.
     * @param configName the name of the configuration file.
     */
    public static void loadConfig(String configName) {
        try {
            JSONObject configObj = JsonIO.readObject(new File(configName));
            System.out.println("Configuration loaded from: " + configName);
        } catch (FileNotFoundException ex) {
            System.out.println("Configuration file not found.");
            System.exit(1);
        } catch (Exception ex) {
            System.out.println("Error loading configuration file: " + ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Process the command line arguments.
     * @param args the array of command line arguments.
     */
    public static void processArgs(String[] args) {
        OptionParser parser;
        LongOption[] opts = new LongOption[2];
        opts[0] = new LongOption("config", true, 'c');
        opts[1] = new LongOption("help", false, 'h');

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("hc:");

        Tuple<Character, String> currOpt;

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
                case 'h':
                    doHelp = true;
                    break;
                case 'c':
                    doConfig = true;
                    configFile = currOpt.getSecond();
                    break;
                case '?':
                    usage();
                    break;
            }
        }

        if (doConfig && doHelp)
            usage();

        if (doConfig)
            loadConfig(configFile);
        else if (doHelp)
            usage();
        else
            loadConfig("config.json");
    }
}