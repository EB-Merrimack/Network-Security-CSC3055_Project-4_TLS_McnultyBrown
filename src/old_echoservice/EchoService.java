/* 
 * Copyright (C) 2023 - 2025  Zachary A. Kissel 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package old_echoservice;

import merrimackutil.util.NonceCache;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import merrimackutil.cli.LongOption;
import merrimackutil.cli.OptionParser;
import merrimackutil.util.Tuple;
import old_echoservice.model.Configuration;

/**
 * This class is the main class for the service.
 * @author Zach Kissel
 */
 public class EchoService
 {
   private static Configuration config = null;
   private static boolean doHelp = false;
   private static boolean doConfig = false;
   private static String configName = null;
   private static NonceCache nonceCache = null;

   /**
    * Prints the help menu.
    */
    public static void usage()
    {
      System.out.println("usage:");
      System.out.println("  echoservice");
      System.out.println("  echoservice --config <configfile>");
      System.out.println("  echoservice --help");
      System.out.println("options:");
      System.out.println("  -c, --config\t\tSet the config file.");
      System.out.println("  -h, --help\t\tDisplay the help.");
      System.exit(1);
    }


   /**
    * Loads the configuration information from the configuration file.
    * @param configName the name of the configuration file.
    */
   public static void loadConfig(String configName)
   {

     JSONObject configObj = null;
     try
     {
       configObj = JsonIO.readObject(new File(configName));
     }
     catch (FileNotFoundException ex)
     {
       System.out.println("Configruation file not found.");
       System.exit(1);
     }
     try
     {
       config = new Configuration(configObj);
     }
     catch (InvalidObjectException ex)
     {
       System.out.println("Invalid configuration file.");
       System.exit(1);
     }
   }

   /**
    * Process the command line arguments.
    * @param args the array of command line arguments.
    */
   public static void processArgs(String[] args)
   {
     OptionParser parser;

     LongOption[] opts = new LongOption[2];
     opts[0] = new LongOption("help", false, 'h');
     opts[1] = new LongOption("config", true, 'c');


     Tuple<Character, String> currOpt;

     parser = new OptionParser(args);
     parser.setLongOpts(opts);
     parser.setOptString("hc:");


     while (parser.getOptIdx() != args.length)
     {
       currOpt = parser.getLongOpt(false);

       switch (currOpt.getFirst())
       {
         case 'h':
           doHelp = true;
         break;
         case 'c':
           doConfig = true;
           configName = currOpt.getSecond();
         break;
         case '?':
           usage();
         break;
       }
     }

     // Verify that that this options are not conflicting.
     if (doConfig &&  doHelp)
         usage();

     if (doConfig)
      loadConfig(configName);
     else if (doHelp)
      usage();

     if (!doConfig && !doHelp)
      loadConfig("config.json");
   }

   public static void main(String[] args) throws UnknownHostException, IOException
   {
      processArgs(args);

      @SuppressWarnings("resource")
      ServerSocket serve = new ServerSocket(config.getPort());

      // Create the service's nonce cache.
      nonceCache = new NonceCache(32, 30);

      // Construct a new thread pool to handle connections.
      ExecutorService pool = Executors.newFixedThreadPool(10);

      while (true)
      {
        Socket sock = serve.accept();
        pool.submit(new ConnectionHandler(sock, config.doDebug(), config.getServiceName(), config.getSecret(), nonceCache));
      }
   }
 }
