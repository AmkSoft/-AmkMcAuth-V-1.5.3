package com.mooo.amksoft.amkmcauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mooo.amksoft.amkmcauth.AmkAUtils;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Language;

public class CmdSetRecvr implements CommandExecutor {

	    @SuppressWarnings("unused") // Despite "unused": IT IS NEEDED in then onEnable Event !!!!!
		private final AmkMcAuth plugin;

	    public CmdSetRecvr(AmkMcAuth instance) {
	        this.plugin = instance;
	    }

	    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
	    {
	        if (!(cs instanceof Player)) {
	            cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.COMMAND_NO_CONSOLE.toString()));
	            return true;
	        }

	        if (args.length < 1) {
	            cs.sendMessage(cmd.getDescription());
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_QUESTION0.toString()));
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_QUESTION1.toString()));
                cs.sendMessage(ChatColor.RED + String.format(AmkAUtils.colorize(Language.RECOVER_QUESTION2.toString() + "."), label));
	          	//cs.sendMessage(String.format(AmkAUtils.colorize(Language.USAGE_LOGIN2.toString()),cmd));
	            return false;
	        }

	  		String RecoverText = AmkAUtils.getFinalArg(args, 0).trim(); // support spaces
	  		return SetMyRcvr((Player) cs, cmd.getName(), RecoverText);
	  	}


	    public static boolean CmdSetMyRcvr(CommandSender cs, String cmd, String RecoverText)
	    {
	  		return SetMyRcvr((Player) cs, cmd, RecoverText.trim());
	    }
	    
	    private static boolean SetMyRcvr(Player cs, String cmd, String RecoverText) {
	        if (cmd.equalsIgnoreCase("setrecover")) {        	
	            if (!cs.hasPermission("amkauth.setrecover")) {
	                AmkAUtils.dispNoPerms(cs);
	                return true;
	            }

	            final Player p = (Player) cs;
	            final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
	            if (!ap.isLoggedIn()) {
	                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.NOT_LOGGED_IN.toString()));
	                return true;
	            }
	            
	            //if (!cs.hasPermission("amkauth.setrecover")) {
	            //    AmkAUtils.dispNoPerms(cs);
	            //    return true;
	            //}
	            if (!RecoverText.contains("=")) {
	                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_QUESTION0.toString()));
	                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_QUESTION1.toString()));
	                cs.sendMessage(ChatColor.RED + String.format(AmkAUtils.colorize(Language.RECOVER_QUESTION2.toString() + "."), cmd));
	                return false;
	            }

	    		ap.setRecoverInfo(RecoverText); 
                cs.sendMessage(AmkAUtils.colorize(Language.UPDATED_SUCCESSFULLY.toString()));

	    		return true;
	        }
	        return false;
	    }
	}
