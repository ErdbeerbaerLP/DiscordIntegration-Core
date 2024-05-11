package de.erdbeerbaerlp.dcintegration.common.storage.linking.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;

import java.util.concurrent.TimeUnit;

public abstract class DBInterface {
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /**
     * Connects to the database
     */
    public abstract void connect();

    /**
     * Initializes the database (adding tables, rows and columns)<br>
     * Gets called every start, so use commands like IF NOT EXISTS
     */
    public abstract void initialize();

    /**
     * Check if the database is connected
     */
    public abstract boolean isConnected();

    /**
     * Adds a new player link to the database or replaces the existing one if discord id is the same
     */
    public abstract void addLink(PlayerLink link);

    /**
     * Deletes a player link from the database
     */
    public abstract void removeLink(String id);
    /**
     * Gets the link in the database using floodgate uuid
     */
    public abstract PlayerLink[] getAllLinks();

    public class DBKeepalive extends Thread {
        DBKeepalive(){
            setDaemon(true);
            setName("Database Keepalive");
        }

        private boolean alive = true;

        public boolean isDBAlive() {
            return alive;
        }


        @Override
        public void run() {
            while(true){
                alive = DBInterface.this.isConnected();
                if (!alive) {
                    System.err.println("Attempting Database reconnect...");
                    DBInterface.this.connect();
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

}
