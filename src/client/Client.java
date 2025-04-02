package client;

import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;

public class Client {
    private static String user;
    private static String host;
    private static int port;
    private static boolean create = false;
    private static boolean post = false;
    private static boolean get = false;
    private static String recvr;
    private static String message;
    private static String privKey;

    /**
     * Prints the help menu.
     */
    public static void usage() {
        System.out.println("usage:");
        System.out.println("  client --create --user <user> --host <host> --port <portnum>");
        System.out.println("  client --post <msg> --user <user> --recvr <user> --host <host> --port <portnum>");
        System.out.println("  client --get --key <privkey> --user <user> --host <host> --port <portnum>");
        System.out.println("options:");
        System.out.println("  -c, --create     Create a new account.");
        System.out.println("  -o, --post       Post a message.");
        System.out.println("  -g, --get        Get all posts.");
        System.out.println("  -r, --recvr      The message receiver.");
        System.out.println("  -k, --key        The private key.");
        System.out.println("  -u, --user       The username.");
        System.out.println("  -h, --host       The host name of the server.");
        System.out.println("  -p, --port       The port number for the server.");
        System.exit(1);
    }

    /**
     * Process the command line arguments based on the defined usage.
     * @param args the array of command line arguments.
     */
    public static void processArgs(String[] args) {
        if (args.length == 0) {
            usage();
        }

        OptionParser parser;
        LongOption[] opts = new LongOption[8];
        opts[0] = new LongOption("create", false, 'c');
        opts[1] = new LongOption("post", true, 'o');
        opts[2] = new LongOption("get", false, 'g');
        opts[3] = new LongOption("recvr", true, 'r');
        opts[4] = new LongOption("key", true, 'k');
        opts[5] = new LongOption("user", true, 'u');
        opts[6] = new LongOption("host", true, 'h');
        opts[7] = new LongOption("port", true, 'p');

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("cgo:r:k:u:h:p:");

        Tuple<Character, String> currOpt;

        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
                case 'c':
                    create = true;
                    break;
                case 'o':
                    post = true;
                    message = currOpt.getSecond();
                    break;
                case 'g':
                    get = true;
                    break;
                case 'r':
                    recvr = currOpt.getSecond();
                    break;
                case 'k':
                    privKey = currOpt.getSecond();
                    break;
                case 'u':
                    user = currOpt.getSecond();
                    break;
                case 'h':
                    host = currOpt.getSecond();
                    break;
                case 'p':
                    try {
                        port = Integer.parseInt(currOpt.getSecond());
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid port number.");
                        usage();
                    }
                    break;
                case '?':
                default:
                    usage();
                    break;
            }
        }

        // Validate required arguments based on the selected command
        if (create) {
            if (user == null || host == null || port == 0) {
                System.err.println("Error: Missing required arguments for --create.");
                usage();
            }
            System.out.println("Creating account for user: " + user);
            // Account creation logic here
        } else if (post) {
            if (user == null || host == null || port == 0 || recvr == null || message == null) {
                System.err.println("Error: Missing required arguments for --post.");
                usage();
            }
            System.out.println("Posting message from " + user + " to " + recvr + ": " + message);
            // Message posting logic here
        } else if (get) {
            if (user == null || host == null || port == 0 || privKey == null) {
                System.err.println("Error: Missing required arguments for --get.");
                usage();
            }
            System.out.println("Retrieving posts for user: " + user);
            // Post retrieval logic here
        } else {
            System.err.println("Error: No valid action specified.");
            usage();
        }
    }
}
