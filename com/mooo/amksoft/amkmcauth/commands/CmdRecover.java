package com.mooo.amksoft.amkmcauth.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mooo.amksoft.amkmcauth.AmkAUtils;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.tools.MySQL;
import com.mooo.amksoft.amkmcauth.tools.SMTP;


public class CmdRecover implements CommandExecutor {

    @SuppressWarnings("unused") // Despite "unused": IT IS NEEDED in then onEnable Event !!!!!
	private final AmkMcAuth plugin;

    public CmdRecover(AmkMcAuth instance) {
        this.plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
    {
        if (!(cs instanceof Player)) {
            cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.COMMAND_NO_CONSOLE.toString()));
            return true;
        }

        String Parm;
        if (args.length < 1)
        	Parm = "";
        else
        	Parm = AmkAUtils.getFinalArg(args, 0).trim(); // support spaces
  		return SendNewPwd((Player) cs, cmd.getName(), Parm);
  	}


    public static boolean CmdSendNewPwd(CommandSender cs, String cmd, String Parm)
    	{
  		return SendNewPwd((Player) cs, cmd, Parm);
    }
    
    private static boolean SendNewPwd(Player cs, String cmd, String Parm) {
        if (cmd.equalsIgnoreCase("recoverpwd")) {        	
            if (!cs.hasPermission("amkauth.recoverpwd")) {
                AmkAUtils.dispNoPerms(cs);
                return true;
            }

            final Player p = (Player) cs;
            final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            
            // possible calls:
            // 1e: recoverpwd 					(action: defaults to recover using email)
            // 2e: recoverpwd email				(action: recover using email = default)
            // 3e: recoverpwd question			(action: force recover using question+answer)
            // 4e: recoverpwd something else	(action: check question+answer + recover)
            // If email is not possible, it falls back to question+answer

            if(Parm.equalsIgnoreCase("email")) Parm="";

            if(!Config.MySqlDbHost.equals("")) { // MongoDB goes BEFORE ProfileFiles !!
            	try {
            		// Connection con = MySQL.getConnection(); 
            		PreparedStatement ps = MySQL.getConnection().prepareStatement(
            						"SELECT RecovrTxt " +
            						"FROM   Players " +
		        					"WHERE  Name  = ? ");
            		ps.setString(1, p.getName());
            		ResultSet res = ps.executeQuery();
            		//Code using ResultSet entries here
            		if (res.next() == true) { 
            			// We have a record, Tell AmkMcAuth THIS is the correct password (no Hashing!!, it is Hashed!!).
              			String SavedRcvrTxt = res.getString("RecovrTxt");
              			if(!SavedRcvrTxt.equals(ap.getRecoverInfo())) {
              				ap.setRecoverInfo(SavedRcvrTxt);
              			}
            		}
            		res.close();
            		ps.close();
            	} catch (SQLException e1) {
            		// TODO Auto-generated catch block
            		e1.printStackTrace();
            	}
            }
            
            boolean recoverInfoChkOk = false;
            if(Parm.length()>0) { // We have a parm value
            	// first check if the given parameter equals to the Recover-Answer.
            	if(ap.getRecoverInfo().contains("=")) {
                	if(ap.getRecoverInfo().indexOf(Parm)>ap.getRecoverInfo().indexOf("=")) {
                		// Parm is found in the recovertext, check on exact value
                		String RecovTxt = ap.getRecoverInfo();
                		String StrSplit[] = RecovTxt.split("=");
                		if(StrSplit[1].trim().equals(Parm.trim())) recoverInfoChkOk=true;
                	}
            	}
            }
            
            boolean emailPossible = Config.emlFromEmail.contains("@") && ap.getEmailAddress().contains("@");
            if (!emailPossible || Parm.equalsIgnoreCase("question") ) {
            	// There is no EMail setup, so Password-Reset using mail is not gonne work, or players asked explicit for question.
            	// Lets try the "old fashion/unsecure" Question&Answer method. The default will be 'email' thought..
            	if(!ap.getRecoverInfo().contains("=")) {
	                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_QUESTION0.toString()));
	                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_QUESTION1.toString()));
	                cs.sendMessage(ChatColor.RED + String.format(AmkAUtils.colorize(Language.RECOVER_QUESTION2.toString() + "."), "setrecover"));
	                return false;
            	}
            	else
            		{
            		// We have nice Recover-text. Split it and show the question.
            		String RecovTxt = ap.getRecoverInfo();
            		String StrSplit[] = RecovTxt.split("=");

	                cs.sendMessage(ChatColor.RED + String.format(AmkAUtils.colorize(Language.RECOVER_RECOVERPW.toString() + "."), "recoverpwd"));
	                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(StrSplit[0].trim()+"?"));
	                return true;
            	}
            }

            // Parm is not 'question' or it is possible to use email for recovery
            
       		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
       		final String Password = RandomStringUtils.random( 7, characters );            	

            if(recoverInfoChkOk) { // Well, We have a correct Answer on the question...
    			if (ap.setPassword(Password, ap.getHashType())) {
    				cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.RECOVER_NEWPWDCHG.toString()));
					AmkMcAuth.getInstance().getLogger().info(p.getName() + " has reset the lost/forgotten Password");
    			}
    			else 
					{ 
    				cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.PASSWORD_COULD_NOT_BE_SET.toString()));
					AmkMcAuth.getInstance().getLogger().info(p.getName() + " !!!! Recover Password could not be set");
				}
                return true;
            }
            
            if (!Config.emlFromEmail.contains("@")) {
                cs.sendMessage(ChatColor.RED + "Incorrect Config Email Setup.");
                cs.sendMessage(ChatColor.RED + Language.ADMIN_SET_UP_INCORRECTLY.toString());
                cs.sendMessage(ChatColor.RED + Language.CONTACT_ADMIN.toString());
                return true;
            }
            if (!ap.getEmailAddress().contains("@")) {
                cs.sendMessage(ChatColor.RED + String.format(AmkAUtils.colorize(Language.PLAYER_INVALID_EMAILADDRESS.toString() + "."), ap.getUserName(), "'" + ap.getEmailAddress() + "'"));
                return true;
            }

			if (ap.setPassword(Password, ap.getHashType())) {
				new Thread(new Runnable() {
					@Override
					public void run() {        		    	
						String Player = ap.getUserName();
    				
						// SetUp Email Session
						SMTP.Email email = SMTP.createEmptyEmail();
						//email.add("Content-Type", "text/html"); //Headers for the email (useful for html) you do not have to set them
						//email.add("Content-Type", "text/plain");  //Default Header ("text/plain") for the email.you do not have to set
						email.from(Config.emlFromNicer, Config.emlFromEmail); //The sender of the email.
						email.to(Player, ap.getEmailAddress()); //The recipient of the email.
						email.subject(Config.recoversubject); //Subject of the email
						email.body(String.format(Config.recoverbodytxt, Player, Password));
						// All the email stuff here
						//SMTP.sendEmail(smtpServer, email, password, mail, debug);    			        
						SMTP.sendEmail(Config.emlSmtpServr,
										Config.emlLoginName, 
										Config.emlLoginPswd, 
										email, false);
					}
				}).start();

				cs.sendMessage(ChatColor.BLUE + AmkAUtils.colorize(Language.PASSWORD_RECOVER_MAIL.toString()));
				AmkMcAuth.getInstance().getLogger().info(p.getName() + " !!!! Recover Password send to Player");
				return true;
			}
			else 
				{ 
				cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.PASSWORD_COULD_NOT_BE_SET.toString()));
				AmkMcAuth.getInstance().getLogger().info(p.getName() + " !!!! Recover Password could not be set");
			}
        }
        return false;
    }
}
