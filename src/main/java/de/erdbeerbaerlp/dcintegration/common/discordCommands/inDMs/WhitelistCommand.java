package de.erdbeerbaerlp.dcintegration.common.discordCommands.inDMs;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import de.erdbeerbaerlp.dcintegration.common.Discord;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.PlayerLinkController;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.erdbeerbaerlp.dcintegration.common.util.Variables.discord_instance;


public class WhitelistCommand extends DMCommand {
    @Override
    public String getName() {
        return "whitelist";
    }


    @Override
    public String getDescription() {
        return Configuration.instance().localization.commands.descriptions.whitelist;
    }

    private final String uuidRegex = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)";

    @Override
    public void execute(String[] args, final MessageChannel channel, User sender) {
        if (discord_instance.getChannel().getGuild().isMember(sender)) {
            Member m = discord_instance.getChannel().getGuild().getMember(sender);
            if (Configuration.instance().linking.requiredRoles.length != 0) {
                AtomicBoolean ok = new AtomicBoolean(false);
                m.getRoles().forEach((role) -> {
                    for (String s : Configuration.instance().linking.requiredRoles) {
                        if (s.equals(role.getId())) ok.set(true);
                    }
                });
                if (!ok.get()) {
                    channel.sendMessage(Configuration.instance().localization.linking.link_requiredRole).queue();
                    return;
                }
            }
        } else {
           channel.sendMessage(Configuration.instance().localization.linking.link_notMember).queue();
            return;
        }
        if (PlayerLinkController.isDiscordLinkedJava(sender.getId())) {
            channel.sendMessage(Configuration.instance().localization.linking.alreadyLinked.replace("%player%", MessageUtils.getNameFromUUID(PlayerLinkController.getPlayerFromDiscord(sender.getId())))).queue();
            return;
        }
        if (args.length > 1) {
            channel.sendMessage(Configuration.instance().localization.commands.tooManyArguments).queue();
            return;
        }
        if (args.length < 1) {
            channel.sendMessage(Configuration.instance().localization.commands.notEnoughArguments).queue();
            return;
        }
        UUID u;
        String s = args[0];
        try {
            final String oldS = s;
            try {
                if (!s.contains("-"))
                    s = s.replaceFirst(
                            uuidRegex, "$1-$2-$3-$4-$5"
                    );
                u = UUID.fromString(s);
            } catch (Exception e) {
                try {
                    final URL url = new URL("https://api.mojang.com/users/profiles/minecraft/"+oldS);
                    final HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                    if(con.getResponseCode() == 204) throw new IOException(); //Just skip to the catch block
                    final JsonReader r = new JsonReader(new InputStreamReader(con.getInputStream()));
                    final JsonParser p = new JsonParser();
                    final JsonElement json = p.parse(r);
                    u = UUID.fromString(json.getAsJsonObject().get("id").getAsString().replaceFirst(uuidRegex, "$1-$2-$3-$4-$5"));
                } catch (IOException ex) {
                    u = Discord.dummyUUID;
                    ex.printStackTrace();
                }
            }
            final boolean linked = PlayerLinkController.linkPlayer(sender.getId(), u);
            if (linked)
                channel.sendMessage(Configuration.instance().localization.linking.linkSuccessful.replace("%prefix%", Configuration.instance().commands.dmPrefix).replace("%player%", MessageUtils.getNameFromUUID(u))).queue();
            else
                channel.sendMessage(Configuration.instance().localization.linking.linkFailed).queue();
        } catch (IllegalArgumentException e) {
            channel.sendMessage(Configuration.instance().localization.linking.link_argumentNotUUID.replace("%prefix%", Configuration.instance().commands.dmPrefix).replace("%arg%", s)).queue();
        }
    }

    @Override
    public boolean canUserExecuteCommand(User user) {
        if(user == null) return false;
        return !PlayerLinkController.isDiscordLinkedJava(user.getId());
    }
}
