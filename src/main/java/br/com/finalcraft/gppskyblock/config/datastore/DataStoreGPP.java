package br.com.finalcraft.gppskyblock.config.datastore;

import br.com.finalcraft.gppskyblock.GPPSkyBlock;
import br.com.finalcraft.gppskyblock.Island;
import br.com.finalcraft.gppskyblock.Utils;
import br.com.finalcraft.gppskyblock.integration.wrapper.griefpreventionplus.WrGPPClaim;
import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.ClaimResult;
import net.kaikk.mc.gpp.ClaimResult.Result;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class DataStoreGPP extends DataStore {

	public String dbUrl, username, password;

	public DataStoreGPP(GPPSkyBlock instance) throws Exception {
		super(instance);
		this.dbUrl = "jdbc:mysql://"+instance.config().dbHostname+"/"+instance.config().dbDatabase;
		this.username = instance.config().dbUsername;
		this.password = instance.config().dbPassword;
		
		try {
			//load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
			throw e;
		}
		
		try {
			this.dbCheck();
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to connect to database.  Check your config file settings. Details: \n"+e.getMessage());
			throw e;
		}
		
		Statement statement = db.createStatement();

		try {
			// Creates tables on the database
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS gppskyblock_islands (player binary(16) NOT NULL, claimid int(11) NOT NULL, sx int(11) NOT NULL, sy int(11) NOT NULL, sz int(11) NOT NULL, PRIMARY KEY (player));");
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to create the necessary database table. Details: \n"+e.getMessage());
			throw e;
		}
		
		ResultSet rs = this.statement().executeQuery("SELECT * FROM gppskyblock_islands");
		islands.clear();

		while (rs.next()) {
			UUID uuid = Utils.toUUID(rs.getBytes(1));
			Claim claim = GriefPreventionPlus.getInstance().getDataStore().getClaim(rs.getInt(2));
			if (claim!=null) {
				islands.put(uuid, new Island(uuid, new WrGPPClaim(claim), new Location(claim.getWorld(), rs.getInt(3)+0.5, rs.getInt(4), rs.getInt(5)+0.5)));
			}
		}
	}

	@Override
	public Island createIsland(UUID uuid) throws Exception {
		if (instance.config().nextRegion > 1822500) {
			throw new Exception("Max amount of islands reached.");
		}
		int[] xz = instance.config().nextRegion();
		
		int bx = xz[0] << 9;
		int bz = xz[1] << 9;
		
		World world = Bukkit.getWorld(instance.config().worldName);
		PlayerData playerData = GriefPreventionPlus.getInstance().getDataStore().getPlayerData(uuid);
		playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()+(((instance.config().radius*2)+1)*2));
		ClaimResult result = GriefPreventionPlus.getInstance().getDataStore().newClaim(world.getUID(), bx+255-instance.config().radius, bz+255-instance.config().radius, bx+255+instance.config().radius, bz+255+instance.config().radius, uuid, null, null, null);
		GriefPreventionPlus.getInstance().getDataStore().savePlayerData(uuid, playerData);
		if (result.getResult()!=Result.SUCCESS) {
			playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks()-(((instance.config().radius*2)+1)*2));
			GriefPreventionPlus.getInstance().getDataStore().savePlayerData(uuid, playerData);
			throw new Exception(result.getReason());
		}
		
		instance.config().nextRegion++;
		instance.config().saveData();
		
		Island island = new Island(uuid, new WrGPPClaim(result.getClaim()));
		try {
			this.addIsland(island);
		} catch (SQLException e) {
			e.printStackTrace();
			GriefPreventionPlus.getInstance().getDataStore().deleteClaim(result.getClaim());
			throw new Exception("data store issue.");
		}
		
		island.reset();
		
		return island;
	}

	void asyncUpdate(List<String> sql) {
		String[] arr = new String[(sql.size())];
		asyncUpdate(sql.toArray(arr));
	}

	void asyncUpdate(String... sql) {
		executor.execute(new DatabaseUpdate(sql));
	}
	
	Future<ResultSet> asyncQuery(String sql) {
		return executor.submit(new DatabaseQuery(sql));
	}
	
	Future<ResultSet> asyncUpdateGenKeys(String sql) {
		return executor.submit(new DatabaseUpdateGenKeys(sql));
	}
	
	synchronized void update(String sql) throws SQLException {
		this.update(this.statement(), sql);
	}
	
	synchronized void update(Statement statement, String sql) throws SQLException {
		statement.executeUpdate(sql);
	}
	
	synchronized void update(String... sql) throws SQLException {
		this.update(this.statement(), sql);
	}
	
	synchronized void update(Statement statement, String... sql) throws SQLException {
		for (String sqlRow : sql) {
			statement.executeUpdate(sqlRow);
		}
	}
	
	synchronized ResultSet query(String sql) throws SQLException {
		return this.query(this.statement(), sql);
	}
	
	synchronized ResultSet query(Statement statement, String sql) throws SQLException {
		return statement.executeQuery(sql);
	}
	
	synchronized ResultSet updateGenKeys(String sql) throws SQLException {
		return this.updateGenKeys(this.statement(), sql);
	}
	
	synchronized ResultSet updateGenKeys(Statement statement, String sql) throws SQLException {
		statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
		return statement.getGeneratedKeys();
	}
	
	synchronized Statement statement() throws SQLException {
		this.dbCheck();
		return this.db.createStatement();
	}
	
	synchronized void dbCheck() throws SQLException {
		if(this.db == null || this.db.isClosed()) {
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.username);
			connectionProps.put("password", this.password);
			
			this.db = DriverManager.getConnection(this.dbUrl, connectionProps); 
		}
	}
	
	synchronized void dbClose()  {
		try {
			if (!this.db.isClosed()) {
				this.db.close();
				this.db=null;
			}
		} catch (SQLException e) {
			
		}
	}

	@Override
	public Island getIsland(UUID playerId) {
		return this.islands.get(playerId);
	}

	@Override
	public void addIsland(Island island) throws Exception {
		this.statement().executeUpdate("INSERT INTO gppskyblock_islands VALUES("+Utils.UUIDtoHexString(island.getOwnerId())+", "+island.getClaim().getID()+", "+island.getSpawn().getBlockX()+", "+island.getSpawn().getBlockY()+", "+island.getSpawn().getBlockZ()+");");
		this.islands.put(island.getOwnerId(), island);
	}

	@Override
	public void removeIsland(Island island) throws Exception {
		this.statement().executeUpdate("DELETE FROM gppskyblock_islands WHERE player = "+Utils.UUIDtoHexString(island.getOwnerId())+" LIMIT 1");
		this.islands.remove(island.getOwnerId());
	}

	@Override
	public void updateIsland(Island island) throws Exception {
		this.statement().executeUpdate("UPDATE gppskyblock_islands SET sx = "+island.getSpawn().getBlockX()+", sy = "+island.getSpawn().getBlockY()+", sz = "+island.getSpawn().getBlockZ()+" WHERE player = "+Utils.UUIDtoHexString(island.getOwnerId())+" LIMIT 1");
	}
	
	private class DatabaseUpdate implements Runnable {
		private String[] sql;
		
		public DatabaseUpdate(String... sql) {
			this.sql = sql;
		}

		@Override
		public void run() {
			try {
				for (String sql : this.sql) {
					if (sql==null) {
						break;
					}
					update(sql);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class DatabaseUpdateGenKeys implements Callable<ResultSet> {
		private String sql;
		
		public DatabaseUpdateGenKeys(String sql) {
			this.sql = sql;
		}
		
		@Override
		public ResultSet call() throws Exception {
			return updateGenKeys(sql);
		}
		
	}
	
	private class DatabaseQuery implements Callable<ResultSet> {
		private String sql;
		
		public DatabaseQuery(String sql) {
			this.sql = sql;
		}
		
		@Override
		public ResultSet call() throws Exception {
			return query(sql);
		}
		
	}
}
