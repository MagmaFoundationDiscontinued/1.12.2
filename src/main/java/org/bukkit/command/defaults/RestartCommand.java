package org.bukkit.command.defaults;


import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.spigotmc.AsyncCatcher;
import org.spigotmc.SpigotConfig;
import org.spigotmc.WatchdogThread;

import java.io.File;
import java.util.List;

public class RestartCommand extends BukkitCommand {

    public RestartCommand(String name) {
        super(name);
        this.description = "Restarts the server";
        this.usageMessage = "/restart";
        this.setPermission("bukkit.command.restart");
    }

    public static void restart()
    {
        AsyncCatcher.enabled = false; // Disable async catcher incase it interferes with us
        File script = new File(SpigotConfig.restartScript);
        try
        {
            if ( script.isFile() )
            {
                System.out.println( "Attempting to restart with " + SpigotConfig.restartScript );

                // Disable Watchdog
                WatchdogThread.doStop();

                // Kick all players
                for ( EntityPlayerMP p : (List< EntityPlayerMP>) MinecraftServer.getServerInst().getPlayerList().playerEntityList )
                {
                    p.connection.disconnect(SpigotConfig.restartMessage);
                }
                // Give the socket a chance to send the packets
                try {
                    Thread.sleep( 100 );
                } catch ( InterruptedException ex ) {
                }
                // Close the socket so we can rebind with the new process
                MinecraftServer.getServerInst().getNetworkSystem().terminateEndpoints();

                // Give time for it to kick in
                try {
                    Thread.sleep( 100 );
                } catch ( InterruptedException ex ) {
                }

                // Actually shutdown
                try {
                    Bukkit.shutdown();
                } catch ( Throwable t ) {
                }

                // This will be done AFTER the server has completely halted
                Thread shutdownHook = new Thread()
                {
                    @Override
                    public void run() {
                        try
                        {
                            String os = System.getProperty( "os.name" ).toLowerCase(java.util.Locale.ENGLISH);
                            if ( os.contains( "win" ) )
                            {
                                Runtime.getRuntime().exec( "cmd /c start " + script.getPath() );
                            } else
                            {
                                Runtime.getRuntime().exec( new String[]
                                        {
                                                "sh", script.getPath()
                                        } );
                            }
                        } catch ( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }
                };

                shutdownHook.setDaemon( true );
                Runtime.getRuntime().addShutdownHook( shutdownHook );
            } else{
                System.out.println( "Startup script '" + SpigotConfig.restartScript + "' does not exist! Stopping server." );

                // Actually shutdown
                try {
                    Bukkit.getServer().shutdown();;
                } catch ( Throwable t ){
                }}
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if ( testPermission( sender ) )
        {
            MinecraftServer.getServerInst().processQueue.add( new Runnable() {
                @Override
                public void run() {
                    restart();
                }
            } );
        }
        return true;
    }
}