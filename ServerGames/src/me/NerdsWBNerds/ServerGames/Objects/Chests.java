package me.NerdsWBNerds.ServerGames.Objects;

import me.NerdsWBNerds.ServerGames.ServerGames;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Random;

/**
 * Chests created by Brenhein
 */
public class Chests {
    // might not work
    public static void resetChests(){
    	// fishing rod, bow, arrow, diamond, wood sword, compass, iron sword, stone sword, gold sword, all armour
    	int[] items = { 346, 261, 262, 260, 264, 268, 345, 267, 272, 283, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 317, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    	for(Chunk c : ServerGames.cornacopia.getWorld().getLoadedChunks()){
    		BlockState[] blockState = c.getTileEntities();
    		for(BlockState tileEntity : blockState){
    			if(tileEntity instanceof Chest){    	
    				System.out.println("Update!");
    				
    				ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>();
    				Chest chestBlock = (Chest)tileEntity;
    				Random rnd = new Random();
    				for(int i = 0; i < 27; i++){
    					arrayList.add(new ItemStack(items[rnd.nextInt(items.length)], 1));
    				}
    				setChest(chestBlock, arrayList);
    			}
    		}
        }
    }

    public static void setChest(Chest c, ArrayList<ItemStack> items){
    	try{
    		Chest chest = (Chest) c;
            ItemStack[] stuff = new ItemStack[27];
            
            for(int i = 0; i < 27; i++){
            	stuff[i] = items.get(i);
            }

            chest.getInventory().clear();  
            chest.getInventory().setContents(stuff);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
}
