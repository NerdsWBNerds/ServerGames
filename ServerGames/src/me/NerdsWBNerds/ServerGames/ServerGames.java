package me.NerdsWBNerds.ServerGames;

import static org.bukkit.ChatColor.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.diddiz.LogBlock.QueryParams;
import me.NerdsWBNerds.ServerGames.Objects.Chests;
import me.NerdsWBNerds.ServerGames.Objects.Spectator;
import me.NerdsWBNerds.ServerGames.Objects.Tribute;
import me.NerdsWBNerds.ServerGames.Timers.Deathmatch;
import me.NerdsWBNerds.ServerGames.Timers.Finished;
import me.NerdsWBNerds.ServerGames.Timers.Game;
import me.NerdsWBNerds.ServerGames.Timers.Lobby;
import me.NerdsWBNerds.ServerGames.Timers.CurrentState;
import me.NerdsWBNerds.ServerGames.Timers.Setup;
import me.NerdsWBNerds.ServerGames.Timers.CurrentState.State;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import de.diddiz.LogBlock.LogBlock;

public class ServerGames extends JavaPlugin implements Listener{
	public String path = "plugins/ServerGames";
	
	public SGListener Listener = new SGListener(this);
	public static Server server;
	public static Logger log;

	public int max = 24;
	public static State state = null;
	public static CurrentState game = null;
	public HashMap<String, Integer> score = new HashMap<String, Integer>();
	public static ArrayList<Location> tubes = new ArrayList<Location>();
	public static ArrayList<String> bugs = new ArrayList<String>();
	public static ArrayList<Tribute> tributes = new ArrayList<Tribute>();
	public static ArrayList<Spectator> spectators = new ArrayList<Spectator>();
	public static ArrayList<Chunk> loaded = new ArrayList<Chunk>();
	public static Location cornacopia = null, waiting = null;
	
	@SuppressWarnings("unused")
	public void onEnable(){

        final PluginManager pm = getServer().getPluginManager();
        final Plugin plugin = pm.getPlugin("LogBlock");

		server = this.getServer();
		log = this.getLogger();

		server.getPluginManager().registerEvents(Listener, this);
		
		new File(path).mkdir();
		load();
		
		hardResetPlayers();

		tpAll(waiting);
		
		//cornacopia.getWorld().setAutoSave(false);
	}
	
	public void onDisable(){
		clearAll();
	}
	
	public void clearAll(){
		this.cancelTasks();
		
		save();
		
		tubes.clear();
		tributes.clear();
		spectators.clear();
		loaded.clear();
		spectators.clear();
		game = null;
		state = null;
		cornacopia = null;
		waiting = null;
	}

	public void resetPlayers(){
		ArrayList<Tribute> hold = new ArrayList<Tribute>();
		for(Tribute t: tributes){
			hold.add(t);
		}
		
		for(Spectator s: spectators){
			hold.add(new Tribute(s.player));
		}
		
		ServerGames.spectators.clear();
		ServerGames.tributes = hold;
	}
	
	public void hardResetPlayers(){
		for(Player p: server.getOnlinePlayers()){
			tributes.add(new Tribute(p));
		}
	}
	
	public void startLobby(){
		server.broadcastMessage(GOLD + "[ServerGames]" + GREEN + " Countdown started.");
		
		clearAll();
		
		load();
		tpAll(waiting);
		
		for(Player p: server.getOnlinePlayers()){
			showAllFor(p);
			showPlayer(p);
		}
		
		this.resetPlayers();
		
		state = State.LOBBY;
		game = new Lobby(this);
		
		startTimer();
	}
	
	public void startSetup(){
		clearAll();
		
		int i = 0;

		for(Player p : server.getOnlinePlayers()){
			if(isTribute(p)){
				if(i >= ServerGames.tubes.size())
					i = 0;
				
				Location to = ServerGames.tubes.get(i);
				showAllFor(p);
				p.setSprinting(false);
				p.setSneaking(false);
				p.setPassenger(null);
				p.setGameMode(GameMode.SURVIVAL);
				p.setFireTicks(0);
				clearItems(p);
				getTribute(p).start = to;
				p.teleport(toCenter(to));
				
				i++;
			}
		}

		cornacopia.getWorld().getEntities().clear();
		cornacopia.getWorld().setThundering(false);
		cornacopia.getWorld().setTime(0);
		cornacopia.getWorld().setWeatherDuration(0);
		cornacopia.getWorld().setStorm(false);


        // ----- WORLD RESETTING -----
		loaded.clear();
        Chests.resetChests();
        this.getServer().broadcastMessage(GOLD + "[ServerGames] " + GREEN + "MAP IS RESETTING!");


        LogBlock logblock = (LogBlock)getServer().getPluginManager().getPlugin("LogBlock");
        QueryParams params = new QueryParams(logblock);

        params.world = cornacopia.getWorld();
        params.silent = false;

        try {
            logblock.getCommandsHandler().new CommandRollback(this.getServer().getConsoleSender(), params, false);
        } catch(Exception e){}

        this.getServer().broadcastMessage(GOLD + "[ServerGames] " + GREEN + "Map has been reset!");
        
        for(Entity e : cornacopia.getWorld().getEntities()){
            if(e.getType() == EntityType.DROPPED_ITEM || e.getType() == EntityType.CREEPER || e.getType() == EntityType.SKELETON || e.getType() == EntityType.SPIDER || e.getType() == EntityType.ENDERMAN || e.getType() == EntityType.ZOMBIE){
                e.remove();
            }
        }

        // ----- WORLD RESETTING -----
		
        load();
        
		state = State.SET_UP;
		game = new Setup(this);
		
		startTimer();
	}
	
	public void startGame(){
		state = State.IN_GAME;
		
		for(Player p : server.getOnlinePlayers()){
			p.setHealth(20);
			p.setFoodLevel(20);
			this.clearItems(p);
		}

		clearAll();
		load();
		
		server.broadcastMessage(GOLD + "[ServerGames]" + GREEN + " Let the game begin!");

		game = new Game(this);
		
		startTimer();
	}
	
	public void startDeath(){
		server.broadcastMessage(GOLD + "[ServerGames] " + RED + "The deathmatch will now start.");
		server.broadcastMessage(GOLD + "[ServerGames] " + RED + "Do not run from the deathmatch.");
		for(Tribute t: ServerGames.tributes){
			t.player.teleport(t.start);
			tell(t.player, GOLD + "[ServerGames] " + GREEN + "You have made it to the deathmatch.");
		}
		for(Spectator s: ServerGames.spectators){
			s.player.teleport(ServerGames.cornacopia);
		}

		clearAll();
		load();
		
		state = State.DEATHMATCH;
		game = new Deathmatch(this);
		
		startTimer();
	}
	
	public void startFinished(){
		clearAll();
		load();
		
		state = State.DONE;
		game = new Finished(this);
		
		startTimer();
	}
	
	public void stopAll(){
		clearAll();
		load();
	}
	
	public void startTimer(){
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, game, 20L, 20L);
	}
	
	public Tribute getTribute(Player player){
		for(Tribute t : tributes){
			if(t.player == player)
				return t;
		}
		
		return null;
	}

	public Spectator getSpectator(Player player){
		for(Spectator t : spectators){
			if(t.player == player)
				return t;
		}
		
		return null;
	}
	
	public void tpAll(Location l){
		for(Tribute t: tributes){
			t.player.teleport(l);
		}
		for(Spectator s: spectators){
			s.player.teleport(l);
		}
	}
	
	public void cancelTasks(){
		this.getServer().getScheduler().cancelAllTasks();
		game = null;
	}
	
	public static void showPlayer(Player player){
		for(Player p : server.getOnlinePlayers()){
			p.showPlayer(player);
		}
	}
	
	public static void hidePlayer(Player player){
		for(Player p : server.getOnlinePlayers()){
			p.hidePlayer(player);
		}
	}
	
	public static void hideAllFrom(Player player){
		for(Player p : server.getOnlinePlayers()){
			player.hidePlayer(p);
		}
	}
	
	public static void showAllFor(Player player){
		for(Player p : server.getOnlinePlayers()){
			player.showPlayer(p);
		}
	}
	
	public Location toCenter(Location l){
		return new Location(l.getWorld(), l.getBlockX() + .5, l.getBlockY(), l.getBlockZ() + .5);
	}
	
	public void removeTribute(Player p){
		tributes.remove(this.getTribute(p));
	}
	
	public void removeSpectator(Player p){
		spectators.remove(getSpectator(p));
	}
	
	public int toInt(String s){
		return Integer.parseInt(s);
	}

	public void save(){
		//////////// --------- Tubes ------------ ///////////////
		File file = new File(path + File.separator + "Tubes.loc");
		new File(path).mkdir();
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		 
		ArrayList<String> t = new ArrayList<String>();
		
		for(Location x: tubes){
			t.add(x.getWorld().getName() + "," + x.getBlockX() + "," + x.getBlockY() + "," + x.getBlockZ());
		}
		
		try{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + File.separator + "Tubes.loc"));
			oos.writeObject(t);
			oos.flush();
			oos.close();
		}catch(Exception e){
			e.printStackTrace();
		}

		//////////// --------- Tubes End ------------ ///////////////
		//////////// --------- Cornacopia ------------ ///////////////
		
		if(cornacopia != null){
			file = new File(path + File.separator + "Corn.loc");
			new File(path).mkdir();
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			 
			String c = "";
			c = (cornacopia.getWorld().getName() + "," + cornacopia.getBlockX() + "," + cornacopia.getBlockY() + "," + cornacopia.getBlockZ());
			
			cornacopia.getWorld().setSpawnLocation(cornacopia.getBlockX(), cornacopia.getBlockY(), cornacopia.getBlockZ());
			
			try{
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + File.separator + "Corn.loc"));
				oos.writeObject(c);
				oos.flush();
				oos.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		//////////// --------- Cornacopia End ------------ ///////////////	
		//////////// --------- Waiting ------------ ///////////////

		if(waiting != null){
			file = new File(path + File.separator + "Wait.loc");
			new File(path).mkdir();
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	
			String w = "";
			w = (waiting.getWorld().getName() + "," + waiting.getBlockX() + "," + waiting.getBlockY() + "," + waiting.getBlockZ());
			
			try{
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + File.separator + "Wait.loc"));
				oos.writeObject(w);
				oos.flush();
				oos.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//////////// --------- Waiting End ------------ ///////////////	
		//////////// --------- Score ------------ ///////////////

		if(score != null){
			file = new File(path + File.separator + "Score.loc");
			new File(path).mkdir();
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try{
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + File.separator + "Score.loc"));
				oos.writeObject(score);
				oos.flush();
				oos.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//////////// --------- Score End ------------ ///////////////	
		//////////// --------- Bug ------------ ///////////////

		if(score != null){
			file = new File(path + File.separator + "Bugs.loc");
			new File(path).mkdir();
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try{
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path + File.separator + "Bugs.loc"));
				oos.writeObject(bugs);
				oos.flush();
				oos.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//////////// --------- Bug End ------------ ///////////////
	}
	
	@SuppressWarnings("unchecked")
	public void load(){
		//////////// --------- Tubes ------------ ///////////////
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path + File.separator + "Tubes.loc"));
			Object result = ois.readObject();

			ArrayList<String> t = new ArrayList<String>();
			t = (ArrayList<String>)result;
			
			tubes = new ArrayList<Location>();
			
			for(String x: t){
				String[] split = x.split(",");
				tubes.add(new Location(server.getWorld(split[0]), toInt(split[1]), toInt(split[2]), toInt(split[3])));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		//////////// --------- Tubes End ------------ ///////////////
		//////////// --------- Cornacopia ------------ ///////////////
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path + File.separator + "Corn.loc"));
			Object result = ois.readObject();
			
			String[] split = ((String)result).split(",");
			Location c = new Location(server.getWorld(split[0]), toInt(split[1]), toInt(split[2]), toInt(split[3]));
			
			ServerGames.cornacopia = c; 
		}catch(Exception e){
			e.printStackTrace();
		}
		//////////// --------- Cornacopia End ------------ ///////////////	
		//////////// --------- Waiting ------------ ///////////////
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path + File.separator + "Wait.loc"));
			Object result = ois.readObject();

			String[] split = ((String)result).split(",");
			Location w = new Location(server.getWorld(split[0]), toInt(split[1]), toInt(split[2]), toInt(split[3]));
			
			ServerGames.waiting = w; 
		}catch(Exception e){
			e.printStackTrace();
		}
		//////////// --------- Waiting End ------------ ///////////////	
		//////////// --------- Score ------------ ///////////////
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path + File.separator + "Score.loc"));
			Object result = ois.readObject();
			
			score = (HashMap<String, Integer>) result; 
		}catch(Exception e){
			e.printStackTrace();
		}
		//////////// --------- Score End ------------ ///////////////
		//////////// --------- Score ------------ ///////////////
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path + File.separator + "Bugs.loc"));
			Object result = ois.readObject();
			
			bugs = (ArrayList<String>) result; 
		}catch(Exception e){
			e.printStackTrace();
		}
		//////////// --------- Score End ------------ ///////////////
	}

	public static boolean inGame(){
		if(state == State.IN_GAME)
			return true;
			
		return false;
	}
	
	public static boolean inLobby(){
		if(state == State.LOBBY)
			return true;
		
		return false;
	}
	
	public static boolean inSetup(){
		if(state == State.SET_UP)
			return true;
		
		return false;
	}
	
	public static boolean inDone(){
		if(state == State.DONE)
			return true;
		
		return false;
	}
	
	public static boolean inDeath(){
		if(state == State.DEATHMATCH)
			return true;
		
		return false;
	}
	
	public static boolean inNothing(){
		if(state == null)
			return true;
		
		return false;
	}
	
	public static boolean isOwner(Player player){
		if(player.getName().equalsIgnoreCase("nerdswbnerds") || player.getName().equalsIgnoreCase("brenhein"))
			return true;
		
		return false;
	}
	
	public void clearItems(Player player){
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
	}
	
	public boolean isTribute(Player player){
		if(this.getTribute(player)!=null)
			return true;
		
		return false;
	}
	
	public boolean isSpectator(Player player){
		if(getSpectator(player)!= null){
			return true;
		}
		
		return false;
	}
	
	public void tell(Player p, String m){
		p.sendMessage(m);
	}
	
	public void addScore(Player player, int add){
		if(score.containsKey(player.getName()))
			this.score.put(player.getName(), this.score.get(player.getName()) + add);
		else
			this.score.put(player.getName(), add);
		
		save();
	}
	
	public int getScore(Player player){
		if(!score.containsKey(player.getName())){
			score.put(player.getName(), 100);
			return 100;
		}else{
			return score.get(player.getName());
		}
	}
	
	public int getScore(String player){
		if(!score.containsKey(player)){
			score.put(player, 100);
			return 100;
		}else{
			return score.get(player);
		}
	}
	
	public void subtractScore(Player player, int take){
		if(score.containsKey(player.getName()) && score.get(player.getName()) - take >= 0)
			this.score.put(player.getName(), this.score.get(player.getName()) - take);
		else
			this.score.put(player.getName(), 0);
		
		save();
	}
	
	public String topInGame(){
		String most = "";

		for(Entry<String, Integer> info : score.entrySet()){
			if(info.getValue() > getScore(most)){
				most = info.getKey();
			}
		}
		
		return most;
	}
	
	public ArrayList<String> getTop(){
		String most = "";
		ArrayList<String> top = new ArrayList<String>();
		HashMap<String, Integer> hold = new HashMap<String, Integer>();
		
		for(Entry<String, Integer> e : score.entrySet()){
			hold.put(e.getKey(), e.getValue());
		}
		
		int theMax = 10;
		if(hold.size()<10)
			theMax = hold.size();
		
		for(int i = 0; i < theMax; i++){
			for(Entry<String, Integer> info : hold.entrySet()){
				if(info.getValue() > getScore(most)){
					most = info.getKey();
					System.out.println(info.getKey());
				}
			}
			
			top.add(most);
			hold.remove(most);
		}
		
		return top;
	}
}
