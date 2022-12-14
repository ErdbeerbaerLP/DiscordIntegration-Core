package de.erdbeerbaerlp.dcintegration.common.storage.database;

import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLink;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerSettings;
import de.erdbeerbaerlp.dcintegration.common.util.Variables;

import java.sql.*;
import java.util.ArrayList;

public class SQLiteInterface extends DBInterface {
    Connection connection = null;

    @Override
    public void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + Variables.discordDataDir.getAbsolutePath() + "/LinkedPlayers.db");
            new DBKeepalive().start();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        try {
            final Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS PlayerLinks(id NOT NULL PRIMARY KEY,mcuuid,fluuid,settings)");
            statement.closeOnCompletion();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        if (connection == null) return false;
        try {
            return connection.isValid(10);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void addLink(PlayerLink link) {
        try {
            final Statement statement = connection.createStatement();
            statement.executeUpdate("insert or replace into PlayerLinks(id,mcuuid,fluuid,settings) values('" + link.discordID + "', '" + link.mcPlayerUUID + "', '" + link.floodgateUUID + "','" + gson.toJson(link.settings) + "')");
            statement.closeOnCompletion();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void removeLink(String id) {
        try {
            final Statement statement = connection.createStatement();
            statement.executeUpdate("delete from PlayerLinks where id='" + id + "'");
            statement.closeOnCompletion();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerLink[] getAllLinks() {
        final ArrayList<PlayerLink> links = new ArrayList<PlayerLink>();
        try {
            final Statement statement = connection.createStatement();
            final ResultSet res = statement.executeQuery("SELECT id,mcuuid,fluuid,settings FROM PlayerLinks");
            while (res.next()) {
                links.add(new PlayerLink(res.getString(1), res.getString(2), res.getString(3), gson.fromJson(res.getString(4), PlayerSettings.class)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return links.toArray(new PlayerLink[0]);
    }


}
